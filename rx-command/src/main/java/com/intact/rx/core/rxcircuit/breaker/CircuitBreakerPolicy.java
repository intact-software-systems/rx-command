package com.intact.rx.core.rxcircuit.breaker;

import java.time.Duration;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.policy.MaxLimit;
import com.intact.rx.policy.Timeout;
import com.intact.rx.templates.Validate;

@SuppressWarnings("WeakerAccess")
public final class CircuitBreakerPolicy {
    public static final CircuitBreakerPolicy unlimited = new CircuitBreakerPolicy(MaxLimit.unlimited(), Timeout.no(), Duration.ZERO);

    private final MaxLimit maxFailures;
    private final Timeout resetTimeout;
    private final Duration errorWindow;

    public CircuitBreakerPolicy(MaxLimit maxFailures, Timeout resetTimeout, Duration errorWindow) {
        this.maxFailures = requireNonNull(maxFailures);
        this.resetTimeout = requireNonNull(resetTimeout);
        this.errorWindow = requireNonNull(errorWindow);

        if (!maxFailures.isUnlimited()) {
            Validate.assertTrue(!resetTimeout.isForever());
        }
    }


    public long getMaxFailures() {
        return maxFailures.getLimit();
    }

    public Timeout getResetTimeout() {
        return resetTimeout;
    }

    public Duration getErrorWindow() {
        return errorWindow;
    }

    public boolean isUnlimited() {
        return maxFailures.isUnlimited();
    }


    public static CircuitBreakerPolicy create(MaxLimit maxFailures, Timeout resetTimeout, Duration errorWindow) {
        return new CircuitBreakerPolicy(maxFailures, resetTimeout, errorWindow);
    }

    @Override
    public String toString() {
        return "CircuitBreakerPolicy{" +
                "maxFailures=" + maxFailures +
                ", resetTimeout=" + resetTimeout +
                ", errorWindow=" + errorWindow +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CircuitBreakerPolicy that = (CircuitBreakerPolicy) o;
        return Objects.equals(maxFailures, that.maxFailures) &&
                Objects.equals(resetTimeout, that.resetTimeout) &&
                Objects.equals(errorWindow, that.errorWindow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxFailures, resetTimeout, errorWindow);
    }
}
