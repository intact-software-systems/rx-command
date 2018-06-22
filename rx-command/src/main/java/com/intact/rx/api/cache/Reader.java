package com.intact.rx.api.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * RxCache reader API for rx.
 *
 * @param <K> type of keys
 * @param <V> type of values
 */
public interface Reader<K, V> {

    /**
     * @param key to lookup in cache
     * @return true if value found
     */
    boolean containsKey(K key);

    /**
     * @param value to lookup in cache
     * @return true if value found
     */
    boolean containsValue(Object value);

    /**
     * @param key to lookup in cache
     * @return optional value
     */
    Optional<V> read(K key);

    /**
     * @param keys to lookup in cache
     * @return values found
     */
    Map<K, V> read(Iterable<? extends K> keys);

    /**
     * @return all values in cache
     */
    Map<K, V> readAll();

    /**
     * @return expired values that remain in cache
     */
    Map<K, V> readExpired();

    /**
     * @return key set in cache
     */
    Set<K> keySet();

    /**
     * @return key set of expired entries in cache
     */
    Set<K> keySetExpired();

    /**
     * @return all values in cache as a list
     */
    List<V> readAsList();

    /**
     * @return size of cache
     */
    int size();

    /**
     * @return true if empty
     */
    boolean isEmpty();

    /**
     * @param key to lookup in cache
     * @return true if value is expired
     */
    boolean isExpired(K key);

    /**
     * @return tue if cache identified by @getCacheId is expired
     */
    boolean isExpired();
}
