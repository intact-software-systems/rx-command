package com.intact.rx.core.rxrepo.reader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxrepo.RxRepositoryReader;
import com.intact.rx.api.rxrepo.RxRequestResult;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.core.rxcache.StreamerStatus;
import com.intact.rx.core.rxrepo.RepositoryRequestResult;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;
import com.intact.rx.exception.ExecutionNotStartedException;

/**
 * Only return cached data
 */
public class RepositoryReaderOfCachedData<K, V> implements RxRepositoryReader<K, V> {
    private final CacheHandle dataCacheHandle;
    private final CachePolicy dataCachePolicy;

    public RepositoryReaderOfCachedData(CacheHandle dataCacheHandle, CachePolicy dataCachePolicy) {
        this.dataCacheHandle = dataCacheHandle;
        this.dataCachePolicy = dataCachePolicy;
    }

    @Override
    public RxCache<K, V> cache() {
        return RxCacheAccess.cache(dataCacheHandle, dataCachePolicy);
    }

    @Override
    public Optional<V> computeIfAbsent(K key, long msecs) {
        return cache().read(key);
    }

    @Override
    public V computeValueIfAbsent(K key, long msecs) {
        return cache().read(key).orElseThrow(() -> new ExecutionNotStartedException("Request of key " + key + " not started"));
    }

    @Override
    public Optional<V> computeIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return acceptCachedOrCompute(key, computeResolver);
    }

    @Override
    public V computeValueIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return acceptCachedOrCompute(key, computeResolver).orElseThrow(() -> new ExecutionNotStartedException("Request of key " + key + " not started"));
    }

    @Override
    public Map<K, V> computeIfAbsent(Iterable<K> keys, long msecs) {
        return cache().read(keys);
    }

    @Override
    public Map<K, V> computeIf(Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return acceptCachedOrCompute(keys, computeResolver);
    }

    @Override
    public RxRequestResult<K, V> computeResultIfAbsent(K key, long msecs) {
        return RepositoryRequestResult.create(
                key,
                cache().read(key).orElse(null),
                RepositoryRequestStatus.create(new StreamerStatus())
        );
    }

    @Override
    public RxRequestResult<K, V> computeResultIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return RepositoryRequestResult.create(
                key,
                cache().read(key).orElse(null),
                RepositoryRequestStatus.create(new StreamerStatus())
        );
    }

    @Override
    public List<V> computeListIfEmpty(long msecs) {
        return cache().readAsList();
    }

    @Override
    public Map<K, V> computeIfEmpty(long msecs) {
        return cache().readAll();
    }

    @Override
    public Optional<V> compute(K key, long msecs) {
        return cache().read(key);
    }

    @Override
    public V computeValue(K key, long msecs) {
        return cache().read(key).orElseThrow(() -> new ExecutionNotStartedException("Request of key " + key + " not started"));
    }

    @Override
    public Map<K, V> compute(Iterable<K> keys, long msecs) {
        return cache().read(keys);
    }

    @Override
    public RxRequestResult<K, V> computeResult(K key, long msecs) {
        return RepositoryRequestResult.create(
                key,
                cache().read(key).orElse(null),
                RepositoryRequestStatus.create(new StreamerStatus())
        );
    }

    @Override
    public List<V> computeList(long msecs) {
        return cache().readAsList();
    }

    @Override
    public Map<K, V> compute(long msecs) {
        return cache().readAll();
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

    // -----------------------------------------------------------
    // Callback functions
    // -----------------------------------------------------------

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
    public RxRepositoryReader<K, V> connect(RxObserver<Map<K, V>> observer) {
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> disconnect(RxObserver<Map<K, V>> observer) {
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> disconnectAll() {
        return this;
    }

    // -----------------------------------------------------------
    // private implementation
    // -----------------------------------------------------------

    private Optional<V> acceptCachedOrCompute(K key, Strategy1<Boolean, V> computeResolver) {
        Optional<V> value = cache().read(key);
        if (!value.isPresent()) {
            return value;
        }
        return !computeResolver.perform(value.get())
                ? value
                : Optional.empty();
    }

    private Map<K, V> acceptCachedOrCompute(Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver) {
        Map<K, V> values = cache().read(keys);
        if (values.isEmpty()) {
            return values;
        }
        return !computeResolver.perform(values)
                ? values
                : Collections.emptyMap();
    }
}
