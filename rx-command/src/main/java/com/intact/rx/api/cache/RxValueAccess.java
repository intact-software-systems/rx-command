package com.intact.rx.api.cache;

import java.util.Optional;
import java.util.function.Supplier;

import com.intact.rx.api.command.VoidStrategy1;

public interface RxValueAccess<V> extends ValueMemento<V>, AutoCloseable {

    /**
     * @return key used to access value
     */
    Object key();

    /**
     * @return true if key represented by object is cached
     */
    boolean isContained();

    /**
     * @return latest value from cache
     */
    Optional<V> read();

    /**
     * @return value taken out of cache
     */
    Optional<V> take();

    /**
     * @param value to be written to cache
     * @return optional overwritten value
     */
    Optional<V> write(V value);

    /**
     * Read and return value by key if it exists. If key does not exist then use
     * lambda to create value and write to cache, then return created value.
     *
     * @param factory to create value V
     * @return existing or created value
     */
    V computeIfAbsent(Supplier<V> factory);

    /**
     * @return value identification
     */
    ValueHandle<Object> getValueHandle();

    /**
     * @return tue if cached value is expired
     */
    boolean isExpired();

    /**
     * @param strategy to execute when value stored with key is created
     */
    RxValueAccess<V> onObjectCreatedDo(VoidStrategy1<V> strategy);

    /**
     * @param strategy to execute when value stored with key is modified
     */
    RxValueAccess<V> onObjectModifiedDo(VoidStrategy1<V> strategy);

    /**
     * @param strategy to execute when value stored with key is expired/deleted
     */
    RxValueAccess<V> onObjectRemovedDo(VoidStrategy1<V> strategy);

    /**
     * @param strategy to execute when value stored with key is undid
     */
    RxValueAccess<V> onUndoDo(VoidStrategy1<V> strategy);

    /**
     * @param strategy to execute when value stored with key is modified
     */
    RxValueAccess<V> onRedoDo(VoidStrategy1<V> strategy);

    /**
     * Disconnect all observers
     */
    void disconnectAll();

    /**
     * Detach this from main cache, i.e., no longer receive callbacks.
     */
    void detach();

    /**
     * Alternate detach, for convenience for "execute around object" pattern, i.e., try with resource close.
     */
    @Override
    void close();

    /**
     * @return true if receiving updates from main cache
     */
    boolean isAttached();

    // -------------------------------------------------------------
    // Memento API
    // -------------------------------------------------------------

    @Override
    RxValueAccess<V> clearRedo();

    @Override
    RxValueAccess<V> clearUndo();
}
