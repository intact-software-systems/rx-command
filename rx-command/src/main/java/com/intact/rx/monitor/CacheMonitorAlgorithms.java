package com.intact.rx.monitor;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.factory.CacheFactory;

import static com.intact.rx.core.cache.strategy.CachePolicyChecker.isInactive;

final class CacheMonitorAlgorithms {
    private static final Logger log = LoggerFactory.getLogger(CacheMonitorAlgorithms.class);

    static void monitorAllCaches() {
        RxCacheAccess.getFactories().forEach((domainCacheId, cacheFactory) -> monitorCacheFactory(cacheFactory));
    }

    private static void monitorCacheFactory(CacheFactory cacheFactory) {
        for (MasterCacheId masterCacheId : cacheFactory.getCacheMasters().keySet()) {
            final CacheMaster cacheMaster = cacheFactory.getCacheMasters().get(masterCacheId);

            if (cacheMaster == null) {
                // It is allowed, but should only happen as a rare race condition
                log.warn("Master cache is null {}", masterCacheId);
                continue;
            }

            for (DataCache<?, ?> dataCache : cacheMaster.state().getDataCacheMap().values()) {
                if (dataCache.isExpired()) {
                    cacheMaster.removeDataCache(dataCache.getCacheId().getDataCacheId());
                } else {
                    dataCache.config().getCleanupStrategy().perform(dataCache.config(), dataCache);

                    boolean removed = cacheMaster.removeIf(dataCache, () -> dataCache.isEmpty() && isInactive(dataCache.getAccessStatus(), RxDefault.getCacheInactiveTimeout()));
                    if (removed) {
                        log.debug("Removed empty data cache {} not modified in {} minutes", dataCache.getCacheId(), Duration.ofMillis(dataCache.getAccessStatus().getTime().getTimeSinceModified()).toMinutes());
                    }
                }
            }

            cacheMaster.config().getCacheMasterCleanupStrategy().perform(cacheMaster);
        }
    }

    private CacheMonitorAlgorithms() {
    }
}
