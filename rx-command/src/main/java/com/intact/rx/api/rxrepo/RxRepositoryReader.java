package com.intact.rx.api.rxrepo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.templates.api.ObservableFluent;
import com.intact.rx.templates.api.RxObserverSupport;

public interface RxRepositoryReader<K, V> extends RxObserverSupport<RxRepositoryReader<K, V>, Map<K, V>>, ObservableFluent<RxRepositoryReader<K, V>, RxObserver<Map<K, V>>> {

    RxCache<K, V> cache();

    /**
     * @param key   maps to value
     * @param msecs wait time before timeout
     * @return value V from cache or remote
     */
    Optional<V> computeIfAbsent(K key, long msecs);

    /**
     * @param key   maps to value
     * @param msecs wait time before timeout
     * @return value V from cache or remote, throws exception if request failed or value is null
     */
    V computeValueIfAbsent(K key, long msecs);

    /**
     * @param key             maps to value
     * @param computeResolver checks if the value found in cache needs updating (false -> no compute, return cached, true -> compute and return fetched value)
     * @param msecs           wait time before timeout
     * @return value V from cache or remote
     */
    Optional<V> computeIf(K key, Strategy1<Boolean, V> computeResolver, long msecs);

    /**
     * @param key             maps to value
     * @param computeResolver checks if the value found in cache needs updating (false -> no compute, return cached, true -> compute and return fetched value)
     * @param msecs           wait time before timeout
     * @return value V from cache or remote
     */
    V computeValueIf(K key, Strategy1<Boolean, V> computeResolver, long msecs);

    /**
     * @param keys  map to value
     * @param msecs wait time before timeout
     * @return values from cache or remote
     */
    Map<K, V> computeIfAbsent(Iterable<K> keys, long msecs);

    /**
     * @param keys            maps to value
     * @param computeResolver checks if the value found in cache needs updating (false -> no compute, return cached, true -> compute and return fetched value)
     * @param msecs           wait time before timeout
     * @return values from cache or remote
     */
    Map<K, V> computeIf(Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs);

    /**
     * @param key   maps to value
     * @param msecs wait time before timeout
     * @return RxRequestResult with status
     */
    RxRequestResult<K, V> computeResultIfAbsent(K key, long msecs);

    /**
     * @param key             maps to value
     * @param computeResolver checks if the value found in cache needs updating (false -> no compute, return cached, true -> compute and return fetched value)
     * @param msecs           wait time before timeout
     * @return RxRequestResult with status
     */
    RxRequestResult<K, V> computeResultIf(K key, Strategy1<Boolean, V> computeResolver, long msecs);

    List<V> computeListIfEmpty(long msecs);

    Map<K, V> computeIfEmpty(long msecs);

    /**
     * @param key   maps to value
     * @param msecs wait time before timeout
     * @return value V from remote
     */
    Optional<V> compute(K key, long msecs);

    /**
     * @param key   maps to value
     * @param msecs wait time before timeout
     * @return value V from remote, throws exception if request failed or value is null
     */
    V computeValue(K key, long msecs);

    /**
     * @param keys  map to value
     * @param msecs wait time before timeout
     * @return values from remote
     */
    Map<K, V> compute(Iterable<K> keys, long msecs);

    /**
     * @param key   maps to value
     * @param msecs wait time before timeout
     * @return RxRequestResult with status
     */
    RxRequestResult<K, V> computeResult(K key, long msecs);

    List<V> computeList(long msecs);

    Map<K, V> compute(long msecs);

    /**
     * Executes and returns immediately
     */
    RxRepositoryReader<K, V> subscribe();

    /**
     * Waits maximum msecs for execution to finish.
     *
     * @param msecs wait time before timeout
     */
    RxRepositoryReader<K, V> waitFor(long msecs);

    RxRequestStatus getStatus();
}
