package com.intact.rx.core.machine.factory;

import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.core.cache.CacheReaderWriter;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.machine.RxThreadPoolId;
import com.intact.rx.core.machine.context.RxThreadPoolConfig;

public final class RxThreadPoolFactory {
    private static final DataCacheId commandControllerCacheId = DataCacheId.create(RxThreadPool.class, MasterCacheId.create(RxThreadPoolFactory.class));
    private static final CachePolicy cachePolicy = CachePolicy.create(CacheMasterPolicy.validForever(), DataCachePolicy.unlimitedForever());

    public static RxThreadPool computeIfAbsent(RxThreadPoolConfig poolPolicy) {
        return poolcache().computeIfAbsent(poolPolicy.getThreadPoolId(), id -> RxThreadPool.create(poolPolicy));
    }

    public static Optional<RxThreadPool> findThreadPool(RxThreadPoolId id) {
        return poolcache().read(id);
    }

    public static void shutdown(RxThreadPoolId rxThreadPoolId) {
        poolcache().take(rxThreadPoolId).ifPresent(ScheduledThreadPoolExecutor::shutdown);
    }

    public static RxCache<RxThreadPoolId, RxThreadPool> poolcache() {
        return new CacheReaderWriter<>(() -> RxCacheAccess.defaultRxCacheFactory().computeDataCacheIfAbsent(commandControllerCacheId, cachePolicy));
    }

    public static RxThreadPool defaultCommandPool() {
        return computeIfAbsent(RxDefault.getDefaultCommandPoolConfig());
    }

    public static RxThreadPool controllerPool() {
        return computeIfAbsent(RxDefault.getControllerPoolPolicy());
    }

    public static RxThreadPool monitorPool() {
        return computeIfAbsent(RxDefault.getMonitorPoolPolicy());
    }

    private RxThreadPoolFactory() {
    }
}
