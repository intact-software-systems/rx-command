package com.intact.rx.core.rxcache.controller;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.Attempt;
import com.intact.rx.policy.Interval;
import com.intact.rx.policy.Timeout;

@SuppressWarnings("WeakerAccess")
public final class ActsSchedulerPolicy {
    private final Attempt attempt;
    private final Interval interval;
    private final Interval retryInterval;
    private final Timeout timeout;
    private final CircuitBreakerPolicy circuitBreakerPolicy;
    private final RateLimiterPolicy rateLimiterPolicy;

    public ActsSchedulerPolicy(Interval interval, Attempt attempt, Interval retryInterval, Timeout timeout, CircuitBreakerPolicy circuitBreakerPolicy, RateLimiterPolicy rateLimiterPolicy) {
        this.interval = requireNonNull(interval);
        this.attempt = requireNonNull(attempt);
        this.retryInterval = requireNonNull(retryInterval);
        this.timeout = requireNonNull(timeout);
        this.circuitBreakerPolicy = requireNonNull(circuitBreakerPolicy);
        this.rateLimiterPolicy = requireNonNull(rateLimiterPolicy);
    }

    public Interval getInterval() {
        return interval;
    }

    public Attempt getAttempt() {
        return attempt;
    }

    public Interval getRetryInterval() {
        return retryInterval;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public CircuitBreakerPolicy getCircuitBreakerPolicy() {
        return circuitBreakerPolicy;
    }

    public RateLimiterPolicy getRateLimiterPolicy() {
        return rateLimiterPolicy;
    }

    @Override
    public String toString() {
        return "ActsSchedulerPolicy{" +
                "attempt=" + attempt +
                ", interval=" + interval +
                ", retryInterval=" + retryInterval +
                ", timeout=" + timeout +
                ", circuitBreakerPolicy=" + circuitBreakerPolicy +
                ", rateLimiterPolicy=" + rateLimiterPolicy +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActsSchedulerPolicy)) return false;
        ActsSchedulerPolicy that = (ActsSchedulerPolicy) o;
        return Objects.equals(attempt, that.attempt) &&
                Objects.equals(interval, that.interval) &&
                Objects.equals(retryInterval, that.retryInterval) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(circuitBreakerPolicy, that.circuitBreakerPolicy) &&
                Objects.equals(rateLimiterPolicy, that.rateLimiterPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attempt, interval, retryInterval, timeout, circuitBreakerPolicy, rateLimiterPolicy);
    }
}
