package com.intact.rx.core.command;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxDefault;
import com.intact.rx.core.command.factory.CommandPolicyBuilder;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.Attempt;
import com.intact.rx.policy.Criterion;
import com.intact.rx.policy.Interval;
import com.intact.rx.policy.Timeout;
import com.intact.rx.templates.ConcurrentFlyweight;

public final class CommandConfig {
    private static final ConcurrentFlyweight<CommandConfig> flyweight = new ConcurrentFlyweight<>();

    private final CommandPolicy commandPolicy;
    private final CircuitId circuitBreakerId;
    private final RateLimiterId rateLimiterId;

    private CommandConfig(CommandPolicy commandPolicy, CircuitId circuitBreakerId, RateLimiterId rateLimiterId) {
        this.commandPolicy = requireNonNull(commandPolicy);
        this.circuitBreakerId = requireNonNull(circuitBreakerId);
        this.rateLimiterId = requireNonNull(rateLimiterId);
    }

    public static CommandConfig create(CommandPolicy commandPolicy, CircuitId circuitBreakerId, RateLimiterId rateLimiterId) {
        return flyweight.compute(() -> new CommandConfig(commandPolicy, circuitBreakerId, rateLimiterId));
    }

    public CommandPolicy getCommandPolicy() {
        return commandPolicy;
    }

    public CircuitId getCircuitBreakerId() {
        return circuitBreakerId;
    }

    public RateLimiterId getRateLimiterId() {
        return rateLimiterId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final CommandPolicyBuilder commandPolicyBuilder = CommandPolicyBuilder.from(RxDefault.getDefaultCommandPolicy());
        private CircuitId circuitBreakerId = RxDefault.getDefaultCircuitBreakerId();
        private RateLimiterId rateLimiterId = RxDefault.getDefaultRateLimiterId();

        public Builder withConfig(CommandConfig commandConfig) {
            withCommandPolicy(commandConfig.getCommandPolicy());
            withCircuitBreakerId(commandConfig.getCircuitBreakerId());
            withRateLimiterId(commandConfig.getRateLimiterId());
            return this;
        }

        public Builder withCommandPolicy(CommandPolicy commandPolicy) {
            this.commandPolicyBuilder.replace(commandPolicy);
            return this;
        }

        public Builder withAttempt(Attempt attempt) {
            commandPolicyBuilder.withAttempt(attempt);
            return this;
        }

        public Builder withTimeout(Timeout timeout) {
            commandPolicyBuilder.withTimeout(timeout);
            return this;
        }

        public Builder withInterval(Interval interval) {
            commandPolicyBuilder.withInterval(interval);
            return this;
        }

        public Builder withRetryInterval(Interval retryInterval) {
            commandPolicyBuilder.withRetryInterval(retryInterval);
            return this;
        }

        public Builder withCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
            commandPolicyBuilder.withCircuitBreakerPolicy(circuitBreakerPolicy);
            return this;
        }

        public Builder withRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy) {
            commandPolicyBuilder.withRateLimiterPolicy(rateLimiterPolicy);
            return this;
        }

        public Builder withSuccessCriterion(Criterion successCriterion) {
            commandPolicyBuilder.withSuccessCriterion(successCriterion);
            return this;
        }

        public Builder withCircuitBreakerId(CircuitId circuitBreakerId) {
            this.circuitBreakerId = requireNonNull(circuitBreakerId);
            return this;
        }

        public Builder withRateLimiterId(RateLimiterId rateLimiterId) {
            this.rateLimiterId = requireNonNull(rateLimiterId);
            return this;
        }

        public CommandConfig build() {
            return create(commandPolicyBuilder.build(), circuitBreakerId, rateLimiterId);
        }
    }

    @Override
    public String toString() {
        return "CommandConfig{" +
                "commandPolicy=" + commandPolicy +
                ", circuitBreakerId=" + circuitBreakerId +
                ", rateLimiterId=" + rateLimiterId +
                '}';
    }


    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandConfig that = (CommandConfig) o;
        return Objects.equals(commandPolicy, that.commandPolicy) &&
                Objects.equals(circuitBreakerId, that.circuitBreakerId) &&
                Objects.equals(rateLimiterId, that.rateLimiterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandPolicy, circuitBreakerId, rateLimiterId);
    }
}
