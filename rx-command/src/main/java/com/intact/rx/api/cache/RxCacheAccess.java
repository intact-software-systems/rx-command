package com.intact.rx.api.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.factory.CacheFactory;
import com.intact.rx.templates.Pair;

import static com.intact.rx.api.RxDefault.getDefaultDomainCacheId;
import static com.intact.rx.api.RxDefault.getDefaultRxCommandDomainCacheId;

/**
 * Single point of access to CacheFactory instances based on DomainCacheId
 */
public final class RxCacheAccess {
    private static final Map<DomainCacheId, CacheFactory> factories = new ConcurrentHashMap<>();

    public static Map<DomainCacheId, CacheFactory> getFactories() {
        //noinspection ReturnOfCollectionOrArrayField
        return factories;
    }

    public static CacheFactory computeIfAbsent(DomainCacheId domainCacheId) {
        requireNonNull(domainCacheId);

        return factories.computeIfAbsent(domainCacheId, id -> new CacheFactory(domainCacheId));
    }

    public static Optional<CacheFactory> find(DomainCacheId domainCacheId) {
        requireNonNull(domainCacheId);

        return Optional.ofNullable(factories.get(domainCacheId));
    }

    public static <K, V> Optional<RxCache<K, V>> find(CacheHandle cacheHandle) {
        requireNonNull(cacheHandle);

        return find(cacheHandle.getDomainCacheId())
                .map(cacheFactory -> {
                    Pair<CacheMaster, DataCache<K, V>> result = cacheFactory.findCache(cacheHandle);

                    RxCache<K, V> cache = null;
                    if (result.first().isPresent() && result.second().isPresent()) {
                        cache = cacheFactory.computeCacheIfAbsent(cacheHandle.getMasterCacheId(), cacheHandle.getDataCacheId(), CachePolicy.create(result.first().get().config(), result.second().get().config()));
                    }
                    return cache;
                });
    }

    public static Optional<CacheFactory> clearAndRemove(DomainCacheId domainCacheId) {
        requireNonNull(domainCacheId);

        Optional.ofNullable(factories.get(domainCacheId)).ifPresent(CacheFactory::clearAllCaches);
        return Optional.ofNullable(factories.remove(domainCacheId));
    }

    public static <K, V> RxCache<K, V> cacheUUID(CachePolicy cachePolicy) {
        requireNonNull(cachePolicy);

        return cache(CacheHandle.uuid(), cachePolicy);
    }

    public static <K, V> RxCache<K, V> cache(CacheHandle cacheHandle, CachePolicy cachePolicy) {
        requireNonNull(cacheHandle);
        requireNonNull(cachePolicy);

        return computeIfAbsent(cacheHandle.getDomainCacheId())
                .computeCacheIfAbsent(
                        cacheHandle.getMasterCacheId(),
                        cacheHandle.getDataCacheId(),
                        cachePolicy
                );
    }

    public static <T> RxSet<T> set(CacheHandle cacheHandle, CachePolicy cachePolicy) {
        requireNonNull(cacheHandle);
        requireNonNull(cachePolicy);

        return computeIfAbsent(cacheHandle.getDomainCacheId())
                .computeSetIfAbsent(
                        cacheHandle.getMasterCacheId(),
                        cacheHandle.getDataCacheId(),
                        cachePolicy
                );
    }

    public static void clearCache(DomainCacheId domainCacheId, MasterCacheId masterCacheId) {
        requireNonNull(domainCacheId);
        requireNonNull(masterCacheId);

        find(domainCacheId).ifPresent(cacheFactory -> cacheFactory.clear(masterCacheId));
    }

    public static void clear(CacheHandle cacheHandle) {
        requireNonNull(cacheHandle);

        find(cacheHandle.getDomainCacheId()).ifPresent(cacheFactory -> cacheFactory.findCache(cacheHandle).first().ifPresent(CacheMaster::clearAll));
    }

    public static void expireDataCache(CacheHandle cacheHandle) {
        requireNonNull(cacheHandle);

        find(cacheHandle.getDomainCacheId()).ifPresent(cacheFactory -> cacheFactory.expire(cacheHandle.getDataCacheId()));
    }

    public static CacheFactory defaultCacheFactory() {
        return computeIfAbsent(getDefaultDomainCacheId());
    }

    public static CacheFactory defaultRxCacheFactory() {
        return computeIfAbsent(getDefaultRxCommandDomainCacheId());
    }

    private RxCacheAccess() {
    }
}
