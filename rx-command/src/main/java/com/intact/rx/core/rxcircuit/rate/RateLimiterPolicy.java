package com.intact.rx.core.rxcircuit.rate;

import java.time.Duration;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.policy.Frequency;
import com.intact.rx.policy.TimeRate;

@SuppressWarnings("WeakerAccess")
public final class RateLimiterPolicy {
    public static final RateLimiterPolicy unlimited = new RateLimiterPolicy(TimeRate.unlimited, new Frequency(Duration.ofMillis(Long.MAX_VALUE)));

    private final TimeRate timebasedQuota;
    private final Frequency timebasedFilter;

    private RateLimiterPolicy(TimeRate timebasedQuota, Frequency timebasedFilter) {
        this.timebasedQuota = requireNonNull(timebasedQuota);
        this.timebasedFilter = requireNonNull(timebasedFilter);
    }

    public TimeRate getTimebasedQuota() {
        return timebasedQuota;
    }

    public Frequency getTimebasedFilter() {
        return timebasedFilter;
    }

    public boolean isUnlimited() {
        return timebasedQuota.isUnlimited() && timebasedFilter.isUnlimited();
    }


    public static RateLimiterPolicy timeBasedQuota(TimeRate timeBasedQuota) {
        return new RateLimiterPolicy(timeBasedQuota, Frequency.unlimited);
    }

    public static RateLimiterPolicy timeBasedFilter(Frequency timeBasedFilter) {
        return new RateLimiterPolicy(TimeRate.unlimited, timeBasedFilter);
    }

    public static RateLimiterPolicy create(TimeRate timeBasedQuota, Frequency timeBasedFilter) {
        return new RateLimiterPolicy(timeBasedQuota, timeBasedFilter);
    }


    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateLimiterPolicy that = (RateLimiterPolicy) o;
        return Objects.equals(timebasedQuota, that.timebasedQuota) &&
                Objects.equals(timebasedFilter, that.timebasedFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timebasedQuota, timebasedFilter);
    }

    @Override
    public String toString() {
        return "RateLimiterPolicy{" +
                "timebasedQuota=" + timebasedQuota +
                ", timebasedFilter=" + timebasedFilter +
                '}';
    }
}
