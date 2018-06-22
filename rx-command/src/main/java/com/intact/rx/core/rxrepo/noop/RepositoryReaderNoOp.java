package com.intact.rx.core.rxrepo.noop;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxrepo.RxRepositoryReader;
import com.intact.rx.api.rxrepo.RxRequestResult;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.core.cache.nullobjects.RxCacheNoOp;
import com.intact.rx.core.rxcache.StreamerStatus;
import com.intact.rx.core.rxrepo.RepositoryRequestResult;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;

public class RepositoryReaderNoOp<K, V> implements RxRepositoryReader<K, V> {
    @SuppressWarnings("rawtypes")
    public static final RepositoryReaderNoOp instance = new RepositoryReaderNoOp();

    @Override
    public RxCache<K, V> cache() {
        //noinspection unchecked
        return RxCacheNoOp.instance;
    }

    @Override
    public Optional<V> computeIfAbsent(K key, long msecs) {
        return Optional.empty();
    }

    @Override
    public V computeValueIfAbsent(K key, long msecs) {
        throw new IllegalAccessError("No value accessible");
    }

    @Override
    public Optional<V> computeIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return Optional.empty();
    }

    @Override
    public V computeValueIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        throw new IllegalAccessError("No value accessible");
    }

    @Override
    public Map<K, V> computeIfAbsent(Iterable<K> keys, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, V> computeIf(Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public RxRequestResult<K, V> computeResultIfAbsent(K key, long msecs) {
        return RepositoryRequestResult.no(key);
    }

    @Override
    public RxRequestResult<K, V> computeResultIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return RepositoryRequestResult.no(key);
    }

    @Override
    public List<V> computeListIfEmpty(long msecs) {
        return Collections.emptyList();
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
    public V computeValue(K key, long msecs) {
        throw new IllegalAccessError("No value accessible");
    }

    @Override
    public Map<K, V> compute(Iterable<K> keys, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public RxRequestResult<K, V> computeResult(K key, long msecs) {
        return RepositoryRequestResult.no(key);
    }

    @Override
    public List<V> computeList(long msecs) {
        return Collections.emptyList();
    }

    @Override
    public Map<K, V> compute(long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public RxRepositoryReader<K, V> subscribe() {
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> waitFor(long msecs) {
        return this;
    }

    @Override
    public RxRequestStatus getStatus() {
        return RepositoryRequestStatus.create(new StreamerStatus());
    }

    @Override
    public RxRepositoryReader<K, V> onCompleteDo(VoidStrategy0 completedFunction) {
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> onNextDo(VoidStrategy1<Map<K, V>> nextFunction) {
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> connect(RxObserver<Map<K, V>> rxObserver) {
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> disconnect(RxObserver<Map<K, V>> rxObserver) {
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> disconnectAll() {
        return this;
    }
}

