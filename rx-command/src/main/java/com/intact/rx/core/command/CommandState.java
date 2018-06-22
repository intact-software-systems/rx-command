package com.intact.rx.core.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.Action;
import com.intact.rx.api.command.Action0;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.core.command.nullobjects.CommandObserverNoOp;
import com.intact.rx.core.command.nullobjects.CommandSubscriptionNoOp;
import com.intact.rx.core.command.observer.CommandObserver;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.exception.ExceptionMessageFactory;
import com.intact.rx.templates.Validate;

/**
 * The state of a Command as it is executed by CommandController
 */
public class CommandState<T> {
    private final Iterable<Action0<T>> actions;
    private final CircuitId circuitId;
    private final RateLimiterId rateLimiterId;

    private final AtomicReference<CommandObserver<T>> commandObserver;
    private final AtomicReference<Subscription> subscription;
    private final AtomicReference<ExecutionStatus> executionStatus;
    private final AtomicReference<ConcurrentHashMap<Action, Throwable>> exceptions;
    private final AtomicReference<Strategy0<Runnable>> futureRemover;

    CommandState(Iterable<Action0<T>> actions, CircuitId circuitBreakerId, RateLimiterId rateLimiterId) {
        this.actions = requireNonNull(actions);
        Validate.assertTrue(actions.iterator().hasNext());
        this.circuitId = requireNonNull(circuitBreakerId);
        this.rateLimiterId = requireNonNull(rateLimiterId);

        //noinspection unchecked,rawtypes
        this.commandObserver = new AtomicReference<>(CommandObserverNoOp.instance);
        this.subscription = new AtomicReference<>(CommandSubscriptionNoOp.instance);
        this.executionStatus = new AtomicReference<>(new ExecutionStatus());
        this.exceptions = new AtomicReference<>(new ConcurrentHashMap<>());
        this.futureRemover = new AtomicReference<>(() -> null);
    }

    // ----------------------------------
    // Getter and setter functions
    // ----------------------------------

    void reset() {
        subscription.set(CommandSubscriptionNoOp.instance);
        executionStatus.set(new ExecutionStatus());
        exceptions.set(new ConcurrentHashMap<>());
    }

    Map<Action, Throwable> getExceptions() {
        return exceptions.get();
    }

    ExecutionStatus getExecutionStatus() {
        return executionStatus.get();
    }

    Iterable<Action0<T>> getActions() {
        return actions;
    }

    CircuitId getCircuitBreakerId() {
        return circuitId;
    }

    RateLimiterId getRateLimiterId() {
        return rateLimiterId;
    }

    Subscription getSubscription() {
        return subscription.get();
    }

    Subscription setAndGetSubscription(Subscription subscription) {
        this.subscription.set(requireNonNull(subscription));
        return this.subscription.get();
    }

    void resetSubscription() {
        subscription.set(CommandSubscriptionNoOp.instance);
    }

    ScheduledFuture<?> scheduleNowAndSetFuture(Runnable runnable, RxThreadPool threadPool) {
        synchronized (futureRemover) {
            ScheduledFuture<?> schedule = threadPool.schedule(runnable, 0L);
            futureRemover.set(() -> threadPool.removeScheduledFuture(schedule));
            return schedule;
        }
    }

    Runnable removeFuture() {
        synchronized (futureRemover) {
            return futureRemover.get().perform();
        }
    }

    // ----------------------------------
    // Command observers
    // ----------------------------------

    void setCommandObserver(CommandObserver<T> observer) {
        commandObserver.set(requireNonNull(observer));
    }

    CommandObserver<T> getCommandObserver() {
        return commandObserver.get();
    }

    @Override
    public String toString() {
        return "CommandState{\n" +
                "           actions=" + actions +
                ", \n           circuitBreakerId=" + circuitId +
                ", \n           executionStatus=" + executionStatus +
                ", \n           exceptions={\n" + ExceptionMessageFactory.buildExceptionsMessage(exceptions.get()) + "}" +
                "\n     }";
    }
}
