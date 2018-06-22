package com.intact.rx.api.cache;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.context.ObjectRootPolicy;
import com.intact.rx.policy.Extension;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.ResourceLimits;

public final class CachePolicy {
    private static final CachePolicy unlimited5Min = unlimited(Lifetime.ofMinutes(5));
    private static final CachePolicy unlimitedHour = unlimited(Lifetime.ofHours(1));
    private static final CachePolicy unlimitedForever = unlimited(Lifetime.forever());

    private final DataCachePolicy dataCachePolicy;
    private final CacheMasterPolicy cacheMasterPolicy;

    private CachePolicy(ResourceLimits limits, Lifetime lifetime) {
        this.cacheMasterPolicy = CacheMasterPolicy.validUntil(Lifetime.forever(), Extension.noRenew());
        this.dataCachePolicy = DataCachePolicy.leastRecentlyUsedAnd(limits, Lifetime.forever(), ObjectRootPolicy.create(lifetime));
    }

    private CachePolicy(CacheMasterPolicy cacheMasterPolicy, DataCachePolicy dataCachePolicy) {
        this.cacheMasterPolicy = requireNonNull(cacheMasterPolicy);
        this.dataCachePolicy = requireNonNull(dataCachePolicy);
    }

    public CacheMasterPolicy getCacheMasterPolicy() {
        return cacheMasterPolicy;
    }

    public DataCachePolicy getDataCachePolicy() {
        return dataCachePolicy;
    }

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static CachePolicy fiveMinutesUnlimited() {
        return unlimited5Min;
    }

    public static CachePolicy oneHourUnlimited() {
        return unlimitedHour;
    }

    public static CachePolicy unlimitedForever() {
        return unlimitedForever;
    }

    public static CachePolicy unlimited(Lifetime lifetime) {
        return new CachePolicy(ResourceLimits.unlimited(), lifetime);
    }

    public static CachePolicy create(ResourceLimits limits, Lifetime lifetime) {
        return new CachePolicy(limits, lifetime);
    }

    public static CachePolicy create(CacheMasterPolicy cacheMasterPolicy, DataCachePolicy dataCachePolicy) {
        return new CachePolicy(cacheMasterPolicy, dataCachePolicy);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + cacheMasterPolicy + " " + dataCachePolicy + "]";
    }
}
