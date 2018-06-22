package com.intact.rx.core.cache;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.Editor;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.observer.ObjectObserver;
import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;

public class CacheEditor<K, V> implements Editor<K, V>, ObjectObserver<K, V> {
    private final RxCache<K, V> editCache;
    private final RxCache<K, V> cache;

    private final Collection<K> contractedKeys = new HashSet<>();
    private final AtomicBoolean contractAll = new AtomicBoolean(false);

    private final CacheMaster cacheMaster = new CacheMaster(RxDefault.getDefaultRxCommandDomainCacheId(), MasterCacheId.uuid(), CacheMasterPolicy.validForever());
    private final DataCache<K, V> kvDataCache = new DataCache<>(cacheMaster, CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), DataCacheId.empty()), DataCachePolicy.unlimitedForever());

    public CacheEditor(RxCache<K, V> cache) {
        this.cache = cache;
        this.editCache = new CacheReaderWriter<>(() -> kvDataCache);
        this.editCache.writeAll(cache.readAll());
    }

    @Override
    public RxCache<K, V> edit() {
        return editCache;
    }

    @Override
    public RxCache<K, V> cache() {
        return cache;
    }

    @Override
    public boolean isModified(K key) {
        return editCache.read(key)
                .map(v -> cache.read(key)
                        .map(existing -> Objects.equals(existing, v))
                        .orElse(true))
                .orElseGet(() -> cache.containsKey(key));
    }

    @Override
    public boolean isModified() {
        //noinspection SimplifiableIfStatement
        if (editCache.size() != cache.size()) {
            return true;
        }

        return !editCache.readAll()
                .entrySet().stream()
                .allMatch(kvEntry -> cache.read(kvEntry.getKey()).map(v -> Objects.equals(kvEntry.getValue(), v)).orElse(false));
    }

    @Override
    public Map<K, V> readModified() {
        return editCache.readAll()
                .entrySet().stream()
                .map(kvEntry -> cache.read(kvEntry.getKey())
                        .map(v -> {
                            if (!Objects.equals(kvEntry.getValue(), v)) {
                                return kvEntry;
                            }
                            return null;
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Optional<V> commit(K key) {
        return editCache.read(key).flatMap(v -> cache.write(key, v));
    }

    @Override
    public Map<? extends K, ? extends V> commit() {
        return cache.writeAll(editCache.readAll());
    }

    @Override
    public Editor<K, V> autoRefreshAll() {
        cache.addObjectObserver(this);
        contractAll.set(true);
        return this;
    }

    @Override
    public Editor<K, V> autoRefresh(Collection<? extends K> iterable) {
        cache.addObjectObserver(this);
        contractedKeys.addAll(iterable);
        return this;
    }

    @Override
    public Editor<K, V> autoRefreshNone() {
        cache.removeObjectObserver(this);
        contractAll.set(false);
        contractedKeys.clear();
        return this;
    }

    @Override
    public void refresh() {
        editCache.writeAll(cache.readAll());
    }

    @Override
    public void onObjectCreated(K key, V value) {
        if (isContracted(key)) {
            editCache.write(key, value);
        }
    }

    @Override
    public void onObjectRemoved(K key, V value) {
        if (isContracted(key)) {
            editCache.take(key);
        }
    }

    @Override
    public void onObjectModified(K key, V value) {
        if (isContracted(key)) {
            editCache.write(key, value);
        }
    }

    private boolean isContracted(K key) {
        return contractAll.get() || contractedKeys.contains(key);
    }
}
