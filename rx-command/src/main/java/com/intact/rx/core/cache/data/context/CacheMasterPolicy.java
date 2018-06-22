package com.intact.rx.core.cache.data.context;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.command.Strategy1;
import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.core.cache.strategy.CacheCleanupAlgorithms;
import com.intact.rx.policy.Extension;
import com.intact.rx.policy.Lifetime;

public class CacheMasterPolicy {
    private static final Strategy1<Boolean, CacheMaster> CLEANUP_CACHE_MASTER = CacheCleanupAlgorithms::cleanupCacheMaster;

    private final Lifetime lifetime;
    private final Extension extension;
    private final Strategy1<Boolean, CacheMaster> cacheMasterCleanupStrategy;

    public CacheMasterPolicy(
            Lifetime lifetime,
            Extension extension,
            Strategy1<Boolean, CacheMaster> cacheMasterCleanupStrategy) {
        this.lifetime = requireNonNull(lifetime);
        this.extension = requireNonNull(extension);
        this.cacheMasterCleanupStrategy = requireNonNull(cacheMasterCleanupStrategy);
    }

    public Lifetime getLifetime() {
        return lifetime;
    }

    public Extension getExtension() {
        return extension;
    }

    public Strategy1<Boolean, CacheMaster> getCacheMasterCleanupStrategy() {
        return cacheMasterCleanupStrategy;
    }

    public static CacheMasterPolicy validForever() {
        return new CacheMasterPolicy(Lifetime.forever(), Extension.noRenew(), CLEANUP_CACHE_MASTER);
    }

    public static CacheMasterPolicy validUntil(Lifetime lifetime, Extension extension) {
        return new CacheMasterPolicy(lifetime, extension, CLEANUP_CACHE_MASTER);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "[lifetime:" + lifetime + " extension:" + extension + " cacheMasterCleanupStrategy=" + cacheMasterCleanupStrategy + "]";
    }
}
