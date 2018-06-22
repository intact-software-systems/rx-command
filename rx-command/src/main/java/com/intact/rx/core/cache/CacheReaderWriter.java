package com.intact.rx.core.cache;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.*;
import com.intact.rx.api.cache.observer.DataCacheObserver;
import com.intact.rx.api.cache.observer.MementoObserver;
import com.intact.rx.api.cache.observer.ObjectObserver;
import com.intact.rx.api.cache.observer.ObjectTypeObserver;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.command.VoidStrategy2;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.DataCacheNoAccess;
import com.intact.rx.core.cache.nullobjects.*;
import com.intact.rx.policy.LoanPolicy;

public class CacheReaderWriter<K, V> implements RxCache<K, V> {
    private final Supplier<DataCache<K, V>> computeIfAbsent;
    private final AtomicReference<DataCache<K, V>> cache;

    public CacheReaderWriter(Supplier<DataCache<K, V>> cacheSupplier) {
        this.computeIfAbsent = requireNonNull(cacheSupplier);
        this.cache = new AtomicReference<>(requireNonNull(cacheSupplier.get()));
    }

    // -----------------------------------------------------------
    // Interface RxCache
    // -----------------------------------------------------------

    @Override
    public RxValueAccess<V> access(K key) {
        return cache().isExpired()
                ? ValueAccessNoOp.instance
                : new CachedValueAccess<>(key, this);
    }

    @Override
    public ValueEditor<V> edit(K key) {
        return cache().isExpired()
                ? ValueEditorNoOp.instance
                : new CachedValueEditor<>(key, this);
    }

    @Override
    public Editor<K, V> edit() {
        return cache().isExpired()
                ? EditorNoOp.instance
                : new CacheEditor<>(this);
    }

    @Override
    public <T> RxSelection<K, T> computeTransformation(Transformation<T, V> transformer) {
        return cache().isExpired()
                ? SelectionNoOp.instance
                : cache().computeTransformationIfAbsent(UUID.randomUUID(), transformer);
    }

    @Override
    public <T> RxSelection<K, T> computeTransformationIfAbsent(Object transformationId, Transformation<T, V> transformer) {
        return cache().isExpired()
                ? SelectionNoOp.instance
                : cache().computeTransformationIfAbsent(transformationId, transformer);
    }

    @Override
    public Optional<RxSelection<K, V>> readTransformation(Object transformationId) {
        return cache().isExpired()
                ? Optional.empty()
                : cache().readTransformation(transformationId);
    }

    @Override
    public <T> Optional<RxSelection<K, T>> detachTransformation(Object transformationId) {
        return cache().takeTransformation(transformationId);
    }

    @Override
    public RxSelection<K, V> computeSelection(Filter<V> filter) {
        return cache().isExpired()
                ? SelectionNoOp.instance
                : cache().computeSelectionIfAbsent(UUID.randomUUID(), filter);
    }

    @Override
    public RxSelection<K, V> computeSelectionIfAbsent(Object selectionId, Filter<V> filter) {
        return cache().isExpired()
                ? SelectionNoOp.instance
                : cache().computeSelectionIfAbsent(selectionId, filter);
    }

    @Override
    public Optional<RxSelection<K, V>> readSelection(Object selectionId) {
        return cache().isExpired()
                ? Optional.empty()
                : cache().readSelection(selectionId);
    }

    @Override
    public Optional<RxSelection<K, V>> detachSelection(Object selectionId) {
        return cache().takeSelection(selectionId);
    }

    @Override
    public Loaned<V> loan(K key, LoanPolicy loanPolicy) {
        return cache().isExpired()
                ?
                LoanedValueNoOp.instance
                :
                new LoanedValue<>(
                        key,
                        () -> cache().loan(key, loanPolicy),
                        (Loaned<V> loaned) -> cache().returnLoan(loaned),
                        cache().getCacheId(),
                        loanPolicy
                );
    }

    @Override
    public Loaned<V> computeIfAbsentAndLoan(K key, Function<? super K, ? extends V> factory, LoanPolicy loanPolicy) {
        DataCache<K, V> cache = cache();
        if (cache.isExpired()) {
            //noinspection unchecked
            return LoanedValueNoOp.instance;
        }

        // TODO: This is a lazy loan, i.e., computeIfAbsentAndLazyLoan
        return new LoanedValue<>(
                key,
                () -> cache().computeIfAbsentAndLoan(key, factory, loanPolicy),
                (Loaned<V> loaned) -> cache().returnLoan(loaned),
                cache.getCacheId(),
                loanPolicy
        );
    }

    @Override
    public Optional<V> returnLoan(Loaned<V> loan) {
        if (Objects.equals(loan.getCacheHandle(), cache().getCacheId())) {
            throw new IllegalStateException("Cannot return loan to wrong cache. Loan: " + loan.getCacheHandle() + ". This: " + cache().getCacheId());
        }

        return cache().returnLoan(loan);
    }

    @Override
    public boolean isLoaned(K key) {
        return cache().isLoaned(key);
    }

    @Override
    public RxCache<K, V> onObjectCreatedDo(VoidStrategy2<K, V> strategy) {
        cache().onObjectCreatedDo(strategy);
        return this;
    }

    @Override
    public RxCache<K, V> onObjectModifiedDo(VoidStrategy2<K, V> strategy) {
        cache().onObjectModifiedDo(strategy);
        return this;
    }

    @Override
    public RxCache<K, V> onObjectRemovedDo(VoidStrategy2<K, V> strategy) {
        cache().onObjectRemovedDo(strategy);
        return this;
    }

    @Override
    public RxCache<K, V> onUndoDo(VoidStrategy1<Map.Entry<K, V>> strategy) {
        cache().onUndoDo(strategy);
        return this;
    }

    @Override
    public RxCache<K, V> onRedoDo(VoidStrategy1<Map.Entry<K, V>> strategy) {
        cache().onRedoDo(strategy);
        return this;
    }

    @Override
    public void disconnectAll() {
        cache().disconnectAll();
    }

    @Override
    public void addObjectTypeObserver(ObjectTypeObserver<V> observer) {
        cache().addObjectTypeObserver(observer);
    }

    @Override
    public void removeObjectTypeObserver(ObjectTypeObserver<V> observer) {
        cache().removeObjectTypeObserver(observer);
    }

    // -----------------------------------------------------------
    // Interface Reader
    // -----------------------------------------------------------

    @Override
    public boolean containsKey(K key) {
        return cache().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cache().containsValue(value);
    }

    @Override
    public Optional<V> read(K key) {
        return cache().read(key);
    }

    @Override
    public Map<K, V> read(Iterable<? extends K> keys) {
        return cache().read(keys);
    }

    @Override
    public Map<K, V> readAll() {
        return cache().readAll();
    }

    @Override
    public Map<K, V> readExpired() {
        return cache().readExpired();
    }

    @Override
    public Set<K> keySet() {
        return cache().keySet();
    }

    @Override
    public Set<K> keySetExpired() {
        return cache().keySetExpired();
    }

    @Override
    public List<V> readAsList() {
        return cache().readAsList();
    }

    @Override
    public CacheHandle getCacheHandle() {
        return cache().getCacheId();
    }

    @Override
    public int size() {
        return cache().size();
    }

    @Override
    public boolean isEmpty() {
        return cache().isEmpty();
    }

    @Override
    public boolean isExpired(K key) {
        return cache().isExpired(key);
    }

    @Override
    public boolean isExpired() {
        return cache().isExpired();
    }

    @Override
    public void addObjectObserver(ObjectObserver<K, V> observer) {
        cache().addObjectObserver(observer);
    }

    @Override
    public void removeObjectObserver(ObjectObserver<K, V> observer) {
        cache().removeObjectObserver(observer);
    }

    @Override
    public void addCacheObserver(DataCacheObserver dataCacheObserver) {
        cache().addCacheObserver(dataCacheObserver);
    }

    @Override
    public void removeCacheObserver(DataCacheObserver dataCacheObserver) {
        cache().removeCacheObserver(dataCacheObserver);
    }

    @Override
    public void addMementoObserver(MementoObserver<Map.Entry<K, V>> mementoObserver) {
        cache().addMementoObserver(mementoObserver);
    }

    @Override
    public void removeMementoObserver(MementoObserver<Map.Entry<K, V>> mementoObserver) {
        cache().removeMementoObserver(mementoObserver);
    }

    // -----------------------------------------------------------
    // Interface Writer
    // -----------------------------------------------------------

    @Override
    public Optional<V> write(K key, V value) {
        return cache().write(key, value);
    }

    @Override
    public Optional<V> writeAndGet(K key, Supplier<V> factory) {
        return cache().writeAndGet(key, factory);
    }

    @Override
    public Map<? extends K, ? extends V> writeAll(Map<? extends K, ? extends V> values) {
        return cache().writeAll(values);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> factory) {
        return cache().computeIfAbsent(key, factory);
    }

    @Override
    public Optional<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return cache().computeIfPresent(key, remappingFunction);
    }

    @Override
    public Optional<V> compute(K key, BiFunction<? super K, Optional<? super V>, ? extends V> remappingFunction) {
        return cache().compute(key, remappingFunction);
    }

    @Override
    public boolean compareAndWrite(K key, Supplier<V> expect, Supplier<V> update) {
        return cache().compareAndWrite(key, expect, update);
    }

    @Override
    public Optional<V> replace(K key, Supplier<V> update) {
        return cache().replace(key, update);
    }

    @Override
    public Optional<V> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return cache().merge(key, value, remappingFunction);
    }

    @Override
    public Optional<V> take(K key) {
        return cache().take(key);
    }

    @Override
    public Map<K, V> take(Iterable<? extends K> keys) {
        return cache().take(keys);
    }

    @Override
    public Map<K, V> takeAll() {
        return cache().takeAll();
    }

    @Override
    public Map<K, V> takeExpired() {
        return cache().takeExpired();
    }

    @Override
    public void clear() {
        cache().clear();
    }

    // -----------------------------------------------------------
    // Interface MementoCache
    // -----------------------------------------------------------

    @Override
    public List<V> undoStack(K key) {
        return cache().undoStack(key);
    }

    @Override
    public List<V> redoStack(K key) {
        return cache().redoStack(key);
    }

    @Override
    public Optional<V> undo(K key) {
        return cache().undo(key);
    }

    @Override
    public Optional<V> redo(K key) {
        return cache().redo(key);
    }

    @Override
    public RxCache<K, V> clearRedo(K key) {
        cache().clearRedo(key);
        return this;
    }

    @Override
    public RxCache<K, V> clearUndo(K key) {
        cache().clearUndo(key);
        return this;
    }

    @Override
    public Optional<Map.Entry<K, V>> undo() {
        return cache().undo();
    }

    @Override
    public Optional<Map.Entry<K, V>> redo() {
        return cache().redo();
    }

    @Override
    public List<Map.Entry<K, V>> undoStack() {
        return cache().undoStack();
    }

    @Override
    public List<Map.Entry<K, V>> redoStack() {
        return cache().redoStack();
    }

    @Override
    public RxCache<K, V> clearRedo() {
        cache().clearRedo();
        return this;
    }

    @Override
    public RxCache<K, V> clearUndo() {
        cache().clearUndo();
        return this;
    }

    // -----------------------------------------------------------
    // Overridden from Object
    // -----------------------------------------------------------

    protected DataCache<K, V> cache() {
        if (cache.get().isExpired()) {
            return new DataCacheNoAccess<>(cache.get());
        }
        cache.set(requireNonNull(computeIfAbsent.get()));
        return cache.get();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "[cache=" + cache +
                "]";
    }
}
