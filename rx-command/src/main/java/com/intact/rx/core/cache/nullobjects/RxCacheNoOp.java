package com.intact.rx.core.cache.nullobjects;

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
import com.intact.rx.policy.LoanPolicy;

public class RxCacheNoOp<K, V> implements RxCache<K, V> {

    @SuppressWarnings("rawtypes")
    public static final RxCache instance = new RxCacheNoOp<>();

    @Override
    public RxValueAccess<V> access(K key) {
        //noinspection unchecked
        return ValueAccessNoOp.instance;
    }

    @Override
    public ValueEditor<V> edit(K key) {
        //noinspection unchecked
        return ValueEditorNoOp.instance;
    }

    @Override
    public Editor<K, V> edit() {
        //noinspection unchecked
        return EditorNoOp.instance;
    }

    @Override
    public <T> RxSelection<K, T> computeTransformation(Transformation<T, V> transformer) {
        //noinspection unchecked
        return SelectionNoOp.instance;
    }

    @Override
    public <T> RxSelection<K, T> computeTransformationIfAbsent(Object transformationId, Transformation<T, V> transformer) {
        //noinspection unchecked
        return SelectionNoOp.instance;
    }

    @Override
    public Optional<RxSelection<K, V>> readTransformation(Object transformationId) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<RxSelection<K, T>> detachTransformation(Object transformationId) {
        return Optional.empty();
    }

    @Override
    public RxSelection<K, V> computeSelection(Filter<V> filter) {
        //noinspection unchecked
        return SelectionNoOp.instance;
    }

    @Override
    public RxSelection<K, V> computeSelectionIfAbsent(Object selectionId, Filter<V> filter) {
        //noinspection unchecked
        return SelectionNoOp.instance;
    }

    @Override
    public Optional<RxSelection<K, V>> readSelection(Object selectionId) {
        return Optional.empty();
    }

    @Override
    public Optional<RxSelection<K, V>> detachSelection(Object selectionId) {
        return Optional.empty();
    }

    @Override
    public Loaned<V> computeIfAbsentAndLoan(K key, Function<? super K, ? extends V> factory, LoanPolicy loanPolicy) {
        //noinspection unchecked
        return LoanedValueNoOp.instance;
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
    public Set<K> keySet() {
        return Collections.emptySet();
    }

    @Override
    public Set<K> keySetExpired() {
        return Collections.emptySet();
    }

    @Override
    public List<V> readAsList() {
        return Collections.emptyList();
    }

    @Override
    public Loaned<V> loan(K key, LoanPolicy loanPolicy) {
        //noinspection unchecked
        return LoanedValueNoOp.instance;
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
    public CacheHandle getCacheHandle() {
        return CacheHandle.uuid();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
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
    public Optional<V> write(K key, V value) {
        return Optional.empty();
    }

    @Override
    public Optional<V> writeAndGet(K key, Supplier<V> factory) {
        return Optional.empty();
    }

    @Override
    public Map<? extends K, ? extends V> writeAll(Map<? extends K, ? extends V> values) {
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
    public Optional<V> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return Optional.empty();
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
    public void clear() {

    }

    @Override
    public void addCacheObserver(DataCacheObserver dataCacheObserver) {

    }

    @Override
    public void removeCacheObserver(DataCacheObserver dataCacheObserver) {

    }

    @Override
    public void addMementoObserver(MementoObserver<Map.Entry<K, V>> mementoObserver) {

    }

    @Override
    public void removeMementoObserver(MementoObserver<Map.Entry<K, V>> mementoObserver) {

    }

    @Override
    public void addObjectObserver(ObjectObserver<K, V> observer) {

    }

    @Override
    public void removeObjectObserver(ObjectObserver<K, V> observer) {

    }

    @Override
    public RxCache<K, V> onObjectCreatedDo(VoidStrategy2<K, V> strategy) {
        return this;
    }

    @Override
    public RxCache<K, V> onObjectModifiedDo(VoidStrategy2<K, V> strategy) {
        return this;
    }

    @Override
    public RxCache<K, V> onObjectRemovedDo(VoidStrategy2<K, V> strategy) {
        return this;
    }

    @Override
    public RxCache<K, V> onUndoDo(VoidStrategy1<Map.Entry<K, V>> strategy) {
        return this;
    }

    @Override
    public RxCache<K, V> onRedoDo(VoidStrategy1<Map.Entry<K, V>> strategy) {
        return this;
    }

    @Override
    public void disconnectAll() {

    }

    @Override
    public void addObjectTypeObserver(ObjectTypeObserver<V> observer) {

    }

    @Override
    public void removeObjectTypeObserver(ObjectTypeObserver<V> observer) {

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
    public RxCache<K, V> clearRedo(K key) {
        return this;
    }

    @Override
    public RxCache<K, V> clearUndo(K key) {
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
    public RxCache<K, V> clearRedo() {
        return this;
    }

    @Override
    public RxCache<K, V> clearUndo() {
        return this;
    }
}
