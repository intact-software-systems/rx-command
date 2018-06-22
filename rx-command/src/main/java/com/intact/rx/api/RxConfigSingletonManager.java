package com.intact.rx.api;

import java.util.Optional;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.core.cache.data.id.MasterCacheId;

@SuppressWarnings("WeakerAccess")
public final class RxConfigSingletonManager {
    public static final RxConfigManager instance = RxConfigManager.forSystem(MasterCacheId.create(RxConfigSingletonManager.class.getSimpleName()));

    public static CacheHandle getCacheHandle() {
        return instance.getCacheHandle();
    }

    public static RxConfig getDefaultRxConfig() {
        return instance.getDefaultRxConfig();
    }

    public static RxConfig getRxConfigFor(Object key) {
        return instance.getRxConfigFor(key);
    }

    public static void setDefaultRxConfig(RxConfig rxConfig) {
        instance.setDefaultRxConfig(rxConfig);
    }

    public static Optional<RxConfig> setRxConfig(Object key, RxConfig rxConfig) {
        return instance.setRxConfig(key, rxConfig);
    }

    public static RxCache<Object, RxConfig> cache() {
        return instance.cache();
    }

    private RxConfigSingletonManager() {
    }
}
