package com.intact.rx.core.command;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.Action;
import com.intact.rx.api.command.Action0;
import com.intact.rx.core.command.observer.CommandObserver;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.command.strategy.ExecutionPolicyChecker;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.exception.CancelCommandException;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.api.Context;

/**
 * Command executes all attached actions in FIFO order. CommandPolicy is only considered when attached to a CommandController.
 */
@SuppressWarnings({"SynchronizedMethod", "AccessToStaticFieldLockedOnInstance"})
public class Command<T> implements com.intact.rx.core.command.api.Command<T> {
    private static final Logger log = LoggerFactory.getLogger(Command.class);

    private final Context<CommandPolicy, CommandState<T>> context;

    public Command(CommandPolicy policy, CircuitId circuitBreakerId, RateLimiterId rateLimiterId, Iterable<Action0<T>> actions) {
        this.context = new ContextObject<>(policy, new CommandState<>(actions, circuitBreakerId, rateLimiterId));
    }

    // Note: For internal rx usage only
    Command(com.intact.rx.core.command.api.Command<T> command) {
        this.context = new ContextObject<>(command.getPolicy(), new CommandState<>(command.getActions(), command.getCircuitBreakerId(), command.getRateLimiterId()));
        this.context.state().getExceptions().putAll(command.getExceptions());
    }

    // -----------------------------------------------------------
    // Interface Runnable
    // -----------------------------------------------------------

    @Override
    public void run() {
        try {
            config().getCompositionStrategy().perform(config(), this);
        } catch (Throwable throwable) {
            onError(throwable);
        }
    }

    // -----------------------------------------------------------
    // Interface Command<T>
    // -----------------------------------------------------------

    @Override
    public synchronized Subscription subscribe(CommandObserver<T> observer, RxThreadPool threadPool) {
        requireNonNull(observer);
        requireNonNull(threadPool);

        if (state().getExecutionStatus().isExecuting()) {
            log.debug("Command is already executing. Returning existing CommandSubscription!");
            return state().getSubscription();
        }

        // ---------------------------------------
        // Set observer and update status
        // ---------------------------------------
        state().setCommandObserver(observer);
        state().getExecutionStatus().start();
        state().getExceptions().clear();

        return state().setAndGetSubscription(
                new CommandActionSubscription(
                        this,
                        threadPool,
                        state().scheduleNowAndSetFuture(this, threadPool)
                )
        );
    }

    @Override
    public synchronized void unsubscribe() {
        state().getExecutionStatus().cancel();
        state().getSubscription().cancel();
        state().resetSubscription();
    }

    @Override
    public boolean isReady() {
        return ExecutionPolicyChecker.isReadyToExecute(
                state().getExecutionStatus(),
                config().getAttempt(),
                config().getInterval(),
                config().getRetryInterval()
        );
    }

    @Override
    public boolean isSuccess() {
        return state().getExecutionStatus().isSuccess();
    }

    @Override
    public boolean isExecuting() {
        return state().getExecutionStatus().isExecuting();
    }

    @Override
    public boolean isDone() {
        return !state().getExecutionStatus().isExecuting() &&
                !ExecutionPolicyChecker.isInAttempt(state().getExecutionStatus(), config().getAttempt());
    }

    @Override
    public boolean isTimeout() {
        return ExecutionPolicyChecker.isTimeout(state().getExecutionStatus(), config().getTimeout());
    }

    @Override
    public boolean isCancelled() {
        return state().getExecutionStatus().isCancelled();
    }

    @Override
    public long readyInMs() {
        return ExecutionPolicyChecker.computeEarliestReadyInMs(
                state().getExecutionStatus(),
                config().getAttempt(),
                config().getInterval(),
                config().getRetryInterval(),
                config().getTimeout());
    }

    @Override
    public long timeoutInMs() {
        return ExecutionPolicyChecker.computeTimeUntilTimeoutMs(
                state().getExecutionStatus(),
                config().getTimeout());
    }

    @Override
    public void request(long n) {
        // TODO: Implement this function?
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void cancel() {
        state().getSubscription().cancel();
        state().getExecutionStatus().cancel();
    }

    @Override
    public void doTimeout() {
        state().getSubscription().cancel();
        onError(new TimeoutException("Command timed out according to command policy: " + this));
    }

    @Override
    public CommandPolicy getPolicy() {
        return config();
    }

    @Override
    public ExecutionStatus getExecutionStatus() {
        return state().getExecutionStatus();
    }

    @Override
    public Map<Action, Throwable> getExceptions() {
        return state().getExceptions();
    }

    @Override
    public CircuitId getCircuitBreakerId() {
        return state().getCircuitBreakerId();
    }

    @Override
    public RateLimiterId getRateLimiterId() {
        return state().getRateLimiterId();
    }

    @Override
    public Iterable<Action0<T>> getActions() {
        return state().getActions();
    }

    @Override
    public boolean isPolicyViolated() {
        return ExecutionPolicyChecker.isPolicyViolated(
                state().getExecutionStatus(),
                config().getAttempt(),
                config().getInterval(),
                config().getRetryInterval(),
                config().getTimeout()
        );
    }

    @Override
    public synchronized void reset() {
        state().reset();
    }

    // -----------------------------------------------------------
    // Interface RxObserver<T>
    // -----------------------------------------------------------

    @Override
    public void onSubscribe(Subscription subscription) {
        state().getCommandObserver().onSubscribe(this, subscription);
    }

    @Override
    public void onNext(T value) {
        if (value == null) {
            log.error("Illegal use of onNext with null value. Command: {}", this);
            throw new IllegalStateException("onNext must only be called with non-null value");
        }

        state().getCommandObserver().onNext(this, value);
    }

    @Override
    public void onError(Throwable throwable) {
        state().resetSubscription();
        state().removeFuture();

        if (throwable instanceof CancelCommandException) {
            state().getExecutionStatus().cancel();
        } else {
            state().getExecutionStatus().failure();
        }

        state().getCommandObserver().onError(this, throwable);
    }

    @Override
    public void onComplete() {
        state().resetSubscription();
        state().removeFuture();

        state().getExecutionStatus().success();
        state().getCommandObserver().onComplete(this);
    }

    // -----------------------------------------------------------
    // Interface RxActionObserver<T>
    // -----------------------------------------------------------

    @Override
    public void onSubscribe(Action0<T> action, Subscription subscription) {
        // Note: Not doing anything at the moment
    }

    @Override
    public void onNext(Action0<T> action, T value) {
        // Note: Not doing anything at the moment
    }

    @Override
    public void onError(Action0<T> action, Throwable throwable) {
        log.debug("Error encountered while executing action [{}]", action, throwable);
        state().getExceptions().put(action, throwable);
    }

    @Override
    public void onComplete(Action0<T> action) {
        // Note: Not doing anything at the moment
    }

    @Override
    public void onFallback(Action0<T> action) {
        state().getExecutionStatus().fallback();
    }

    // -----------------------------------------------------------
    // Access ContextObject
    // -----------------------------------------------------------

    private CommandPolicy config() {
        return context.config();
    }

    private CommandState<T> state() {
        return context.state();
    }

    // -----------------------------------------------------------
    // Overridden methods from Object
    // -----------------------------------------------------------

    @Override
    public String toString() {
        return "Command@" + hashCode() + " {\n" +
                "   context=" + context +
                "\n}";
    }
}
