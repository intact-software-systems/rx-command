package com.intact.rx.api.rxrepo;

import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.Strategy2;

/**
 * The caller provides the lambda to perform the value lookup.
 * <p>
 * Methods return empty optionals when the requested item does not exist.
 * <p>
 * Typical usage: accessor.get(key, k -> service.get(k))
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public interface RxRepositoryAccessor<K, V> {

    /**
     * Get the value with a supplier that does the getter.  Should throw an exception on retrieval failure or timeouts.
     *
     * @param key    Key
     * @param getter getter function
     * @return value V from cache or remote
     */
    Optional<V> computeIfAbsent(K key, Strategy1<V, K> getter);

    /**
     * @param key             Key
     * @param computeResolver checks if the value found in cache needs updating (false -> no compute, return cached, true -> compute and return fetched value)
     * @param getter          getter function
     * @return value V from cache or remote
     */
    Optional<V> computeIf(K key, Strategy1<Boolean, V> computeResolver, Strategy1<V, K> getter);

    /**
     * @param key    Key
     * @param getter getter function
     * @return value V from cache or remote, throws exception if request failed or value is null
     */
    V computeValueIfAbsent(K key, Strategy1<V, K> getter);

    /**
     * @param key             Key
     * @param computeResolver checks if the value found in cache needs updating (false -> no compute, return cached, true -> compute and return fetched value)
     * @param getter          getter function
     * @return value V from cache or remote, throws exception if request failed or value is null
     */
    V computeValueIf(K key, Strategy1<Boolean, V> computeResolver, Strategy1<V, K> getter);

    /**
     * Get multiple values.  Should throw an exception on retrieval failure or timeouts.
     *
     * @param keys   Keys
     * @param getter getter function
     * @return values
     */
    Map<K, V> computeIfAbsent(Iterable<K> keys, Strategy1<V, K> getter);

    /**
     * Get the value with a supplier that does the getter.  Should throw an exception on retrieval failure or timeouts.
     *
     * @param key    Key
     * @param getter getter function
     * @return value V from remote
     */
    Optional<V> compute(K key, Strategy1<V, K> getter);

    /**
     * @param key    Key
     * @param getter getter function
     * @return value V from remote, throws exception if request failed or value is null
     */
    V computeValue(K key, Strategy1<V, K> getter);

    /**
     * Get multiple values.  Should throw an exception on retrieval failure or timeouts.
     *
     * @param keys   Keys
     * @param getter getter function
     * @return values
     */
    Map<K, V> compute(Iterable<K> keys, Strategy1<V, K> getter);

    /**
     * Executes putter and caches (key, value) if success.
     *
     * @param key    key to be used by write command
     * @param value  value to be used by write command
     * @param putter lambda to perform put
     * @return true if write command returned true.
     */
    boolean put(K key, V value, Strategy2<Boolean, K, V> putter);

    /**
     * Does not cache (key, value) and removes existing (key, value) from cache.
     *
     * @param key    key to be used by write command
     * @param value  value to be used by write command
     * @param putter lambda to perform put
     * @return true if command was executed and value removed from local cache.
     */
    boolean putAndRemoveLocal(K key, V value, Strategy2<Boolean, K, V> putter);

    /**
     * Executes putter and caches (key, value) if success.
     *
     * @param key    key to be used by write command
     * @param value  value to be used by write command
     * @param putter lambda to perform put
     * @return content of cache
     */
    Optional<V> putAndGet(K key, V value, Strategy2<Boolean, K, V> putter);

    /**
     * Executes putter and caches (key, value) if success.
     *
     * @param key    key to be used by write command
     * @param value  value to be used by write command
     * @param putter lambda to perform put
     * @return content of cache
     */
    boolean putIfAbsent(K key, V value, Strategy2<Boolean, K, V> putter);
}
