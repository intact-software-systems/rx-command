package com.intact.rx.core.rxrepo.writer;

import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.*;
import com.intact.rx.api.rxrepo.RxRepositoryWriter;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;

public class RepositoryWriterToCache<K, V> implements RxRepositoryWriter<K, V> {
    private final CachePolicy cachePolicy;
    private final CacheHandle cacheHandle;

    public RepositoryWriterToCache(CachePolicy cachePolicy, CacheHandle cacheHandle) {
        this.cachePolicy = cachePolicy;
        this.cacheHandle = cacheHandle;
    }

    @Override
    public RxCache<K, V> cache() {
        return RxCacheAccess.cache(cacheHandle, cachePolicy);
    }

    @Override
    public boolean put(K key, V value, long msecs) {
        return cache().write(key, value).isPresent();
    }

    @Override
    public boolean putAndRemoveLocal(K key, V value, long msecs) {
        return cache().take(key).isPresent();
    }

    @Override
    public Optional<V> putAndGet(K key, V value, long msecs) {
        cache().write(key, value);
        return cache().read(key);
    }

    @Override
    public Optional<V> preComputeAndPut(K key, V value, Strategy3<V, K, V, Optional<V>> preComputer, long msecs) {
        return cache().write(key, preComputer.perform(key, value, cache().read(key)));
    }

    @Override
    public Optional<V> putAndPostCompute(K key, V value, Strategy4<V, Boolean, K, V, Optional<V>> postComputer, long msecs) {
        Optional<V> previous = cache().read(key);
        return cache().write(key, postComputer.perform(true, key, value, previous));
    }

    @Override
    public boolean compute(K key, Strategy0<V> computer, long msecs) {
        cache().write(key, computer.perform());
        return true;
    }

    @Override
    public boolean putIfAbsent(K key, V value, long msecs) {
        cache().write(key, value);
        return true;
    }

    @Override
    public boolean putIfPresent(K key, V value, long msecs) {
        cache().write(key, value);
        return true;
    }

    @Override
    public boolean putIfNotEqual(K key, V value, long msecs) {
        cache().write(key, value);
        return true;
    }

    @Override
    public boolean putIf(Strategy2<Boolean, K, V> criterion, K key, V value, long msecs) {
        if (criterion.perform(key, value)) {
            cache().write(key, value);
            return true;
        }
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
