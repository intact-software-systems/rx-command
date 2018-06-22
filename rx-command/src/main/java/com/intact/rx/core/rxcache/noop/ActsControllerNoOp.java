package com.intact.rx.core.rxcache.noop;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.core.rxcache.api.ActsController;
import com.intact.rx.templates.Tuple2;

public class ActsControllerNoOp implements ActsController {
    public static final ActsControllerNoOp instance = new ActsControllerNoOp();

    @Override
    public <K, V> Tuple2<FutureStatus, Optional<V>> computeIfAbsent(CacheHandle cacheHandle, K key, long msecs) {
        return new Tuple2<>(FutureStatus.NotStarted, Optional.empty());
    }

    @Override
    public <K, V> Tuple2<FutureStatus, Map<K, V>> computeIfAbsent(CacheHandle cacheHandle, Iterable<K> keys, long msecs) {
        return new Tuple2<>(FutureStatus.NotStarted, Collections.emptyMap());
    }

    @Override
    public <K, V> Tuple2<FutureStatus, Optional<V>> computeIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return new Tuple2<>(FutureStatus.NotStarted, Optional.empty());
    }

    @Override
    public <K, V> Tuple2<FutureStatus, Map<K, V>> computeIf(CacheHandle cacheHandle, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return new Tuple2<>(FutureStatus.NotStarted, Collections.emptyMap());
    }

    @Override
    public boolean subscribe() {
        return false;
    }

    @Override
    public boolean unsubscribe() {
        return false;
    }

    @Override
    public boolean isSubscribed() {
        return false;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public FutureStatus waitFor(long msecs) {
        return FutureStatus.NotStarted;
    }

    @Override
    public FutureStatus waitForGroupN(int n, long msecs) {
        return FutureStatus.NotStarted;
    }

    @Override
    public List<Throwable> getExceptions() {
        return Collections.emptyList();
    }
}
