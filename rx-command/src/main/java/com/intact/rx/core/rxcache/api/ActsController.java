package com.intact.rx.core.rxcache.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.templates.Tuple2;

public interface ActsController {

    <K, V> Tuple2<FutureStatus, Optional<V>> computeIfAbsent(CacheHandle cacheHandle, K key, long msecs);

    <K, V> Tuple2<FutureStatus, Map<K, V>> computeIfAbsent(CacheHandle cacheHandle, Iterable<K> keys, long msecs);

    <K, V> Tuple2<FutureStatus, Optional<V>> computeIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs);

    <K, V> Tuple2<FutureStatus, Map<K, V>> computeIf(CacheHandle cacheHandle, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs);

    boolean subscribe();

    boolean unsubscribe();

    boolean isSubscribed();

    void cancel();

    boolean isSuccess();

    boolean isCancelled();

    /**
     * Waits for one controller execution.
     *
     * @param msecs max time until function returns
     * @return status on execution
     */
    FutureStatus waitFor(long msecs);

    /**
     * Waits for a group to finish. However, it must be already started.
     *
     * @param msecs max time until function returns
     * @return status on execution
     */
    FutureStatus waitForGroupN(int n, long msecs);

    List<Throwable> getExceptions();
}
