package com.intact.rx.api.cache;

import java.util.Optional;

import com.intact.rx.templates.api.Memento;

/**
 * Edit cached value outside the main cache.
 * <p>
 * Note: Requires Object.equal() to be implemented by V.
 *
 * @param <V>
 */
public interface ValueEditor<V> {

    /**
     * @return key that represents cached value
     */
    Object getKey();

    /**
     * @return valueHandle that identifies cached value
     */
    ValueHandle<Object> getValueHandle();

    /**
     * @return edited value, initial value is identical to cached value.
     */
    Optional<V> readEdited();

    /**
     * @return cached value
     */
    Optional<V> readCached();

    /**
     * @param newVale to store in this object outside main cache
     * @return previous value
     */
    Optional<V> write(V newVale);

    /**
     * @return history of edited value
     */
    Memento<V> getMemento();

    /**
     * @return true if edited value is not equal cached value. Requires equal to be implemented by V.
     */
    boolean isModified();

    /**
     * Commits/writes edited value to cache if non-null and modified.
     *
     * @return true if commited to cache.
     */
    boolean commit();
}
