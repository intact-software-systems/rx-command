package com.intact.rx.api.rxrepo;

import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy2;
import com.intact.rx.api.command.Strategy3;
import com.intact.rx.api.command.Strategy4;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;
import com.intact.rx.templates.api.ObservableFluent;
import com.intact.rx.templates.api.RxObserverSupport;

/**
 * API for a repository writer, implicit that implementation is setup with a lambda that executes the write command: Strategy2 Boolean, K, V
 *
 * @param <K> key used to find value in cache
 * @param <V> value to be saved in cache
 */
public interface RxRepositoryWriter<K, V> extends RxObserverSupport<RxRepositoryWriter<K, V>, Map<K, V>>, ObservableFluent<RxRepositoryWriter<K, V>, RxObserver<Map<K, V>>> {

    RxCache<K, V> cache();

    /**
     * Executes pusher and caches (key, value) if success.
     *
     * @param key   key to be used by write command
     * @param value value to be used by write command
     * @param msecs max wait time in milliseconds
     * @return true if write command returned true.
     */
    boolean put(K key, V value, long msecs);

    /**
     * Does not cache (key, value) and removes existing (key, value) from cache.
     *
     * @param key   key to be used by write command
     * @param value value to be used by write command
     * @param msecs max wait time in milliseconds
     * @return true if command was executed and value removed from local cache.
     */
    boolean putAndRemoveLocal(K key, V value, long msecs);

    /**
     * Executes pusher and caches (key, value) if success.
     *
     * @param key   key to be used by write command
     * @param value value to be used by write command
     * @return content of cache
     */
    Optional<V> putAndGet(K key, V value, long msecs);

    /**
     * Use preComputer to:
     * <p>
     * V computedValue = preComputer.perform(key, newValue, cachedValue);
     * <p>
     * computedValue is used by previously given writer lambda and then written to cache
     */
    Optional<V> preComputeAndPut(K key, V value, Strategy3<V, K, V, Optional<V>> preComputer, long msecs);

    /**
     * Use previously given writer lambda to:
     * <p>
     * Boolean isAccepted = pusher.perform(key, value);
     * V toBeCached = postComputer.perform(isAccepted, key, value, oldCachedValue);
     * <p>
     * toBeCached is cached
     */
    Optional<V> putAndPostCompute(K key, V value, Strategy4<V, Boolean, K, V, Optional<V>> postComputer, long msecs);

    /**
     * Computer returns value V to be used by writer lambda, and is written to cache.
     */
    boolean compute(K key, Strategy0<V> computer, long msecs);

    boolean putIfAbsent(K key, V value, long msecs);

    boolean putIfPresent(K key, V value, long msecs);

    boolean putIfNotEqual(K key, V value, long msecs);

    boolean putIf(Strategy2<Boolean, K, V> criterion, K key, V value, long msecs);

    RepositoryRequestStatus lastRequestStatus();
}
