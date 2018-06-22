package com.intact.rx.core.rxcircuit.rate;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.rxcircuit.RateLimiterObserver;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.context.ObjectRootPolicy;
import com.intact.rx.policy.Extension;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.ResourceLimits;

public class RateLimiterCache implements RateLimiterObserver {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterCache.class);

    private static final RateLimiterSubject rateLimiterSubject = new RateLimiterSubject();
    private static final RateLimiterCache instance = new RateLimiterCache();
    private static final CachePolicy rateLimiterCachePolicy =
            CachePolicy.create(
                    CacheMasterPolicy.validForever(),
                    DataCachePolicy.leastRecentlyUsedAnd(
                            ResourceLimits.unlimited(),
                            Lifetime.forever(),
                            ObjectRootPolicy.create(Lifetime.ofHours(1), Extension.renewOnRead())
                    )
            );

    public static RateLimiterSubject observeAll() {
        return rateLimiterSubject;
    }

    public static RateLimiter rateLimiter(RateLimiterId rateLimiterId, RateLimiterPolicy rateLimiterPolicy) {
        if (rateLimiterId.isNone() || rateLimiterPolicy.isUnlimited()) {
            return RateLimiterAlwaysAllow.instance;
        }

        return cache(rateLimiterId.getCacheHandle())
                .computeIfAbsent(
                        rateLimiterId,
                        id -> new RxRateLimiter(rateLimiterId, rateLimiterPolicy)
                                .onViolatedFrequencyDo(instance::onViolatedFrequency)
                                .onViolatedQuotaDo(instance::onViolatedQuota)
                );
    }

    public static Optional<RateLimiter> find(RateLimiterId rateLimiterId) {
        return cache(rateLimiterId.getCacheHandle()).read(rateLimiterId);
    }

    private static RxCache<RateLimiterId, RateLimiter> cache(CacheHandle handle) {
        return RxCacheAccess.cache(handle, rateLimiterCachePolicy);
    }

    // --------------------------------------------
    // Interface RateLimiterObserver
    // --------------------------------------------

    @Override
    public void onViolatedFrequency(RateLimiterId rateLimiterId) {
        log.info("Rate limit by frequency violated for {}", rateLimiterId);
        rateLimiterSubject.onViolatedFrequency(rateLimiterId);
    }

    @Override
    public void onViolatedQuota(RateLimiterId rateLimiterId) {
        log.info("Rate limit by quota violated for {}", rateLimiterId);
        rateLimiterSubject.onViolatedQuota(rateLimiterId);
    }
}
