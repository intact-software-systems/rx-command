package com.intact.rx.core.command;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.command.Action0;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.factory.CommandBuilder;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;

/**
 * TODO: Make round robin to support FIFO, LIFO, etc and also max concurrent commands.
 * TODO: Implement queue functions that returns next ready Command and upon finish executing puts at the back of the queue.
 */
public class Commands<T> extends ArrayList<com.intact.rx.core.command.api.Command<T>> {
    private static final long serialVersionUID = -4474539447203843105L;

    @SuppressWarnings("rawtypes")
    private static final Commands emptyList = new Commands();

    @SuppressWarnings("unchecked")
    public static <V> Commands<V> emptyList() {
        //noinspection ReturnOfCollectionOrArrayField
        return emptyList;
    }

    private final ExecutionStatus executionStatus = new ExecutionStatus();
    private final AtomicReference<ConcurrentHashMap<com.intact.rx.core.command.api.Command<T>, Throwable>> exceptions = new AtomicReference<>(new ConcurrentHashMap<>());

    public Commands() {
    }

    public Commands(Collection<com.intact.rx.core.command.api.Command<T>> commands) {
        super(commands);
    }

    private Commands(ConcurrentHashMap<com.intact.rx.core.command.api.Command<T>, Throwable> exceptions) {
        this.exceptions.set(requireNonNull(exceptions));
    }

    // ------------------------------------------------------------
    // Factory functions
    // ------------------------------------------------------------

    @SafeVarargs
    public static <V> Commands<V> create(com.intact.rx.core.command.api.Command<V>... commandList) {
        Commands<V> commands = new Commands<>();
        commands.addCommands(commandList);
        return commands;
    }

    @SafeVarargs
    public static <K, V> Commands<V> actionsAsCommands(CommandPolicy policy, CircuitId circuitBreakerId, RateLimiterId rateLimiterId, Action0<V>... actions) {
        return Arrays.stream(actions)
                .map(action -> CommandBuilder.<V>withPolicy(policy).withCircuitBreakerId(circuitBreakerId).withRateLimiterId(rateLimiterId).addAction(action).build())
                .collect(Collectors.toCollection(Commands::new));
    }

    // ---------------------------------
    // Mutable functions
    // ---------------------------------

    @SafeVarargs
    private final List<com.intact.rx.core.command.api.Command<T>> addCommands(com.intact.rx.core.command.api.Command<T>... commandList) {
        Collections.addAll(this, commandList);
        return this;
    }

    public void reset() {
        // Precondition: Validate.assertTrue(!areCommandsExecuting());
        // Precondition: Validate.assertTrue(!executionStatus.isExecuting());
        this.forEach(Command::reset);
        this.exceptions.set(new ConcurrentHashMap<>());
    }

    // ---------------------------------
    // Immutable functions
    // ---------------------------------

    void addException(com.intact.rx.core.command.api.Command<T> command, Throwable throwable) {
        this.exceptions.get().put(command, throwable);
    }

    public Commands<T> copy() {
        return this.stream()
                .map(com.intact.rx.core.command.Command::new)
                .collect(Collectors.toCollection(() -> new Commands<>(exceptions.get())));
    }

    public ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public Collection<Throwable> getExceptions() {
        List<Throwable> exceptions = new ArrayList<>();
        for (com.intact.rx.core.command.api.Command<T> command : this) {
            Optional.ofNullable(this.exceptions.get().get(command)).ifPresent(exceptions::add);
            command.getExceptions().forEach((action, throwable) -> exceptions.add(throwable));
        }
        return exceptions;
    }

    public List<com.intact.rx.core.command.api.Command<T>> getCommands() {
        return Collections.unmodifiableList(this);
    }

    public int numCommandsExecuting() {
        return (int) this.stream().filter(Command::isExecuting).count();
    }

    public boolean areCommandsExecuting() {
        return this.stream().anyMatch(Command::isExecuting);
    }

    /**
     * Check if commands are successfully executed.
     */
    public boolean isSuccess() {
        return this.stream().allMatch(Command::isSuccess);
    }

    /**
     * Check if commands are done, i.e., no more execution can be scheduled.
     */
    public boolean isDone() {
        return this.stream().allMatch(Command::isDone);
    }

    /**
     * Compute the earliest time a command can be ready at, based on timeout and interval policy:
     * <p>
     * "The time when a command should be done with the current execution and is ready for another execution".
     */
    long earliestReadyInMs() {
        long maxWaitTimeMs = Long.MAX_VALUE;
        for (com.intact.rx.core.command.api.Command<T> command : this) {
            maxWaitTimeMs = Math.min(maxWaitTimeMs, command.readyInMs());
        }
        return maxWaitTimeMs;
    }

    // ---------------------------------
    // Action functions
    // ---------------------------------

    /**
     * Stop timed-out commands
     * <p>
     * Algorithm: command.doTimeout() leads to callback to commandController.processOnError(command, ..)
     * which cancels command through its future.
     */
    void stopTimeoutCommands() {
        stream().filter(Command::isTimeout).forEach(Command::doTimeout);
    }

    void doTimeout() {
        this.forEach(Command::doTimeout);
    }

    void unsubscribe() {
        this.forEach(Command::unsubscribe);
    }
}
