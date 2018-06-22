package com.intact.rx.core.command.factory;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.command.Strategy2;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.*;

public class CommandPolicyBuilder {
    private Attempt attempt;
    private Interval interval;
    private Interval retryInterval;
    private Timeout timeout;
    @SuppressWarnings("rawtypes")
    private Strategy2<Boolean, CommandPolicy, Command> compositionStrategy;
    private Criterion successCriterion;
    private CircuitBreakerPolicy circuitBreakerPolicy;
    private RateLimiterPolicy rateLimiterPolicy;
    private ErrorType errorType;

    private CommandPolicyBuilder() {
        fromPolicy(RxDefault.getDefaultCommandPolicy());
    }

    private CommandPolicyBuilder(CommandPolicy commandPolicy) {
        fromPolicy(commandPolicy);
    }

    private void fromPolicy(CommandPolicy commandPolicy) {
        requireNonNull(commandPolicy);

        this.attempt = commandPolicy.getAttempt();
        this.interval = commandPolicy.getInterval();
        this.retryInterval = commandPolicy.getRetryInterval();
        this.timeout = commandPolicy.getTimeout();
        this.compositionStrategy = commandPolicy.getCompositionStrategy();
        this.successCriterion = commandPolicy.getSuccessCriterion();
        this.circuitBreakerPolicy = commandPolicy.getCircuitBreakerPolicy();
        this.rateLimiterPolicy = commandPolicy.getRateLimiterPolicy();
        this.errorType = commandPolicy.getErrorType();
    }

    public static CommandPolicyBuilder from(CommandPolicy commandPolicy) {
        return new CommandPolicyBuilder(requireNonNull(commandPolicy));
    }

    public static CommandPolicyBuilder fromDefaults() {
        return new CommandPolicyBuilder();
    }

    public CommandPolicyBuilder replace(CommandPolicy commandPolicy) {
        fromPolicy(commandPolicy);
        return this;
    }

    public CommandPolicyBuilder withAttempt(Attempt attempt) {
        this.attempt = requireNonNull(attempt);
        return this;
    }

    public CommandPolicyBuilder withInterval(Interval interval) {
        this.interval = requireNonNull(interval);
        return this;
    }

    public CommandPolicyBuilder withRetryInterval(Interval retryInterval) {
        this.retryInterval = requireNonNull(retryInterval);
        return this;
    }

    public CommandPolicyBuilder withTimeout(Timeout timeout) {
        this.timeout = requireNonNull(timeout);
        return this;
    }

    public CommandPolicyBuilder withCompositionStrategy(@SuppressWarnings("rawtypes") Strategy2<Boolean, CommandPolicy, Command> compositionStrategy) {
        this.compositionStrategy = requireNonNull(compositionStrategy);
        return this;
    }

    public CommandPolicyBuilder withSuccessCriterion(Criterion successCriterion) {
        this.successCriterion = requireNonNull(successCriterion);
        return this;
    }

    public CommandPolicyBuilder withCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
        this.circuitBreakerPolicy = requireNonNull(circuitBreakerPolicy);
        return this;
    }

    public CommandPolicyBuilder withRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy) {
        this.rateLimiterPolicy = requireNonNull(rateLimiterPolicy);
        return this;
    }

    public CommandPolicyBuilder withErrorOnNull() {
        this.errorType = ErrorType.ERROR_ON_EXCEPTION_AND_NULL;
        return this;
    }

    public CommandPolicy build() {
        return new CommandPolicy(attempt, interval, retryInterval, timeout, compositionStrategy, successCriterion, circuitBreakerPolicy, rateLimiterPolicy, errorType);
    }
}
