package com.intact.rx.core.cache.data;

import java.util.Map.Entry;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.CacheMasterState;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.data.id.Typename;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.core.cache.strategy.CachePolicyChecker;
import com.intact.rx.core.cache.subject.DataCacheSubject;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.Pair;
import com.intact.rx.templates.api.Context;

@SuppressWarnings("PublicMethodNotExposedInInterface")
public class CacheMaster
        extends DataCacheSubject
        implements Context<CacheMasterPolicy, CacheMasterState> {
    private static final Logger log = LoggerFactory.getLogger(CacheMaster.class);

    private final ContextObject<CacheMasterPolicy, CacheMasterState> context;

    public CacheMaster(DomainCacheId domainCacheId, final MasterCacheId masterCacheId, final CacheMasterPolicy policy) {
        this.context = new ContextObject<>(policy, new CacheMasterState(domainCacheId, masterCacheId));
    }

    public DomainCacheId getDomainCacheId() {
        return state().getDomainCacheId();
    }

    public MasterCacheId getMasterCacheId() {
        return state().getMasterCacheId();
    }

    public AccessStatus getAccessStatus() {
        return state().getAccessStatus();
    }

    // -----------------------------------------------------
    // find caches
    // -----------------------------------------------------

    public <K, V> DataCache<K, V> findCache(DataCacheId dataCacheId) {
        return findCachePrivate(dataCacheId);
    }

    public <K, V> DataCache<K, V> findCacheByName(Typename typename) {
        return findCachePrivate(typename);
    }

    public <K, V> DataCache<K, V> findCacheByType(Class<V> clazz) {
        return findCachePrivate(Typename.create(clazz));
    }

    public <K, V> DataCache<K, V> findCacheById(String id) {
        return findCachePrivate(Typename.create(id));
    }

    // -----------------------------------------------------
    // Add and remove data-cache and object-cache
    // -----------------------------------------------------

    /**
     * Note: if CacheMaster is expired then return expired DataCache. The DataCache itself will prevent access to data.
     */
    public <K, V> DataCache<K, V> getOrCreateDataCache(DataCacheId dataCacheId, DataCachePolicy dataCachePolicy) {
        requireNonNull(dataCacheId);
        requireNonNull(dataCachePolicy);

        DataCache<K, V> dataCache;
        boolean isCreated = false;

        //noinspection SynchronizeOnThis
        synchronized (this) {
            dataCache = findCachePrivate(dataCacheId);
            if (dataCache == null) {
                if (!state().getObjectCaches().containsKey(dataCacheId)) {
                    state().addObjectCache(new ObjectCache<>(dataCacheId, dataCachePolicy));
                }

                dataCache = new DataCache<>(this, CacheHandle.create(state().getDomainCacheId(), dataCacheId), dataCachePolicy);
                state().addDataCache(dataCache);
                isCreated = true;
            }
        }

        if (state().getAccessStatus().isExpired()) {
            dataCache.setExpired();
            log.debug("Expired CacheMaster {}. Created expired DataCache {}", state().getMasterCacheId(), dataCacheId);
        } else if (isCreated) {
            processOnWrite();
            onCreatedCache(CacheHandle.create(state().getDomainCacheId(), dataCacheId));
        } else {
            processOnRead();
        }

        return requireNonNull(dataCache);
    }

    /**
     * Note: if CacheMaster is expired then return expired ObjectCache. The ObjectCache itself will prevent access to expired data.
     */
    @SuppressWarnings("SynchronizedMethod")
    synchronized <K, V> ObjectCache<K, V> getOrCreateObjectCache(CacheHandle cacheHandle, DataCachePolicy policy) {
        if (state().getAccessStatus().isExpired()) {
            //noinspection AccessToStaticFieldLockedOnInstance
            log.debug("Creating expired object cache. Handling quietly");
            return new ObjectCacheNoAccess<>(cacheHandle.getDataCacheId(), policy);
        }
        if (!state().getObjectCaches().containsKey(cacheHandle.getDataCacheId())) {
            state().addObjectCache(new ObjectCache<>(cacheHandle.getDataCacheId(), policy));
        }
        //noinspection unchecked
        return (ObjectCache<K, V>) state().getObjectCaches().get(cacheHandle.getDataCacheId());
    }

    public <K, V> boolean removeIf(DataCache<K, V> dataCache, Strategy0<Boolean> condition) {
        requireNonNull(dataCache);
        requireNonNull(condition);

        return removeIfPrivate(dataCache.getCacheId().getDataCacheId(), condition).first().isPresent();
    }

    public boolean removeDataCache(DataCacheId dataCacheId) {
        requireNonNull(dataCacheId);

        @SuppressWarnings("rawtypes") Pair<DataCache, ObjectCache> tuple = removeIfPrivate(dataCacheId, () -> true);

        tuple.second().ifPresent(ObjectCache::clear);

        if (tuple.first().isPresent()) {
            state().getAccessStatus().modified();
        } else {
            state().getAccessStatus().notModified();
        }
        return tuple.first().map(DataCache::setExpired).orElse(false);
    }

    public void clearAll() {
        state().getDataCacheMap().forEach((dataCacheId, dataCache) -> removeDataCache(dataCacheId));
    }

    /**
     * Return false if cache master already set to expired, i.e., setExpired had no effect.
     */
    public boolean setExpired() {
        //noinspection SynchronizeOnThis
        synchronized (this) {
            if (state().getAccessStatus().isExpired()) {
                return false;
            }
            state().getAccessStatus().expired();
        }

        for (Entry<DataCacheId, DataCache<?, ?>> entry : state().getDataCacheMap().entrySet()) {
            entry.getValue().setExpired();
        }

        return true;
    }

    /**
     * Clears all attached data caches.
     */
    public boolean clear() {
        state().getDataCacheMap().forEach((dataCacheId, dataCache) -> dataCache.clear());
        return true;
    }

    public boolean isExceededLifetime() {
        return !CachePolicyChecker.isInLifetime(state().getAccessStatus(), config().getLifetime());
    }

    public boolean isEmpty() {
        return state().isEmpty();
    }

    // -----------------------------------------------------------
    // Interface ContextObject
    // -----------------------------------------------------------

    @Override
    public CacheMasterPolicy config() {
        return this.context.config();
    }

    @Override
    public CacheMasterState state() {
        return this.context.state();
    }

    // -----------------------------------------------------------
    // Private functions
    // -----------------------------------------------------------

    @SuppressWarnings({"SynchronizedMethod", "rawtypes"})
    private synchronized Pair<DataCache, ObjectCache> removeIfPrivate(DataCacheId dataCacheId, Strategy0<Boolean> condition) {
        return condition.perform()
                ? Pair.create(state().getDataCacheMap().remove(dataCacheId), state().getObjectCaches().remove(dataCacheId))
                : Pair.empty();
    }


    @SuppressWarnings("unchecked")
    private <K, V> DataCache<K, V> findCachePrivate(DataCacheId dataCacheId) {
        return (DataCache<K, V>) state().getDataCacheMap().get(dataCacheId);
    }

    @SuppressWarnings("unchecked")
    private <K, V> DataCache<K, V> findCachePrivate(final Typename typename) {
        return state().getDataCacheMap().entrySet().stream()
                .filter(entry -> Objects.equals(entry.getKey().getId(), typename))
                .findFirst()
                .map(entry -> (DataCache<K, V>) entry.getValue())
                .orElse(null);
    }

    private void processOnRead() {
        if (config().getExtension().isRenewOnRead() || config().getExtension().isRenewOnAccess()) {
            state().getAccessStatus().renewLoan();
        }
    }

    private void processOnWrite() {
        if (config().getExtension().isRenewOnWrite() || config().getExtension().isRenewOnAccess()) {
            state().getAccessStatus().renewLoan();
        }
    }
}
