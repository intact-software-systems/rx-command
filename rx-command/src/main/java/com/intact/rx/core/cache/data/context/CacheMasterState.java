package com.intact.rx.core.cache.data.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.ObjectCache;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.status.AccessStatus;

public class CacheMasterState {
    private final DomainCacheId domainCacheId;
    private final MasterCacheId masterCacheId;

    private final Map<DataCacheId, DataCache<?, ?>> dataCaches;
    private final Map<DataCacheId, ObjectCache<?, ?>> objectCaches;

    private final AccessStatus accessStatus = new AccessStatus();

    public CacheMasterState(DomainCacheId domainCacheId, MasterCacheId masterCacheId) {
        this.domainCacheId = requireNonNull(domainCacheId);
        this.masterCacheId = requireNonNull(masterCacheId);

        this.dataCaches = new ConcurrentHashMap<>();
        this.objectCaches = new ConcurrentHashMap<>();
    }

    public Map<DataCacheId, DataCache<?, ?>> getDataCacheMap() {
        accessStatus.read();
        //noinspection ReturnOfCollectionOrArrayField
        return dataCaches;
    }

    public Map<DataCacheId, ObjectCache<?, ?>> getObjectCaches() {
        //noinspection ReturnOfCollectionOrArrayField
        return objectCaches;
    }

    public <K, V> void addObjectCache(ObjectCache<K, V> objectCache) {
        objectCaches.put(objectCache.getCacheId(), objectCache);
    }

    public <K, V> void addDataCache(DataCache<K, V> dataCache) {
        accessStatus.modified();
        dataCaches.put(dataCache.getCacheId().getDataCacheId(), dataCache);
    }

    public MasterCacheId getMasterCacheId() {
        return masterCacheId;
    }

    public DomainCacheId getDomainCacheId() {
        return domainCacheId;
    }

    public AccessStatus getAccessStatus() {
        return accessStatus;
    }

    public boolean isEmpty() {
        return dataCaches.isEmpty();
    }
}
