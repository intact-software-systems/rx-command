package com.intact.rx.core.rxcircuit.rate;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.RemovedFromCacheObserver;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.policy.TimeRate;
import com.intact.rx.templates.StatusTrackerTimestamped;

public class RxQuotaRateLimiter implements RateLimiter, RemovedFromCacheObserver {
    private static final Logger log = LoggerFactory.getLogger(RxRateLimiter.class);

    private final RateLimiterId rateLimiterId;
    private final TimeRate timebasedQuota;
    private final RateLimiterSubject rateLimiterSubject;
    private final AtomicReference<StatusTrackerTimestamped> slidingWindowQuota;

    public RxQuotaRateLimiter(RateLimiterId rateLimiterId, TimeRate timebasedQuota) {
        this.rateLimiterId = requireNonNull(rateLimiterId);
        this.timebasedQuota = requireNonNull(timebasedQuota);
        this.rateLimiterSubject = new RateLimiterSubject();
        this.slidingWindowQuota = !timebasedQuota.isUnlimited()
                ? new AtomicReference<>(new StatusTrackerTimestamped(timebasedQuota.getDuration()))
                : new AtomicReference<>(null);
    }

    @Override
    public boolean allowRequest() {
        if (slidingWindowQuota.get() == null) {
            return true;
        }

        // If the number of calls in this period exceeds allowed quota, then violation of time based quota
        if (slidingWindowQuota.get().sumInWindow() >= timebasedQuota.getTotal()) {
            log.debug("Violated time based quota {} with {} calls", timebasedQuota, slidingWindowQuota.get().sumInWindow());
            rateLimiterSubject.onViolatedQuota(rateLimiterId);
            return false;
        }
        slidingWindowQuota.get().next(1);
        return true;
    }

    @Override
    public RateLimiter onViolatedFrequencyDo(VoidStrategy1<RateLimiterId> onViolatedFrequency) {
        // Note: Not supported, ignore quietly.
        return this;
    }

    @Override
    public RateLimiter onViolatedQuotaDo(VoidStrategy1<RateLimiterId> onViolatedQuota) {
        rateLimiterSubject.onViolatedQuotaDo(onViolatedQuota);
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
        return "RxQuotaRateLimiter{" +
                "rateLimiterId=" + rateLimiterId +
                ", timebasedQuota=" + timebasedQuota +
                ", rateLimiterSubject=" + rateLimiterSubject +
                ", slidingWindowQuota=" + slidingWindowQuota +
                '}';
    }
}
