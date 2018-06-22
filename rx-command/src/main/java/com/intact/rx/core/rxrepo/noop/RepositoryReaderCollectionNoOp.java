package com.intact.rx.core.rxrepo.noop;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.rxrepo.RxRepositoryReaderCollection;

public class RepositoryReaderCollectionNoOp<K, V> implements RxRepositoryReaderCollection<K, V> {
    @SuppressWarnings("rawtypes")
    public static final RepositoryReaderCollectionNoOp instance = new RepositoryReaderCollectionNoOp();

    @Override
    public Optional<RxCache<K, V>> cache(K requestKey) {
        return Optional.empty();
    }

    @Override
    public Optional<V> computeIfAbsent(K key, long msecs) {
        return Optional.empty();
    }

    @Override
    public Map<K, V> computeIfEmpty(long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public Optional<V> compute(K key, long msecs) {
        return Optional.empty();
    }

    @Override
    public Map<K, V> compute(long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public void subscribe(K requestKey) {

    }

    @Override
    public void subscribe() {

    }
}
