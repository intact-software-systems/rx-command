package com.intact.rx.core.rxcache.controller;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.Attempt;
import com.intact.rx.policy.Interval;
import com.intact.rx.policy.Timeout;

public final class ActsSingleExecutionPolicy {
    private final Attempt attempt;
    private final Interval interval;
    private final Interval retryInterval;
    private final Timeout timeout;
    private final CircuitBreakerPolicy circuitBreakerPolicy;
    private final RateLimiterPolicy rateLimiterPolicy;

    private ActsSingleExecutionPolicy(long retry, Interval retryInterval, Timeout timeout, CircuitBreakerPolicy circuitBreakerPolicy, RateLimiterPolicy rateLimiterPolicy) {
        this.attempt = Attempt.retry(retry);
        this.retryInterval = requireNonNull(retryInterval);
        this.timeout = requireNonNull(timeout);
        this.circuitBreakerPolicy = requireNonNull(circuitBreakerPolicy);
        this.rateLimiterPolicy = requireNonNull(rateLimiterPolicy);
        this.interval = Interval.no();
    }

    public ActsControllerPolicy toActsControllerPolicy() {
        return new ActsControllerPolicy(
                this.interval,
                this.attempt,
                this.retryInterval,
                this.timeout,
                this.circuitBreakerPolicy,
                this.rateLimiterPolicy
        );
    }

    public static ActsSingleExecutionPolicy create(Timeout timeout, CircuitBreakerPolicy circuitBreakerPolicy, RateLimiterPolicy rateLimiterPolicy) {
        return new ActsSingleExecutionPolicy(0, Interval.no(), timeout, circuitBreakerPolicy, rateLimiterPolicy);
    }

    public static ActsSingleExecutionPolicy create(long retry, Interval retryInterval, Timeout timeout, CircuitBreakerPolicy circuitBreakerPolicy, RateLimiterPolicy rateLimiterPolicy) {
        return new ActsSingleExecutionPolicy(retry, retryInterval, timeout, circuitBreakerPolicy, rateLimiterPolicy);
    }

    @Override
    public String toString() {
        return "ActsSingleExecutionPolicy{" +
                "attempt=" + attempt +
                ", interval=" + interval +
                ", retryInterval=" + retryInterval +
                ", timeout=" + timeout +
                ", circuitBreakerPolicy=" + circuitBreakerPolicy +
                ", rateLimiterPolicy=" + rateLimiterPolicy +
                '}';
    }
}
