package com.intact.rx.core.command;

import java.util.List;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.command.Strategy2;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.strategy.ComputationStrategies;
import com.intact.rx.policy.Attempt;
import com.intact.rx.policy.Interval;
import com.intact.rx.policy.MaxLimit;
import com.intact.rx.policy.Timeout;

/**
 * Notification policy on errors?
 * MaxLimit on number of attached commands?
 * <p>
 * Policy on "Maximum number of concurrently executing commands in one controller".
 * Policy on "Maximum time to finish all commands before controller stops.".
 * <p>
 * Scheduler:
 * - Hand out jobs based on requests (pull based), not faster then job frequency policies and RateLimiter for system
 * - Enforce Hours of Operation policy (opening hours)
 */
@SuppressWarnings("WeakerAccess")
public class CommandControllerPolicy {
    private static final CommandControllerPolicy parallelPolicy = parallelAnd(Timeout.no());
    private static final CommandControllerPolicy sequentialPolicy = sequentialAnd(Timeout.no());

    private final Attempt attempt;
    private final Interval interval;
    private final Timeout timeout;
    private final MaxLimit maxConcurrentCommands;
    @SuppressWarnings("rawtypes")
    private final Strategy2<List<com.intact.rx.core.command.api.Command>, Commands, MaxLimit> computationStrategy;

    public CommandControllerPolicy(
            Attempt attempt,
            Interval interval,
            Timeout timeout,
            MaxLimit maxConcurrentCommands,
            @SuppressWarnings("rawtypes") Strategy2<List<com.intact.rx.core.command.api.Command>, Commands, MaxLimit> computationStrategy) {
        this.attempt = requireNonNull(attempt);
        this.interval = requireNonNull(interval);
        this.timeout = requireNonNull(timeout);
        this.computationStrategy = requireNonNull(computationStrategy);
        this.maxConcurrentCommands = requireNonNull(maxConcurrentCommands);
    }

    public Attempt getAttempt() {
        return attempt;
    }

    public Interval getInterval() {
        return interval;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public MaxLimit getMaxConcurrentCommands() {
        return maxConcurrentCommands;
    }

    @SuppressWarnings("rawtypes")
    public Strategy2<List<Command>, Commands, MaxLimit> getComputationStrategy() {
        return computationStrategy;
    }

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static CommandControllerPolicy parallel() {
        return parallelPolicy;
    }

    public static CommandControllerPolicy sequential() {
        return sequentialPolicy;
    }

    public static CommandControllerPolicy sequentialAnd(Timeout timeout) {
        return new CommandControllerPolicy(Attempt.once(), Interval.nowThenTenSeconds(), timeout, MaxLimit.withLimit(500), (commands, maxLimit) -> ComputationStrategies.sequentialComputation(commands, maxLimit));
    }

    public static CommandControllerPolicy parallelAnd(Timeout timeout) {
        return new CommandControllerPolicy(Attempt.once(), Interval.nowThenTenSeconds(), timeout, MaxLimit.withLimit(500), (commands, maxLimit) -> ComputationStrategies.parallelComputation(commands, maxLimit));
    }

    public static CommandControllerPolicy parallelAnd(Attempt attempt, Interval interval, Timeout timeout) {
        return new CommandControllerPolicy(attempt, interval, timeout, MaxLimit.withLimit(500), (commands, maxLimit) -> ComputationStrategies.parallelComputation(commands, maxLimit));
    }

    @Override
    public String toString() {
        return "CommandControllerPolicy{" +
                "attempt=" + attempt +
                ", interval=" + interval +
                ", timeout=" + timeout +
                ", maxConcurrentCommands=" + maxConcurrentCommands +
                ", computationStrategy=" + computationStrategy +
                '}';
    }
}
