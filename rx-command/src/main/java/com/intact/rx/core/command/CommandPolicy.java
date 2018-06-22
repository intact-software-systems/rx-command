package com.intact.rx.core.command;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.command.Strategy2;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.strategy.CompositionStrategies;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.*;

public class CommandPolicy {
    private static final CommandPolicy runOnceNow =
            new CommandPolicy(
                    Attempt.once(),
                    Interval.nowThenThreeSeconds(),
                    Interval.nowThenThreeSeconds(),
                    Timeout.ofMillis(60000),
                    CompositionStrategies::compose,
                    Criterion.all(),
                    CircuitBreakerPolicy.unlimited,
                    RateLimiterPolicy.unlimited,
                    ErrorType.ERROR_ON_EXCEPTION
            );

    private final Attempt attempt;
    private final Interval interval;
    private final Interval retryInterval;
    private final Timeout timeout;
    private final Criterion successCriterion;
    @SuppressWarnings("rawtypes")
    private final Strategy2<Boolean, CommandPolicy, com.intact.rx.core.command.api.Command> compositionStrategy;
    private final CircuitBreakerPolicy circuitBreakerPolicy;
    private final RateLimiterPolicy rateLimiterPolicy;
    private final ErrorType errorType;

    public CommandPolicy(Attempt attempt,
                         Interval interval,
                         Interval retryInterval,
                         Timeout timeout,
                         @SuppressWarnings("rawtypes") Strategy2<Boolean, CommandPolicy, com.intact.rx.core.command.api.Command> compositionStrategy,
                         Criterion successCriterion,
                         CircuitBreakerPolicy circuitBreakerPolicy,
                         RateLimiterPolicy rateLimiterPolicy,
                         ErrorType errorType) {
        this.attempt = requireNonNull(attempt);
        this.interval = requireNonNull(interval);
        this.retryInterval = requireNonNull(retryInterval);
        this.timeout = requireNonNull(timeout);
        this.compositionStrategy = requireNonNull(compositionStrategy);
        this.successCriterion = requireNonNull(successCriterion);
        this.circuitBreakerPolicy = requireNonNull(circuitBreakerPolicy);
        this.rateLimiterPolicy = requireNonNull(rateLimiterPolicy);
        this.errorType = errorType;
    }

    public Interval getInterval() {
        return interval;
    }

    public Interval getRetryInterval() {
        return retryInterval;
    }

    public Attempt getAttempt() {
        return attempt;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public Criterion getSuccessCriterion() {
        return successCriterion;
    }

    @SuppressWarnings("rawtypes")
    public Strategy2<Boolean, CommandPolicy, Command> getCompositionStrategy() {
        return compositionStrategy;
    }

    public CircuitBreakerPolicy getCircuitBreakerPolicy() {
        return circuitBreakerPolicy;
    }

    public RateLimiterPolicy getRateLimiterPolicy() {
        return rateLimiterPolicy;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public boolean isErrorOnNull() {
        return Objects.equals(errorType, ErrorType.ERROR_ON_EXCEPTION_AND_NULL);
    }

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static CommandPolicy runOnceNow() {
        return runOnceNow;
    }

    public static CommandPolicy create(Attempt attempt, Interval interval, Interval retryInterval, Timeout timeout) {
        return new CommandPolicy(
                attempt,
                interval,
                retryInterval,
                timeout,
                CompositionStrategies::compose,
                Criterion.all(),
                CircuitBreakerPolicy.unlimited,
                RateLimiterPolicy.unlimited,
                ErrorType.ERROR_ON_EXCEPTION_AND_NULL);
    }

    public static CommandPolicy runOnce(Interval retryInterval, Timeout timeout, long retry, ErrorType errorType) {
        return new CommandPolicy(
                Attempt.retry(retry),
                retryInterval,
                retryInterval,
                timeout,
                CompositionStrategies::compose,
                Criterion.all(),
                CircuitBreakerPolicy.unlimited,
                RateLimiterPolicy.unlimited,
                errorType);
    }

    public static CommandPolicy runOnce(Interval retryInterval, Timeout timeout, long retry) {
        return new CommandPolicy(
                Attempt.retry(retry),
                retryInterval,
                retryInterval,
                timeout,
                CompositionStrategies::compose,
                Criterion.all(),
                CircuitBreakerPolicy.unlimited,
                RateLimiterPolicy.unlimited,
                ErrorType.ERROR_ON_EXCEPTION_AND_NULL);
    }

    public static CommandPolicy runOnce(Timeout timeout, long retry) {
        return new CommandPolicy(
                Attempt.retry(retry),
                Interval.nowThenTenSeconds(),
                Interval.ofMillis(1000),
                timeout,
                CompositionStrategies::compose,
                Criterion.all(),
                CircuitBreakerPolicy.unlimited,
                RateLimiterPolicy.unlimited,
                ErrorType.ERROR_ON_EXCEPTION_AND_NULL);
    }


    public static CommandPolicy runNTimes(Interval interval, long successTimes) {
        return new CommandPolicy(
                Attempt.numSuccessfulTimes(successTimes, successTimes),
                interval,
                interval,
                Timeout.ofSixtySeconds(),
                CompositionStrategies::compose,
                Criterion.all(),
                CircuitBreakerPolicy.unlimited,
                RateLimiterPolicy.unlimited,
                ErrorType.ERROR_ON_EXCEPTION_AND_NULL);
    }

    public static CommandPolicy runForever(Interval interval, Interval retryInterval, Timeout timeout) {
        return new CommandPolicy(
                Attempt.forever(),
                interval,
                retryInterval,
                timeout,
                CompositionStrategies::compose,
                Criterion.unconditional(),
                CircuitBreakerPolicy.unlimited,
                RateLimiterPolicy.unlimited,
                ErrorType.ERROR_ON_EXCEPTION_AND_NULL);
    }

    public static CommandPolicy runForever(Interval interval, Timeout timeout) {
        return new CommandPolicy(
                Attempt.forever(),
                interval,
                interval, // retryInterval == interval
                timeout,
                CompositionStrategies::compose,
                Criterion.unconditional(),
                CircuitBreakerPolicy.unlimited,
                RateLimiterPolicy.unlimited,
                ErrorType.ERROR_ON_EXCEPTION_AND_NULL);
    }

    public static CommandPolicy runForever(Interval interval) {
        return new CommandPolicy(
                Attempt.forever(),
                interval,
                interval, // retryInterval == interval
                Timeout.ofSixtySeconds(),
                CompositionStrategies::compose,
                Criterion.unconditional(),
                CircuitBreakerPolicy.unlimited,
                RateLimiterPolicy.unlimited,
                ErrorType.ERROR_ON_EXCEPTION_AND_NULL);
    }

    @Override
    public String toString() {
        return "CommandPolicy{" +
                "attempt=" + attempt +
                ", interval=" + interval +
                ", retryInterval=" + retryInterval +
                ", timeout=" + timeout +
                ", successCriterion=" + successCriterion +
                ", compositionStrategy=" + compositionStrategy +
                ", circuitBreakerPolicy=" + circuitBreakerPolicy +
                ", rateLimiterPolicy=" + rateLimiterPolicy +
                ", errorType=" + errorType +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandPolicy that = (CommandPolicy) o;
        return Objects.equals(attempt, that.attempt) &&
                Objects.equals(interval, that.interval) &&
                Objects.equals(retryInterval, that.retryInterval) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(successCriterion, that.successCriterion) &&
                Objects.equals(compositionStrategy, that.compositionStrategy) &&
                Objects.equals(circuitBreakerPolicy, that.circuitBreakerPolicy) &&
                Objects.equals(rateLimiterPolicy, that.rateLimiterPolicy) &&
                errorType == that.errorType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attempt, interval, retryInterval, timeout, successCriterion, compositionStrategy, circuitBreakerPolicy, rateLimiterPolicy, errorType);
    }
}
