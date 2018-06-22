package com.intact.rx.api.cache.observer;

/**
 * Attach to DataCache (the home for type T)
 */
public interface ObjectObserver<K, V> {

    void onObjectCreated(K key, V value);

    void onObjectRemoved(K key, V value);

    void onObjectModified(K key, V value);
}
