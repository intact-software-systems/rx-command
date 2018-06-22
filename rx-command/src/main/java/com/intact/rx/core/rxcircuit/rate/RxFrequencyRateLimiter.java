package com.intact.rx.core.rxcircuit.rate;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.RemovedFromCacheObserver;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.policy.Frequency;
import com.intact.rx.templates.StatusTrackerTimestamped;

public class RxFrequencyRateLimiter implements RateLimiter, RemovedFromCacheObserver {
    private static final Logger log = LoggerFactory.getLogger(RxRateLimiter.class);

    private final RateLimiterId rateLimiterId;
    private final Frequency timebasedFilter;
    private final RateLimiterSubject rateLimiterSubject;
    private final AtomicReference<StatusTrackerTimestamped> slidingTimebasedFilter;

    public RxFrequencyRateLimiter(RateLimiterId rateLimiterId, Frequency timebasedFilter) {
        this.rateLimiterId = requireNonNull(rateLimiterId);
        this.timebasedFilter = requireNonNull(timebasedFilter);
        this.rateLimiterSubject = new RateLimiterSubject();
        this.slidingTimebasedFilter = !timebasedFilter.isUnlimited()
                ? new AtomicReference<>(new StatusTrackerTimestamped(timebasedFilter.getPeriod()))
                : new AtomicReference<>(null);
    }

    @Override
    public boolean allowRequest() {
        if (slidingTimebasedFilter.get() == null) {
            return true;
        }

        // If "time since accessed" is more recent than the "allowed time based period/filter", then violation of time based filter
        if (slidingTimebasedFilter.get().totalSum() > 0 && slidingTimebasedFilter.get().timeSinceMostRecentAccess().toNanos() < timebasedFilter.getPeriod().toNanos()) {
            log.debug("Violated time based filter (last < filter) {} < {} ", slidingTimebasedFilter.get().timeSinceMostRecentAccess().toMillis(), timebasedFilter.getPeriod().toMillis());
            rateLimiterSubject.onViolatedFrequency(rateLimiterId);
            return false;
        }
        slidingTimebasedFilter.get().next(1);
        return true;
    }

    @Override
    public RxFrequencyRateLimiter onViolatedFrequencyDo(VoidStrategy1<RateLimiterId> onViolatedFrequency) {
        rateLimiterSubject.onViolatedFrequencyDo(onViolatedFrequency);
        return this;
    }

    @Override
    public RxFrequencyRateLimiter onViolatedQuotaDo(VoidStrategy1<RateLimiterId> onViolatedQuota) {
        // Note: Not supported, ignore quietly.
        return this;
    }

    @Override
    public void disconnectAll() {
        rateLimiterSubject.disconnectAll();
    }

    // --------------------------------------------
    // Interface RemovedFromCacheObserver
    // --------------------------------------------

    @Override
    public void onRemovedFromCache() {
        rateLimiterSubject.disconnectAll();
    }

    @Override
    public String toString() {
        return "RxFrequencyRateLimiter{" +
                "rateLimiterId=" + rateLimiterId +
                ", timebasedFilter=" + timebasedFilter +
                ", rateLimiterSubject=" + rateLimiterSubject +
                ", slidingTimebasedFilter=" + slidingTimebasedFilter +
                '}';
    }
}
