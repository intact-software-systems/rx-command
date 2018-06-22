package com.intact.rx.api.cache;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import com.intact.rx.api.cache.observer.DataCacheObserver;
import com.intact.rx.api.cache.observer.MementoObserver;
import com.intact.rx.api.cache.observer.ObjectObserver;
import com.intact.rx.api.cache.observer.ObjectTypeObserver;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.command.VoidStrategy2;
import com.intact.rx.policy.LoanPolicy;

public interface RxCache<K, V> extends ReaderWriter<K, V>, MementoCache<K, V> {

    /**
     * @return cache identification
     */
    CacheHandle getCacheHandle();

    /**
     * @param key to lookup value in cache
     * @return proxy to access (key, value) residing in this cache
     */
    RxValueAccess<V> access(K key);

    /**
     * @param key to lookup value in cache
     * @return proxy to edit value outside main cache
     */
    ValueEditor<V> edit(K key);

    /**
     * @return proxy to edit contents of cache without interruption from main cache
     */
    Editor<K, V> edit();

    /**
     * @param transformer function
     * @param <T>         new type T
     * @return selection
     */
    <T> RxSelection<K, T> computeTransformation(Transformation<T, V> transformer);

    /**
     * @param transformationId key to identify transformation
     * @param transformer      function
     * @param <T>              new type T
     * @return selection
     */
    <T> RxSelection<K, T> computeTransformationIfAbsent(Object transformationId, Transformation<T, V> transformer);

    /**
     * @param transformationId key to identify transformation
     * @return selection if it exists
     */
    Optional<RxSelection<K, V>> readTransformation(Object transformationId);

    /**
     * @param transformationId key to identify selection
     * @return optional removed selection
     */
    <T> Optional<RxSelection<K, T>> detachTransformation(Object transformationId);

    /**
     * @param filter data from this cache into selection
     * @return selection
     */
    RxSelection<K, V> computeSelection(Filter<V> filter);

    /**
     * @param selectionId key to identify selection
     * @param filter      to be used in selection
     * @return selection
     */
    RxSelection<K, V> computeSelectionIfAbsent(Object selectionId, Filter<V> filter);

    /**
     * @param selectionId key to identify selection
     * @return selection if it exists
     */
    Optional<RxSelection<K, V>> readSelection(Object selectionId);

    /**
     * @param selectionId key to identify selection
     * @return optional removed selection
     */
    Optional<RxSelection<K, V>> detachSelection(Object selectionId);

    /**
     * @param key        to lookup in cache
     * @param loanPolicy policy on loan
     * @return loaned value access
     */
    Loaned<V> loan(K key, LoanPolicy loanPolicy);

    /**
     * Read and return value by key if it exists. If key does not exist then use
     * lambda to create value and write to cache, then return created value.
     *
     * @param key        to find value
     * @param factory    to create value V
     * @param loanPolicy policy on loan
     * @return existing or created value
     */
    Loaned<V> computeIfAbsentAndLoan(K key, Function<? super K, ? extends V> factory, LoanPolicy loanPolicy);

    /**
     * Expires value represented by key from cache if it
     * a) was loaned, and
     * b) loan-count is zero after reloan (return of loaned value).
     *
     * @param loan to return
     * @return optional value is set if removed from cache as part of return
     */
    Optional<V> returnLoan(Loaned<V> loan);

    /**
     * @param key to lookup in cache
     * @return true if value found is loaned
     */
    boolean isLoaned(K key);

    /**
     * @param strategy to execute when value stored with key is created
     */
    RxCache<K, V> onObjectCreatedDo(VoidStrategy2<K, V> strategy);

    /**
     * @param strategy to execute when value stored with key is modified
     */
    RxCache<K, V> onObjectModifiedDo(VoidStrategy2<K, V> strategy);

    /**
     * @param strategy to execute when value stored with key is expired/deleted
     */
    RxCache<K, V> onObjectRemovedDo(VoidStrategy2<K, V> strategy);

    /**
     * @param strategy to execute when undo performed on cache
     */
    RxCache<K, V> onUndoDo(VoidStrategy1<Entry<K, V>> strategy);

    /**
     * @param strategy to execute when redo performed on cache
     */
    RxCache<K, V> onRedoDo(VoidStrategy1<Entry<K, V>> strategy);

    /**
     * disconnects all observers
     */
    void disconnectAll();

    // -----------------------------------------------------------
    // Observer API
    // -----------------------------------------------------------

    void addObjectTypeObserver(ObjectTypeObserver<V> observer);

    void removeObjectTypeObserver(ObjectTypeObserver<V> observer);

    void addObjectObserver(ObjectObserver<K, V> observer);

    void removeObjectObserver(ObjectObserver<K, V> observer);

    void addCacheObserver(DataCacheObserver dataCacheObserver);

    void removeCacheObserver(DataCacheObserver dataCacheObserver);

    void addMementoObserver(MementoObserver<Entry<K, V>> mementoObserver);

    void removeMementoObserver(MementoObserver<Entry<K, V>> mementoObserver);

    // -----------------------------------------------------------
    // Memento API
    // -----------------------------------------------------------

    @Override
    RxCache<K, V> clearUndo(K key);

    @Override
    RxCache<K, V> clearRedo(K key);

    @Override
    RxCache<K, V> clearUndo();

    @Override
    RxCache<K, V> clearRedo();
}
