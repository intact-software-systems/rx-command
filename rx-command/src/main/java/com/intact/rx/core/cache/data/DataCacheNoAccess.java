package com.intact.rx.core.cache.data;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.intact.rx.api.cache.*;
import com.intact.rx.api.cache.observer.DataCacheObserver;
import com.intact.rx.api.cache.observer.MementoObserver;
import com.intact.rx.api.cache.observer.ObjectObserver;
import com.intact.rx.api.cache.observer.ObjectTypeObserver;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.command.VoidStrategy2;
import com.intact.rx.core.cache.nullobjects.SelectionNoOp;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.policy.LoanPolicy;

public class DataCacheNoAccess<K, V> extends DataCache<K, V> {
    public DataCacheNoAccess(DataCache<K, V> dataCache) {
        super(dataCache);
    }

    @Override
    public Optional<RxSelection<K, V>> readSelection(Object selectionId) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<RxSelection<K, T>> readTransformation(Object transformationId) {
        return Optional.empty();
    }

    @Override
    public void addMementoObserver(MementoObserver<Map.Entry<K, V>> observer) {
    }

    @Override
    public boolean compareAndWrite(K key, V expect, V update) {
        return false;
    }

    @Override
    public Optional<V> replace(K key, V update) {
        return Optional.empty();
    }

    @Override
    public DataCacheNoAccess<K, V> onUndoDo(VoidStrategy1<Map.Entry<K, V>> strategy) {
        return this;
    }

    @Override
    public DataCacheNoAccess<K, V> onRedoDo(VoidStrategy1<Map.Entry<K, V>> strategy) {
        return this;
    }

    @Override
    public List<V> undoStack(K key) {
        return Collections.emptyList();
    }

    @Override
    public List<V> redoStack(K key) {
        return Collections.emptyList();
    }

    @Override
    public Optional<V> undo(K key) {
        return Optional.empty();
    }

    @Override
    public Optional<V> redo(K key) {
        return Optional.empty();
    }

    @Override
    public DataCacheNoAccess<K, V> clearRedo(K key) {
        return this;
    }

    @Override
    public DataCacheNoAccess<K, V> clearUndo(K key) {
        return this;
    }

    @Override
    public Optional<Map.Entry<K, V>> undo() {
        return Optional.empty();
    }

    @Override
    public Optional<Map.Entry<K, V>> redo() {
        return Optional.empty();
    }

    @Override
    public List<Map.Entry<K, V>> undoStack() {
        return Collections.emptyList();
    }

    @Override
    public List<Map.Entry<K, V>> redoStack() {
        return Collections.emptyList();
    }

    @Override
    public DataCacheNoAccess<K, V> clearRedo() {
        return this;
    }

    @Override
    public DataCacheNoAccess<K, V> clearUndo() {
        return this;
    }

    @Override
    public Set<K> keySet() {
        return Collections.emptySet();
    }

    @Override
    public Set<K> keySetExpired() {
        return Collections.emptySet();
    }

    @Override
    public DataCacheNoAccess<K, V> onObjectCreatedDo(VoidStrategy2<K, V> strategy) {
        return this;
    }

    @Override
    public DataCacheNoAccess<K, V> onObjectModifiedDo(VoidStrategy2<K, V> strategy) {
        return this;
    }

    @Override
    public DataCacheNoAccess<K, V> onObjectRemovedDo(VoidStrategy2<K, V> strategy) {
        return this;
    }

    @Override
    public RxSelection<K, V> computeSelectionIfAbsent(Object selectionId, Filter<V> filter) {
        //noinspection unchecked
        return SelectionNoOp.instance;
    }

    @Override
    public Optional<RxSelection<K, V>> takeSelection(Object selectionId) {
        return Optional.empty();
    }

    @Override
    public <T> RxSelection<K, T> computeTransformationIfAbsent(Object selectionId, Transformation<T, V> transformer) {
        //noinspection unchecked
        return SelectionNoOp.instance;
    }

    @Override
    public <T> Optional<RxSelection<K, T>> takeTransformation(Object selectionId) {
        return Optional.empty();
    }

    @Override
    public boolean isSelectionAttached(Object id) {
        return false;
    }

    @Override
    public Optional<V> write(K key, V value) {
        return Optional.empty();
    }

    @Override
    public Optional<V> writeAndGet(K key, Supplier<V> factory) {
        return Optional.empty();
    }

    @Override
    public Map<K, V> writeAll(Map<? extends K, ? extends V> values) {
        return Collections.emptyMap();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> factory) {
        return factory.apply(key);
    }

    @Override
    public Optional<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Optional.empty();
    }

    @Override
    public Optional<V> compute(K key, BiFunction<? super K, Optional<? super V>, ? extends V> remappingFunction) {
        return Optional.empty();
    }

    @Override
    public boolean compareAndWrite(K key, Supplier<V> expect, Supplier<V> update) {
        return false;
    }

    @Override
    public Optional<V> replace(K key, Supplier<V> update) {
        return Optional.empty();
    }

    @Override
    public boolean isTransformationAttached(Object transformationId) {
        return false;
    }

    @Override
    public Optional<V> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return Optional.empty();
    }

    @Override
    public Optional<V> computeIfAbsentAndLoan(K key, Function<? super K, ? extends V> factory, LoanPolicy loanPolicy) {
        return Optional.ofNullable(factory.apply(key));
    }

    @Override
    public Optional<V> take(K key) {
        return Optional.empty();
    }

    @Override
    public Map<K, V> take(Iterable<? extends K> keys) {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, V> takeAll() {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, V> takeExpired() {
        return Collections.emptyMap();
    }

    @Override
    public boolean containsKey(K key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Optional<V> read(K key) {
        return Optional.empty();
    }

    @Override
    public Optional<V> loan(K key, LoanPolicy loanPolicy) {
        return Optional.empty();
    }

    @Override
    public Optional<V> returnLoan(Loaned<V> loan) {
        return Optional.empty();
    }

    @Override
    public boolean isLoaned(K key) {
        return false;
    }

    @Override
    public Map<K, V> read(Iterable<? extends K> keys) {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, V> readAll() {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, V> readExpired() {
        return Collections.emptyMap();
    }

    @Override
    public List<V> readAsList() {
        return Collections.emptyList();
    }

    @Override
    public int size() {
        return 0;
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    public void clear() {
        // NB! Access to invalidate cache is kept
        super.clear();
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    public CacheHandle getCacheId() {
        return super.getCacheId();
    }

    @Override
    public boolean isExpired(K key) {
        return true;
    }

    @Override
    public boolean isExpired() {
        return true;
    }

    @Override
    public void addObjectObserver(ObjectObserver<K, V> observer) {
    }

    @Override
    public void addObjectTypeObserver(ObjectTypeObserver<V> observer) {
    }

    @Override
    public void addCacheObserver(DataCacheObserver observer) {
    }

    @Override
    public Set<K> findKeysWithAccessStates(AccessStatus.AccessState... accessStates) {
        return Collections.emptySet();
    }

    @Override
    public List<ObjectRoot<K, V>> getRoots() {
        return Collections.emptyList();
    }

    @Override
    public boolean setExpired() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public AccessStatus getAccessStatus() {
        return new AccessStatus();
    }
}
