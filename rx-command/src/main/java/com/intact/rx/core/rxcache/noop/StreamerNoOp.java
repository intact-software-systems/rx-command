package com.intact.rx.core.rxcache.noop;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxcache.RxStreamerStatus;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.nullobjects.RxCacheNoOp;
import com.intact.rx.core.rxcache.StreamerStatus;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.core.rxcache.api.ActsController;
import com.intact.rx.exception.ExecutionNotStartedException;

public class StreamerNoOp implements RxStreamer {

    public static final StreamerNoOp instance = new StreamerNoOp();

    @Override
    public MasterCacheId getMasterCacheId() {
        return MasterCacheId.empty();
    }

    @Override
    public RxConfig getRxConfig() {
        return RxConfig.fromRxDefault();
    }

    @Override
    public RxStreamer subscribe() {
        return this;
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
    public RxStreamer waitFor(long msecs) {
        return this;
    }

    @Override
    public FutureStatus waitForResult(long msecs) {
        return FutureStatus.NotStarted;
    }

    @Override
    public RxStreamerStatus getStatus() {
        return new StreamerStatus();
    }

    @Override
    public <K, V> RxStreamer onNextDo(VoidStrategy1<Map<K, V>> observer, Class<V> cachedType) {
        return this;
    }

    @Override
    public <K, V> RxStreamer connect(RxObserver<Map<K, V>> subject, Class<V> cachedType) {
        return this;
    }

    @Override
    public ActsController controller() {
        return ActsControllerNoOp.instance;
    }

    @Override
    public RxStreamerStatus getAsyncStatus() {
        return new StreamerStatus();
    }

    @Override
    public <K1, T> RxCache<K1, T> cache(CacheHandle cacheHandle) {
        //noinspection unchecked
        return RxCacheNoOp.instance;
    }

    @Override
    public <K1, T> RxCache<K1, T> cache(Class<T> from) {
        //noinspection unchecked
        return RxCacheNoOp.instance;
    }

    @Override
    public <V> List<V> computeListIfEmpty(Class<V> aClass, long msecs) {
        return Collections.emptyList();
    }

    @Override
    public <V> List<V> computeListIfEmpty(CacheHandle cacheHandle, long msecs) {
        return Collections.emptyList();
    }

    @Override
    public <K, V> Map<K, V> computeIfEmpty(Class<V> aClass, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Map<K, V> computeIfEmpty(CacheHandle cacheHandle, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Map<K, V> computeIfAbsent(CacheHandle cacheHandle, Iterable<K> keys, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Map<K, V> computeIfAbsent(Class<V> cachedType, Iterable<K> keys, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Map<K, V> computeIf(CacheHandle cacheHandle, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Map<K, V> computeIf(Class<V> cachedType, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Optional<V> computeIfAbsent(Class<V> aClass, K key, long msecs) {
        return Optional.empty();
    }

    @Override
    public <K, V> Optional<V> computeIfAbsent(CacheHandle cacheHandle, K key, long msecs) {
        return Optional.empty();
    }

    @Override
    public <K, V> Optional<V> computeIf(Class<V> aClass, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return Optional.empty();
    }

    @Override
    public <K, V> Optional<V> computeIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return Optional.empty();
    }

    @Override
    public <K, V> V computeValueIfAbsent(CacheHandle cacheHandle, K key, long msecs) {
        throw new ExecutionNotStartedException("Fetching " + key + " never started");
    }

    @Override
    public <K, V> V computeValueIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        throw new ExecutionNotStartedException("Fetching " + key + " never started");
    }

    @Override
    public <V> List<V> computeList(Class<V> cachedType, long msecs) {
        return Collections.emptyList();
    }

    @Override
    public <V> List<V> computeList(CacheHandle cacheHandle, long msecs) {
        return Collections.emptyList();
    }

    @Override
    public <K, V> Map<K, V> compute(Class<V> cachedType, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Map<K, V> compute(CacheHandle cacheHandle, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Map<K, V> compute(CacheHandle cacheHandle, Iterable<K> keys, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Map<K, V> compute(Class<V> cachedType, Iterable<K> keys, long msecs) {
        return Collections.emptyMap();
    }

    @Override
    public <K, V> Optional<V> compute(Class<V> cachedType, K key, long msecs) {
        return Optional.empty();
    }

    @Override
    public <K, V> Optional<V> compute(CacheHandle cacheHandle, K key, long msecs) {
        return Optional.empty();
    }

    @Override
    public <K, V> V computeValue(CacheHandle cacheHandle, K key, long msecs) {
        throw new ExecutionNotStartedException("Fetching " + key + " never started");
    }

    @Override
    public RxStreamer clearCache() {
        return this;
    }

    @Override
    public <V> RxStreamer clearCache(Class<V> cachedType) {
        return this;
    }

    @Override
    public RxStreamer clearCache(MasterCacheId masterCacheId) {
        return this;
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onNext(ActGroup value) {

    }

    @Override
    public void onSubscribe(Subscription subscription) {

    }

    @Override
    public RxStreamer onCompleteDo(VoidStrategy0 completedFunction) {
        return this;
    }

    @Override
    public RxStreamer onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        return this;
    }

    @Override
    public RxStreamer onNextDo(VoidStrategy1<ActGroup> nextFunction) {
        return this;
    }

    @Override
    public RxStreamer onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        return this;
    }
}
