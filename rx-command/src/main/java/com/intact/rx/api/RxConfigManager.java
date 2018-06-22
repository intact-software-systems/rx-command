package com.intact.rx.api;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.policy.Lifetime;

@SuppressWarnings("WeakerAccess")
public class RxConfigManager {
    private RxConfig defaultRxConfig = RxConfig.fromRxDefault();

    private final CacheHandle cacheHandle;

    private RxConfigManager(MasterCacheId masterCacheId) {
        requireNonNull(masterCacheId);
        this.cacheHandle = CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), masterCacheId, RxConfig.class);
    }

    public static RxConfigManager forSystem(MasterCacheId masterCacheId) {
        return new RxConfigManager(masterCacheId);
    }

    public static RxConfigManager forSystem(Object systemKey) {
        return new RxConfigManager(MasterCacheId.create(systemKey));
    }

    public CacheHandle getCacheHandle() {
        return cacheHandle;
    }

    public RxConfig getDefaultRxConfig() {
        return defaultRxConfig;
    }

    public RxConfig getRxConfigFor(Object key) {
        return cache().read(key).orElse(getDefaultRxConfig());
    }

    public RxConfigManager setDefaultRxConfig(RxConfig rxConfig) {
        defaultRxConfig = requireNonNull(rxConfig);
        return this;
    }

    public Optional<RxConfig> setRxConfig(Object key, RxConfig rxConfig) {
        return cache().write(key, rxConfig);
    }

    public RxCache<Object, RxConfig> cache() {
        return RxCacheAccess.cache(cacheHandle, CachePolicy.unlimited(Lifetime.forever()));
    }
}
