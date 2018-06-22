package com.intact.rx.core.rxcache.controller;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxDefault;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.templates.ConcurrentFlyweight;

public final class ActsControllerConfig {
    private static final ConcurrentFlyweight<ActsControllerConfig> flyweight = new ConcurrentFlyweight<>();

    private final ActsControllerPolicy actsControllerPolicy;
    private final CircuitId circuitBreakerId;
    private final RateLimiterId rateLimiterId;

    private ActsControllerConfig(ActsControllerPolicy actsControllerPolicy, CircuitId circuitBreakerId, RateLimiterId rateLimiterId) {
        this.actsControllerPolicy = requireNonNull(actsControllerPolicy);
        this.circuitBreakerId = requireNonNull(circuitBreakerId);
        this.rateLimiterId = requireNonNull(rateLimiterId);
    }

    public static ActsControllerConfig create(ActsControllerPolicy actsControllerPolicy, CircuitId circuitBreakerId, RateLimiterId rateLimiterId) {
        return flyweight.compute(() -> new ActsControllerConfig(actsControllerPolicy, circuitBreakerId, rateLimiterId));
    }

    public ActsControllerPolicy getActsControllerPolicy() {
        return actsControllerPolicy;
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
        private ActsControllerPolicy actsControllerPolicy = RxDefault.getDefaultActsControllerPolicy();
        private CircuitId circuitBreakerId = RxDefault.getDefaultCircuitBreakerId();
        private RateLimiterId rateLimiterId = RxDefault.getDefaultRateLimiterId();

        public Builder withActsControllerPolicy(ActsControllerPolicy actsControllerPolicy) {
            this.actsControllerPolicy = requireNonNull(actsControllerPolicy);
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

        public Builder withConfig(ActsControllerConfig actsControllerConfig) {
            this.actsControllerPolicy = actsControllerConfig.getActsControllerPolicy();
            this.circuitBreakerId = actsControllerConfig.getCircuitBreakerId();
            this.rateLimiterId = actsControllerConfig.getRateLimiterId();
            return this;
        }

        public ActsControllerConfig build() {
            return create(actsControllerPolicy, circuitBreakerId, rateLimiterId);
        }
    }

    @Override
    public String toString() {
        return "ActsControllerConfig{" +
                "actsControllerPolicy=" + actsControllerPolicy +
                ", circuitBreakerId=" + circuitBreakerId +
                ", rateLimiterId=" + rateLimiterId +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActsControllerConfig that = (ActsControllerConfig) o;
        return Objects.equals(actsControllerPolicy, that.actsControllerPolicy) &&
                Objects.equals(circuitBreakerId, that.circuitBreakerId) &&
                Objects.equals(rateLimiterId, that.rateLimiterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actsControllerPolicy, circuitBreakerId, rateLimiterId);
    }
}
