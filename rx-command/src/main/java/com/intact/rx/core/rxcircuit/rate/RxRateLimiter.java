package com.intact.rx.core.rxcircuit.rate;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.RemovedFromCacheObserver;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.templates.StatusTrackerTimestamped;

@SuppressWarnings("WeakerAccess")
public class RxRateLimiter implements RateLimiter, RemovedFromCacheObserver {
    private static final Logger log = LoggerFactory.getLogger(RxRateLimiter.class);

    private final RateLimiterId rateLimiterId;
    private final RateLimiterPolicy rateLimiterPolicy;
    private final RateLimiterSubject rateLimiterSubject;
    private final AtomicReference<StatusTrackerTimestamped> slidingWindowQuota;
    private final AtomicReference<StatusTrackerTimestamped> slidingTimebasedFilter;

    public RxRateLimiter(RateLimiterId rateLimiterId, RateLimiterPolicy rateLimiterPolicy) {
        this.rateLimiterId = requireNonNull(rateLimiterId);
        this.rateLimiterPolicy = requireNonNull(rateLimiterPolicy);
        this.rateLimiterSubject = new RateLimiterSubject();

        this.slidingWindowQuota = !rateLimiterPolicy.getTimebasedQuota().isUnlimited()
                ? new AtomicReference<>(new StatusTrackerTimestamped(rateLimiterPolicy.getTimebasedQuota().getDuration()))
                : new AtomicReference<>(null);

        this.slidingTimebasedFilter = !rateLimiterPolicy.getTimebasedFilter().isUnlimited()
                ? new AtomicReference<>(new StatusTrackerTimestamped(rateLimiterPolicy.getTimebasedFilter().getPeriod()))
                : new AtomicReference<>(null);
    }

    @Override
    public boolean allowRequest() {

        boolean violatedQuota = false;
        if (slidingWindowQuota.get() != null) {
            // If the number of calls in this period exceeds allowed quota, then violation of time based quota
            if (slidingWindowQuota.get().sumInWindow() >= rateLimiterPolicy.getTimebasedQuota().getTotal()) {
                log.debug("Violated time based quota {} with {} calls", rateLimiterPolicy.getTimebasedQuota(), slidingWindowQuota.get().sumInWindow());
                rateLimiterSubject.onViolatedQuota(rateLimiterId);
                violatedQuota = true;
            }
            if (!violatedQuota) {
                slidingWindowQuota.get().next(1);
            }
        }

        boolean violatedFrequencey = false;
        if (slidingTimebasedFilter.get() != null) {
            // If "time since accessed" is more recent than the "allowed time based period/filter", then violation of time based filter
            if (slidingTimebasedFilter.get().totalSum() > 0 && slidingTimebasedFilter.get().timeSinceMostRecentAccess().toNanos() < rateLimiterPolicy.getTimebasedFilter().getPeriod().toNanos()) {
                log.debug("Violated time based filter (last < filter) {} < {} ", slidingTimebasedFilter.get().timeSinceMostRecentAccess().toMillis(), rateLimiterPolicy.getTimebasedFilter().getPeriod().toMillis());
                rateLimiterSubject.onViolatedFrequency(rateLimiterId);
                violatedFrequencey = true;
            }

            if (!violatedFrequencey) {
                slidingTimebasedFilter.get().next(1);
            }
        }

        return !violatedQuota && !violatedFrequencey;
    }

    @Override
    public RxRateLimiter onViolatedFrequencyDo(VoidStrategy1<RateLimiterId> onViolated) {
        rateLimiterSubject.onViolatedFrequencyDo(onViolated);
        return this;
    }

    @Override
    public RxRateLimiter onViolatedQuotaDo(VoidStrategy1<RateLimiterId> onViolated) {
        rateLimiterSubject.onViolatedQuotaDo(onViolated);
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
        return "RxRateLimiter{" +
                "rateLimiterId=" + rateLimiterId +
                ", rateLimiterPolicy=" + rateLimiterPolicy +
                ", rateLimiterSubject=" + rateLimiterSubject +
                ", slidingWindowQuota=" + slidingWindowQuota +
                '}';
    }
}
