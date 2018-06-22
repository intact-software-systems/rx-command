package com.intact.rx.api.rxrepo;

import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.cache.RxCache;

public interface RxRepositoryReaderCollection<K, V> {

    Optional<RxCache<K, V>> cache(K requestKey);

    Optional<V> computeIfAbsent(K key, long msecs);

    Map<K, V> computeIfEmpty(long msecs);

    Optional<V> compute(K key, long msecs);

    Map<K, V> compute(long msecs);

    void subscribe(K requestKey);

    void subscribe();
}
