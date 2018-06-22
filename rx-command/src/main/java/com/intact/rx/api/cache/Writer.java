package com.intact.rx.api.cache;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * RxCache writer API for rx.
 *
 * @param <K> type of keys
 * @param <V> type of values
 */
public interface Writer<K, V> {

    /**
     * @param key   to find value
     * @param value to be written to cache
     * @return optional overwritten value
     */
    Optional<V> write(K key, V value);

    /**
     * @param key     to find value
     * @param factory supplies the value to be written to cache
     * @return value written to cache
     */
    Optional<V> writeAndGet(K key, Supplier<V> factory);

    /**
     * @param values to be written to cache
     * @return overwritten values
     */
    Map<? extends K, ? extends V> writeAll(Map<? extends K, ? extends V> values);

    /**
     * Read and return value by key if it exists. If key does not exist then use
     * lambda to create value and write to cache, then return created value.
     *
     * @param key     to find value
     * @param factory supplies the value to be written to cache
     * @return existing or created value
     */
    V computeIfAbsent(K key, Function<? super K, ? extends V> factory);

    /**
     * If the value for the specified key is present and non-null, attempts to
     * compute a new mapping given the key and its current mapped value.
     * <p>
     * <p>If the function returns {@code null}, the mapping is removed.  If the
     * function itself throws an (unchecked) exception, the exception is
     * rethrown, and the current mapping is left unchanged.
     *
     * @param key               key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     */
    Optional<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Writes the value to the given updated value using remapping function.
     *
     * @param remappingFunction supplies the new value
     * @return previous value associated with key
     */
    Optional<V> compute(K key, BiFunction<? super K, Optional<? super V>, ? extends V> remappingFunction);

    /**
     * Writes the value to the given updated value if the current value {@code ==} the expected value.
     *
     * @param expect supplies the expected value
     * @param update supplies the new value
     * @return {@code true} if successful, false if the actual value was not equal to the expected value.
     */
    default boolean compareAndWrite(K key, V expect, V update) {
        return compareAndWrite(key, () -> expect, () -> update);
    }

    /**
     * Writes the value to the given updated value if the current value {@code ==} the expected value.
     *
     * @param expect supplies the expected value
     * @param update supplies the new value
     * @return {@code true} if successful, false if the actual value was not equal to the expected value.
     */
    boolean compareAndWrite(K key, Supplier<V> expect, Supplier<V> update);

    /**
     * Writes the value to the given updated value if value associated with key is present.
     *
     * @param update supplies the new value
     * @return previous value associated with key
     */
    default Optional<V> replace(K key, V update) {
        return replace(key, () -> update);
    }

    /**
     * Writes the value to the given updated value if value associated with key is present.
     *
     * @param update supplies the new value
     * @return previous value associated with key
     */
    Optional<V> replace(K key, Supplier<V> update);

    /**
     * If the specified key is not already associated with a value or is
     * associated with null, associates it with the given non-null value.
     * Otherwise, replaces the associated value with the results of the given
     * remapping function, or removes if the result is {@code null}. This
     * method may be of use when combining multiple mapped values for a key.
     *
     * @param key               key with which the resulting value is to be associated
     * @param value             the non-null value to be merged with the existing value
     *                          associated with the key or, if no existing value or a null value
     *                          is associated with the key, to be associated with the key
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if no value is associated with the key
     */
    Optional<V> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction);

    /**
     * @param key to find value
     * @return value taken out of cache
     */
    Optional<V> take(K key);

    /**
     * @param keys to find values
     * @return values taken out of cache
     */
    Map<K, V> take(Iterable<? extends K> keys);

    /**
     * @return all (key, value) pairs in cache, none remain.
     */
    Map<K, V> takeAll();

    /**
     * @return expired (key, value) pairs in cache.
     */
    Map<K, V> takeExpired();

    /**
     * Remove all entries from cache.
     */
    void clear();
}
