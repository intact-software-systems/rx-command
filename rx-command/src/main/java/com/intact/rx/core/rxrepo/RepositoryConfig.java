package com.intact.rx.core.rxrepo;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.Access;

@SuppressWarnings("WeakerAccess")
public final class RepositoryConfig {
    private static final Strategy0<Access> allAccess = () -> Access.ALL;

    private final Strategy0<Access> readAccess;
    private final Strategy0<Access> writeAccess;
    private final CircuitBreakerPolicy circuitBreakerPolicy;
    private final RateLimiterPolicy rateLimiterPolicy;
    private final CircuitId circuitBreakerId;
    private final RateLimiterId rateLimiterId;

    private RepositoryConfig(Strategy0<Access> readAccess, Strategy0<Access> writeAccess, CircuitId circuitBreakerId, CircuitBreakerPolicy circuitBreakerPolicy, RateLimiterId rateLimiterId, RateLimiterPolicy rateLimiterPolicy) {
        this.readAccess = requireNonNull(readAccess);
        this.writeAccess = requireNonNull(writeAccess);
        this.circuitBreakerPolicy = requireNonNull(circuitBreakerPolicy);
        this.circuitBreakerId = requireNonNull(circuitBreakerId);
        this.rateLimiterId = requireNonNull(rateLimiterId);
        this.rateLimiterPolicy = requireNonNull(rateLimiterPolicy);
    }

    public Strategy0<Access> getReadAccess() {
        return readAccess;
    }

    public Strategy0<Access> getWriteAccess() {
        return writeAccess;
    }

    public CircuitBreakerPolicy getCircuitBreakerPolicy() {
        return circuitBreakerPolicy;
    }

    public RateLimiterPolicy getRateLimiterPolicy() {
        return rateLimiterPolicy;
    }

    public RateLimiterId getRateLimiterId() {
        return rateLimiterId;
    }

    public CircuitId getCircuitBreakerId() {
        return circuitBreakerId;
    }

    public boolean isAllowAll() {
        return Objects.equals(readAccess, allAccess) && Objects.equals(writeAccess, allAccess);
    }

    public static RepositoryConfig fromRxDefault() {
        return new RepositoryConfig(
                allAccess,
                allAccess,
                CircuitId.builder()
                        .withCircuitBreakerId(RxDefault.getDefaultCircuitBreakerId().getHandle().getKey())
                        .withCircuitScope(RxDefault.getRepositoryCircuitCacheHandle())
                        .build(),
                RxDefault.getDefaultActsControllerPolicy().getCircuitBreakerPolicy(),
                RateLimiterId.builder()
                        .withRateLimiterId(RxDefault.getDefaultRateLimiterId().getHandle().getKey())
                        .withRateLimiterScope(RxDefault.getRepositoryCircuitCacheHandle())
                        .build(),
                RxDefault.getDefaultActsControllerPolicy().getRateLimiterPolicy()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder buildFrom(RepositoryConfig repositoryConfig) {
        return new Builder(requireNonNull(repositoryConfig));
    }

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public static class Builder {
        private Strategy0<Access> readAccess;
        private Strategy0<Access> writeAccess;
        private CircuitBreakerPolicy circuitBreakerPolicy;
        private RateLimiterPolicy rateLimiterPolicy;
        private final CircuitId.Builder circuitBreakerIdBuilder;
        private final RateLimiterId.Builder rateLimiterIdBuilder;

        public Builder() {
            this.readAccess = allAccess;
            this.writeAccess = allAccess;
            this.circuitBreakerPolicy = RxDefault.getDefaultCommandPolicy().getCircuitBreakerPolicy();
            this.rateLimiterPolicy = RxDefault.getDefaultCommandPolicy().getRateLimiterPolicy();
            this.circuitBreakerIdBuilder = CircuitId.buildFrom(RxDefault.getDefaultCircuitBreakerId()).withCircuitScope(RxDefault.getRepositoryCircuitCacheHandle());
            this.rateLimiterIdBuilder = RateLimiterId.buildFrom(RxDefault.getDefaultRateLimiterId()).withRateLimiterScope(RxDefault.getRepositoryCircuitCacheHandle());
        }

        public Builder(RepositoryConfig repositoryConfig) {
            this.readAccess = requireNonNull(repositoryConfig.readAccess);
            this.writeAccess = requireNonNull(repositoryConfig.writeAccess);
            this.circuitBreakerPolicy = requireNonNull(repositoryConfig.circuitBreakerPolicy);
            this.rateLimiterPolicy = requireNonNull(repositoryConfig.rateLimiterPolicy);
            this.circuitBreakerIdBuilder = CircuitId.buildFrom(repositoryConfig.circuitBreakerId);
            this.rateLimiterIdBuilder = RateLimiterId.buildFrom(repositoryConfig.rateLimiterId);
        }

        public Builder withReadAccessControl(Strategy0<Access> accessControl) {
            this.readAccess = requireNonNull(accessControl);
            return this;
        }

        public Builder withWriteAccessControl(Strategy0<Access> writeAccess) {
            this.writeAccess = requireNonNull(writeAccess);
            return this;
        }

        public Builder withCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
            this.circuitBreakerPolicy = requireNonNull(circuitBreakerPolicy);
            return this;
        }

        public Builder withCircuitBreakerIsolatedById(Object circuitId) {
            this.circuitBreakerIdBuilder.withCircuitBreakerId(requireNonNull(circuitId));
            this.circuitBreakerIdBuilder.withCircuitScope(CacheHandle.uuid());
            return this;
        }

        public Builder withCircuitBreakerSharedById(Object circuitId) {
            this.circuitBreakerIdBuilder.withCircuitBreakerId(requireNonNull(circuitId));
            this.circuitBreakerIdBuilder.withCircuitScope(RxDefault.getRepositoryCircuitCacheHandle());
            return this;
        }

        public Builder withRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy) {
            this.rateLimiterPolicy = requireNonNull(rateLimiterPolicy);
            return this;
        }

        public Builder withRateLimiterIsolatedById(Object circuitId) {
            this.rateLimiterIdBuilder.withRateLimiterId(requireNonNull(circuitId));
            this.rateLimiterIdBuilder.withRateLimiterScope(CacheHandle.uuid());
            return this;
        }

        public Builder withRateLimiterSharedById(Object circuitId) {
            this.rateLimiterIdBuilder.withRateLimiterId(requireNonNull(circuitId));
            this.rateLimiterIdBuilder.withRateLimiterScope(RxDefault.getRepositoryCircuitCacheHandle());
            return this;
        }

        public RepositoryConfig build() {
            return new RepositoryConfig(
                    readAccess,
                    writeAccess,
                    circuitBreakerIdBuilder.build(),
                    circuitBreakerPolicy,
                    rateLimiterIdBuilder.build(),
                    rateLimiterPolicy
            );
        }
    }

    @Override
    public String toString() {
        return "RepositoryConfig{" +
                "readAccess=" + readAccess +
                ", writeAccess=" + writeAccess +
                ", circuitBreakerPolicy=" + circuitBreakerPolicy +
                ", circuitBreakerId=" + circuitBreakerId +
                ", rateLimiterId=" + rateLimiterId +
                '}';
    }
}
