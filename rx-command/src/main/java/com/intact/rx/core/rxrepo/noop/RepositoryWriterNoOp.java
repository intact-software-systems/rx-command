package com.intact.rx.core.rxrepo.noop;

import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.*;
import com.intact.rx.api.rxrepo.RxRepositoryWriter;
import com.intact.rx.core.cache.nullobjects.RxCacheNoOp;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;

public class RepositoryWriterNoOp<K, V> implements RxRepositoryWriter<K, V> {
    @SuppressWarnings("rawtypes")
    public static final RepositoryWriterNoOp instance = new RepositoryWriterNoOp();

    @Override
    public RxCache<K, V> cache() {
        //noinspection unchecked
        return RxCacheNoOp.instance;
    }

    @Override
    public boolean put(K key, V value, long msecs) {
        return false;
    }

    @Override
    public boolean putAndRemoveLocal(K key, V value, long msecs) {
        return false;
    }

    @Override
    public Optional<V> putAndGet(K key, V value, long msecs) {
        return Optional.empty();
    }

    @Override
    public Optional<V> preComputeAndPut(K key, V value, Strategy3<V, K, V, Optional<V>> preComputer, long msecs) {
        return Optional.empty();
    }

    @Override
    public Optional<V> putAndPostCompute(K key, V value, Strategy4<V, Boolean, K, V, Optional<V>> postComputer, long msecs) {
        return Optional.empty();
    }

    @Override
    public boolean compute(K key, Strategy0<V> computer, long msecs) {
        return false;
    }

    @Override
    public boolean putIfAbsent(K key, V value, long msecs) {
        return false;
    }

    @Override
    public boolean putIfPresent(K key, V value, long msecs) {
        return false;
    }

    @Override
    public boolean putIfNotEqual(K key, V value, long msecs) {
        return false;
    }

    @Override
    public boolean putIf(Strategy2<Boolean, K, V> criterion, K key, V value, long msecs) {
        return false;
    }

    @Override
    public RepositoryRequestStatus lastRequestStatus() {
        return RepositoryRequestStatus.no();
    }

    @Override
    public RxRepositoryWriter<K, V> connect(RxObserver<Map<K, V>> mapRxObserver) {
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> disconnect(RxObserver<Map<K, V>> mapRxObserver) {
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> disconnectAll() {
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> onCompleteDo(VoidStrategy0 completedFunction) {
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> onNextDo(VoidStrategy1<Map<K, V>> nextFunction) {
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        return this;
    }
}
