package com.intact.rx.core.rxcache.controller;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.Attempt;
import com.intact.rx.policy.Interval;
import com.intact.rx.policy.Timeout;

@SuppressWarnings("WeakerAccess")
public final class ActsControllerPolicy {
    public static final ActsControllerPolicy oneTimePolicy = new ActsControllerPolicy(
            Interval.no(),
            Attempt.once(),
            Interval.no(),
            Timeout.no(),
            CircuitBreakerPolicy.unlimited,
            RateLimiterPolicy.unlimited);

    private final Attempt attempt;
    private final Interval groupInterval;
    private final Interval retryGroupInterval;
    private final Timeout timeout;
    private final CircuitBreakerPolicy circuitBreakerPolicy;
    private final RateLimiterPolicy rateLimiterPolicy;

    public ActsControllerPolicy(Interval groupInterval, Attempt attempt, Interval retryGroupInterval, Timeout timeout, CircuitBreakerPolicy circuitBreakerPolicy, RateLimiterPolicy rateLimiterPolicy) {
        this.groupInterval = requireNonNull(groupInterval);
        this.attempt = requireNonNull(attempt);
        this.retryGroupInterval = requireNonNull(retryGroupInterval);
        this.timeout = requireNonNull(timeout);
        this.circuitBreakerPolicy = requireNonNull(circuitBreakerPolicy);
        this.rateLimiterPolicy = requireNonNull(rateLimiterPolicy);
    }

    public Interval getGroupInterval() {
        return groupInterval;
    }

    public Attempt getAttempt() {
        return attempt;
    }

    public Interval getRetryGroupInterval() {
        return retryGroupInterval;
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

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static ActsControllerPolicy from(ActsControllerPolicy policy, CircuitBreakerPolicy circuitBreakerPolicy, RateLimiterPolicy rateLimiterPolicy) {
        return new ActsControllerPolicy(
                policy.groupInterval,
                policy.attempt,
                policy.retryGroupInterval,
                policy.timeout,
                circuitBreakerPolicy,
                rateLimiterPolicy);
    }

    @Override
    public String toString() {
        return "ActsControllerPolicy{" +
                "attempt=" + attempt +
                ", groupInterval=" + groupInterval +
                ", retryGroupInterval=" + retryGroupInterval +
                ", timeout=" + timeout +
                ", circuitBreakerPolicy=" + circuitBreakerPolicy +
                ", rateLimiterPolicy=" + rateLimiterPolicy +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActsControllerPolicy that = (ActsControllerPolicy) o;
        return Objects.equals(attempt, that.attempt) &&
                Objects.equals(groupInterval, that.groupInterval) &&
                Objects.equals(retryGroupInterval, that.retryGroupInterval) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(circuitBreakerPolicy, that.circuitBreakerPolicy) &&
                Objects.equals(rateLimiterPolicy, that.rateLimiterPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attempt, groupInterval, retryGroupInterval, timeout, circuitBreakerPolicy, rateLimiterPolicy);
    }
}
