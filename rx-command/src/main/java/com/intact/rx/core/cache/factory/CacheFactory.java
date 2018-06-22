package com.intact.rx.core.cache.factory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxSet;
import com.intact.rx.api.cache.observer.CacheMasterObserver;
import com.intact.rx.api.cache.observer.DataCacheObserver;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.core.cache.CacheReaderWriter;
import com.intact.rx.core.cache.SetReaderWriter;
import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.data.id.Typename;
import com.intact.rx.core.cache.subject.CacheMasterSubject;
import com.intact.rx.templates.Pair;

public class CacheFactory {
    private static final Logger log = LoggerFactory.getLogger(CacheFactory.class);

    private final DomainCacheId domainCacheId;
    private final CacheMasterSubject masterCacheSubject;
    private final Map<MasterCacheId, CacheMaster> cacheMasters;

    public CacheFactory(DomainCacheId domainCacheId) {
        this.domainCacheId = requireNonNull(domainCacheId);
        this.masterCacheSubject = new CacheMasterSubject();
        this.cacheMasters = new ConcurrentHashMap<>();
    }

    public DomainCacheId getDomainCacheId() {
        return domainCacheId;
    }

    // -----------------------------------------------------
    // find caches
    // -----------------------------------------------------

    public Map<MasterCacheId, CacheMaster> getCacheMasters() {
        return Collections.unmodifiableMap(cacheMasters);
    }

    public <K, V> Pair<CacheMaster, DataCache<K, V>> findCache(CacheHandle cacheHandle) {
        CacheMaster cacheMaster = cacheMasters.get(cacheHandle.getMasterCacheId());
        if (cacheMaster != null && cacheHandle.getMasterCacheId().isValid()) {
            DataCache<K, V> dataCache = cacheMaster.findCache(cacheHandle.getDataCacheId());
            return Pair.create(cacheMaster, dataCache);
        }
        return Pair.create(null, null);
    }

    public List<DataCache<?, ?>> findCaches(DataCacheId dataCacheId) {
        return cacheMasters.values().stream()
                .map(cacheMaster -> cacheMaster.findCache(dataCacheId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<DataCache<Object, Object>> findCachesByName(Typename typename) {
        return cacheMasters.values().stream()
                .map(cacheMaster -> cacheMaster.findCacheByName(typename))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public CacheMaster findCacheMaster(MasterCacheId masterCacheId) {
        return masterCacheId.isValid() ? cacheMasters.get(masterCacheId) : null;
    }

    public <K, V> Optional<DataCache<K, V>> findDataCache(MasterCacheId masterCacheId, Typename typename) {
        CacheMaster cacheMaster = cacheMasters.get(masterCacheId);
        if (cacheMaster == null || !masterCacheId.isValid()) {
            return Optional.empty();
        }
        return Optional.ofNullable(cacheMaster.findCacheByName(typename));
    }

    // -----------------------------------------------------
    // "get or create" functions
    // -----------------------------------------------------

    public <K, V> RxCache<K, V> computeCacheIfAbsent(MasterCacheId masterCacheId, DataCacheId dataCacheId, CachePolicy cachePolicy) {
        return new CacheReaderWriter<>(() -> getOrCreateDataCachePrivate(masterCacheId, dataCacheId, cachePolicy));
    }

    public <T> RxSet<T> computeSetIfAbsent(MasterCacheId masterCacheId, DataCacheId dataCacheId, CachePolicy cachePolicy) {
        return new SetReaderWriter<>(() -> getOrCreateDataCachePrivate(masterCacheId, dataCacheId, cachePolicy));
    }

    public <K, V> DataCache<K, V> computeDataCacheIfAbsent(MasterCacheId masterCacheId, DataCacheId dataCacheId, CachePolicy cachePolicy) {
        return getOrCreateCacheMasterPrivate(masterCacheId, cachePolicy.getCacheMasterPolicy())
                .getOrCreateDataCache(dataCacheId, cachePolicy.getDataCachePolicy());
    }

    public <K, V> DataCache<K, V> computeDataCacheIfAbsent(DataCacheId dataCacheId, CachePolicy cachePolicy) {
        return getOrCreateCacheMasterPrivate(dataCacheId.getOwner(), cachePolicy.getCacheMasterPolicy())
                .getOrCreateDataCache(dataCacheId, cachePolicy.getDataCachePolicy());
    }

    public <K, V> DataCache<K, V> computeDataCacheIfAbsent(CacheHandle cacheHandle, CachePolicy cachePolicy) {
        return getOrCreateCacheMasterPrivate(cacheHandle.getMasterCacheId(), cachePolicy.getCacheMasterPolicy())
                .getOrCreateDataCache(cacheHandle.getDataCacheId(), cachePolicy.getDataCachePolicy());
    }

    public <V> void expire(MasterCacheId masterCacheId, Class<V> cachedType) {
        expire(DataCacheId.create(cachedType, masterCacheId));
    }

    public void expire(DataCacheId dataCacheId) {
        CacheMaster cacheMaster = cacheMasters.get(dataCacheId.getOwner());
        if (cacheMaster != null) {
            DataCache<?, ?> dataCache = cacheMaster.state().getDataCacheMap().get(dataCacheId);
            if (dataCache != null) {
                dataCache.setExpired();
            }
        }
    }

    public void expire(MasterCacheId masterCacheId) {
        if (cacheMasters.containsKey(masterCacheId)) {
            CacheMaster cacheMaster = cacheMasters.get(masterCacheId);
            if (cacheMaster != null) {
                cacheMaster.setExpired();
            }
        }
    }

    public boolean expireIf(CacheMaster cacheMaster, Strategy0<Boolean> condition) {
        requireNonNull(cacheMaster);
        requireNonNull(condition);

        if (condition.perform()) {
            cacheMaster.setExpired();
            return removeCacheMasterPrivate(cacheMaster.getMasterCacheId()) != null;
        }
        return false;
    }

    public void clear(MasterCacheId masterCacheId) {
        if (cacheMasters.containsKey(masterCacheId)) {
            CacheMaster cacheMaster = cacheMasters.get(masterCacheId);
            if (cacheMaster != null) {
                cacheMaster.clear();
                log.info("Cleared master cache: {}", masterCacheId);
            }
        }
    }

    public CacheMaster removeCacheMaster(MasterCacheId cacheMasterId) {
        return removeCacheMasterPrivate(cacheMasterId);
    }

    public void clearAllCaches() {
        cacheMasters.forEach((key, value) -> value.clearAll());
    }

    // ------------------------------------------
    // Private functions
    // ------------------------------------------

    private <K, V> DataCache<K, V> getOrCreateDataCachePrivate(MasterCacheId masterCacheId, DataCacheId dataCacheId, CachePolicy cachePolicy) {
        requireNonNull(masterCacheId);
        requireNonNull(dataCacheId);
        requireNonNull(cachePolicy);

        CacheMaster master = getOrCreateCacheMasterPrivate(masterCacheId, cachePolicy.getCacheMasterPolicy());

        return master.getOrCreateDataCache(dataCacheId, cachePolicy.getDataCachePolicy());
    }

    private CacheMaster getOrCreateCacheMasterPrivate(MasterCacheId cacheMasterId, CacheMasterPolicy cacheMasterPolicy) {
        requireNonNull(cacheMasterId);
        requireNonNull(cacheMasterPolicy);

        CacheMaster existingInstance;
        CacheMaster newInstance = null;

        // -------------------------------------------------
        // NB! This critical section is mutex with remove
        // -------------------------------------------------
        synchronized (cacheMasters) {
            existingInstance = cacheMasters.get(cacheMasterId);

            if (existingInstance == null || existingInstance.isExceededLifetime()) {
                newInstance = new CacheMaster(domainCacheId, cacheMasterId, cacheMasterPolicy);
                cacheMasters.put(newInstance.getMasterCacheId(), newInstance);
            }
        }

        // -----------------------------------------------
        // Expire existing (old) master if new is created
        // -----------------------------------------------
        if (newInstance != null && existingInstance != null) {
            // Implies: existingMaster isExpired == true;
            existingInstance.setExpired();
            existingInstance.clearAll();
        }

        // -----------------------------------------------
        // Observer callbacks
        // -----------------------------------------------
        if (existingInstance != null && existingInstance.isExceededLifetime()) {
            masterCacheSubject.onRemovedCacheMaster(existingInstance);
        }
        if (newInstance != null) {
            masterCacheSubject.onCreatedCacheMaster(newInstance);
        }

        // --------------------------------------
        // always return new instance if created
        // --------------------------------------
        return newInstance != null
                ? newInstance
                : existingInstance;
    }

    private CacheMaster removeCacheMasterPrivate(MasterCacheId cacheMasterId) {
        requireNonNull(cacheMasterId);

        // -------------------------------------------------
        // NB! This critical section is mutex with getOrCreate....
        // -------------------------------------------------
        synchronized (cacheMasters) {
            return cacheMasters.remove(cacheMasterId);
        }
    }

    // ------------------------------------------
    // MasterCacheObserver connect/disconnect
    // ------------------------------------------

    public boolean connect(CacheMasterObserver observer) {
        return masterCacheSubject.connect(observer);
    }

    public boolean disconnect(CacheMasterObserver observer) {
        return masterCacheSubject.disconnect(observer);
    }

    // ------------------------------------------
    // DataCacheObserver connect/disconnect
    // ------------------------------------------

    public void connect(DataCacheObserver observer) {
        cacheMasters.forEach((masterCacheId, cacheMaster) -> cacheMaster.connect(observer));
    }

    public void disconnect(DataCacheObserver observer) {
        cacheMasters.forEach((masterCacheId, cacheMaster) -> cacheMaster.disconnect(observer));
    }

    public void disconnectAll() {
        cacheMasters.values().forEach(CacheMaster::disconnectAll);
        masterCacheSubject.disconnectAll();
    }
}
