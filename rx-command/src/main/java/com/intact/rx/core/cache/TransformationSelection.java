package com.intact.rx.core.cache;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxFilter;
import com.intact.rx.api.cache.RxSelection;
import com.intact.rx.api.cache.Transformation;
import com.intact.rx.api.cache.observer.SelectionObserver;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.subject.SelectionSubject;

public class TransformationSelection<K, V, T> implements RxSelection<K, V>, RxFilter<K, T> {
    private static final Logger log = LoggerFactory.getLogger(CacheSelection.class);

    private final CacheMaster cacheMaster = new CacheMaster(RxDefault.getDefaultRxCommandDomainCacheId(), MasterCacheId.uuid(), CacheMasterPolicy.validForever());
    private final DataCache<K, V> localCache = new DataCache<>(cacheMaster, CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), DataCacheId.empty()), DataCachePolicy.unlimitedForever());
    private final SelectionSubject<V> selectionSubject = new SelectionSubject<>();

    private final Object transformationId;
    private final Transformation<V, T> transformer;
    private final WeakReference<DataCache<K, T>> attachedToCache;

    public TransformationSelection(Object transformationId, Transformation<V, T> transformer, DataCache<K, T> attachedToCache) {
        this.transformationId = requireNonNull(transformationId);
        this.transformer = requireNonNull(transformer);
        this.attachedToCache = new WeakReference<>(requireNonNull(attachedToCache));
    }

    public SelectionSubject<V> getSubject() {
        return selectionSubject;
    }

    // ----------------------------------------
    // Interface RxSelection
    // ----------------------------------------

    @Override
    public Object getId() {
        return transformationId;
    }

    @Override
    public CacheHandle getCacheId() {
        return localCache.getCacheId();
    }

    @Override
    public boolean filter(K key, T newValue, boolean expired) {
        if (key == null || newValue == null) {
            log.warn("Filtering of illegal null key {} or value {}. Ignoring.", key, newValue);
            return false;
        }

        if (!expired) {
            boolean alreadyMember = localCache.containsKey(key);
            Optional<V> addToSelection = transformer.apply(newValue, alreadyMember);

            addToSelection.ifPresent(v -> {
                Optional<V> previous = localCache.write(key, v);

                // -----------------------------------------------
                // Notify listeners accordingly
                // -----------------------------------------------
                // Is incoming object new to this selection?
                if (!alreadyMember) {
                    selectionSubject.onObjectIn(v);
                }
                // Has object changed?
                else if (previous.isPresent() && !Objects.equals(newValue, previous.get())) {
                    selectionSubject.onObjectModified(v);
                }
                // else object has not changed
            });

            return addToSelection.isPresent();
        }
        // remove from selection
        else {
            localCache.take(key).ifPresent(selectionSubject::onObjectOut);
            return false;
        }
    }

    @Override
    public boolean detach() {
        return Optional.ofNullable(attachedToCache.get()).map(dataCache -> dataCache.takeTransformation(transformationId).isPresent()).orElse(false);
    }

    @Override
    public boolean isAttached() {
        return Optional.ofNullable(attachedToCache.get()).map(dataCache -> dataCache.isTransformationAttached(transformationId)).orElse(false);
    }

    @Override
    public TransformationSelection<K, V, T> onObjectInDo(VoidStrategy1<V> strategy) {
        selectionSubject.onObjectInDo(strategy);
        return this;
    }

    @Override
    public TransformationSelection<K, V, T> onObjectOutDo(VoidStrategy1<V> strategy) {
        selectionSubject.onObjectOutDo(strategy);
        return this;
    }

    @Override
    public TransformationSelection<K, V, T> onObjectModifiedDo(VoidStrategy1<V> strategy) {
        selectionSubject.onObjectModifiedDo(strategy);
        return this;
    }

    @Override
    public TransformationSelection<K, V, T> onDetachDo(VoidStrategy0 strategy) {
        selectionSubject.onDetachDo(strategy);
        return this;
    }

    @Override
    public boolean connect(SelectionObserver<V> observer) {
        return selectionSubject.connect(observer);
    }

    @Override
    public boolean disconnect(SelectionObserver<V> observer) {
        return selectionSubject.disconnect(observer);
    }

    @Override
    public void disconnectAll() {
        selectionSubject.disconnectAll();
    }

    // ----------------------------------------
    // Interface Writer
    // ----------------------------------------

    @Override
    public Optional<V> write(K key, V value) {
        return localCache.write(key, value);
    }

    @Override
    public Optional<V> writeAndGet(K key, Supplier<V> factory) {
        return localCache.writeAndGet(key, factory);
    }

    @Override
    public Map<? extends K, ? extends V> writeAll(Map<? extends K, ? extends V> values) {
        return localCache.writeAll(values);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> factory) {
        return localCache.computeIfAbsent(key, factory);
    }

    @Override
    public Optional<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return localCache.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Optional<V> compute(K key, BiFunction<? super K, Optional<? super V>, ? extends V> remappingFunction) {
        return localCache.compute(key, remappingFunction);
    }

    @Override
    public boolean compareAndWrite(K key, Supplier<V> expect, Supplier<V> update) {
        return localCache.compareAndWrite(key, expect, update);
    }

    @Override
    public Optional<V> replace(K key, Supplier<V> update) {
        return localCache.replace(key, update);
    }

    @Override
    public Optional<V> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return localCache.merge(key, value, remappingFunction);
    }

    @Override
    public Optional<V> take(K key) {
        return localCache.take(key);
    }

    @Override
    public Map<K, V> take(Iterable<? extends K> keys) {
        return localCache.take(keys);
    }

    @Override
    public Map<K, V> takeAll() {
        return localCache.takeAll();
    }

    @Override
    public Map<K, V> takeExpired() {
        return localCache.takeExpired();
    }

    @Override
    public void clear() {
        localCache.clear();
    }

    // ----------------------------------------
    // Interface Reader
    // ----------------------------------------

    @Override
    public boolean containsKey(K key) {
        return localCache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return localCache.containsValue(value);
    }

    @Override
    public Optional<V> read(K key) {
        return localCache.read(key);
    }

    @Override
    public Map<K, V> read(Iterable<? extends K> keys) {
        return localCache.read(keys);
    }

    @Override
    public Map<K, V> readAll() {
        return localCache.readAll();
    }

    @Override
    public Map<K, V> readExpired() {
        return localCache.readExpired();
    }

    @Override
    public Set<K> keySet() {
        return localCache.keySet();
    }

    @Override
    public Set<K> keySetExpired() {
        return localCache.keySetExpired();
    }

    @Override
    public List<V> readAsList() {
        return localCache.readAsList();
    }

    @Override
    public int size() {
        return localCache.size();
    }

    @Override
    public boolean isEmpty() {
        return localCache.isEmpty();
    }

    @Override
    public boolean isExpired(K key) {
        return Optional.ofNullable(attachedToCache.get()).map(cache -> cache.isExpired(key)).orElse(true);
    }

    @Override
    public boolean isExpired() {
        return Optional.ofNullable(attachedToCache.get()).map(DataCache::isExpired).orElse(true);
    }

    // ----------------------------------------
    // Interface AutoClosable
    // ----------------------------------------

    @Override
    public void close() {
        detach();
        disconnectAll();
    }
}

