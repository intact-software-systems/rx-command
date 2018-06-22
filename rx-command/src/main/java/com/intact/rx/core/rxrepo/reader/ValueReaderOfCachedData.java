package com.intact.rx.core.rxrepo.reader;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.*;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.api.rxrepo.RxValueReader;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;
import com.intact.rx.exception.ExecutionNotStartedException;

public class ValueReaderOfCachedData<K, V> implements RxValueReader<V> {
    private final CacheHandle dataCacheHandle;
    private final CachePolicy dataCachePolicy;
    private final K key;

    public ValueReaderOfCachedData(CacheHandle dataCacheHandle, CachePolicy dataCachePolicy, K key) {
        this.dataCacheHandle = requireNonNull(dataCacheHandle);
        this.dataCachePolicy = requireNonNull(dataCachePolicy);
        this.key = requireNonNull(key);
    }

    @Override
    public RxValueAccess<V> accessCached() {
        return cache().access(key);
    }

    @Override
    public RxRequestStatus getStatus() {
        return RepositoryRequestStatus.no();
    }

    @Override
    public Optional<V> computeIfAbsent(long msecs) {
        return cache().read(key);
    }

    @Override
    public Optional<V> computeIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        Optional<V> value = cache().read(key);
        boolean computeValue = true;
        if (value.isPresent()) {
            computeValue = computeResolver.perform(value.get());
        }
        return !computeValue ? value : Optional.empty();
    }

    @Override
    public V computeValueIfAbsent(long msecs) {
        return cache().read(key).orElseThrow(() -> new ExecutionNotStartedException("Request of key " + key + " not started"));
    }

    @Override
    public V computeValueIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        return acceptCachedOrCompute(key, computeResolver).orElseThrow(() -> new ExecutionNotStartedException("Request of key " + key + " not started"));
    }

    @Override
    public Optional<V> compute(long msecs) {
        return cache().read(key);
    }

    @Override
    public V computeValue(long msecs) {
        return cache().read(key).orElseThrow(() -> new ExecutionNotStartedException("Request of key " + key + " not started"));
    }

    @Override
    public RxValueReader<V> subscribe() {
        return this;
    }

    @Override
    public RxValueReader<V> waitFor(long msecs) {
        return this;
    }

    // -----------------------------------------------------------
    // Callback functions
    // -----------------------------------------------------------

    @Override
    public RxValueReader<V> onCompleteDo(VoidStrategy0 completedFunction) {
        return this;
    }

    @Override
    public RxValueReader<V> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        return this;
    }

    @Override
    public RxValueReader<V> onNextDo(VoidStrategy1<V> nextFunction) {
        return this;
    }

    @Override
    public RxValueReader<V> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        return this;
    }

    @Override
    public RxValueReader<V> connect(RxObserver<V> observer) {
        return this;
    }

    @Override
    public RxValueReader<V> disconnect(RxObserver<V> observer) {
        return this;
    }

    @Override
    public RxValueReader<V> disconnectAll() {
        return this;
    }

    // -----------------------------------------------------------
    // private implementation
    // -----------------------------------------------------------

    private RxCache<K, V> cache() {
        return RxCacheAccess.cache(dataCacheHandle, dataCachePolicy);
    }

    private Optional<V> acceptCachedOrCompute(K key, Strategy1<Boolean, V> computeResolver) {
        Optional<V> value = this.cache().read(key);
        if (!value.isPresent()) {
            return value;
        }

        return !computeResolver.perform(value.get())
                ? value
                : Optional.empty();
    }
}
