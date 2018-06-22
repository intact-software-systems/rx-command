package com.intact.rx.core.rxcache;

import java.util.*;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxcache.RxStreamerStatus;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.factory.CacheFactory;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.core.rxcache.api.ActsController;
import com.intact.rx.core.rxcache.noop.ActsControllerNoOp;
import com.intact.rx.exception.ExecutionEndedWithNoResultException;

public class StreamerOnlyCacheAccess implements RxStreamer {
    private final DomainCacheId domainCacheId;
    private final MasterCacheId masterCacheId;
    private final CachePolicy cachePolicy;

    public StreamerOnlyCacheAccess(DomainCacheId domainCacheId, MasterCacheId masterCacheId, CachePolicy cachePolicy) {
        this.domainCacheId = domainCacheId;
        this.masterCacheId = masterCacheId;
        this.cachePolicy = cachePolicy;
    }

    @Override
    public MasterCacheId getMasterCacheId() {
        return masterCacheId;
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
        return cacheAccess(cacheHandle);
    }

    @Override
    public <K1, T> RxCache<K1, T> cache(Class<T> from) {
        return cacheAccess(CacheHandle.create(domainCacheId, masterCacheId, from));
    }

    @Override
    public <V> List<V> computeListIfEmpty(Class<V> cachedType, long msecs) {
        return this.<Object, V>cacheAccess(CacheHandle.create(domainCacheId, masterCacheId, cachedType)).readAsList();
    }

    @Override
    public <V> List<V> computeListIfEmpty(CacheHandle cacheHandle, long msecs) {
        return this.<Object, V>cacheAccess(cacheHandle).readAsList();
    }

    @Override
    public <K, V> Map<K, V> computeIfEmpty(Class<V> cachedType, long msecs) {
        return this.<K, V>cacheAccess(CacheHandle.create(domainCacheId, masterCacheId, cachedType)).readAll();
    }

    @Override
    public <K, V> Map<K, V> computeIfEmpty(CacheHandle cacheHandle, long msecs) {
        return this.<K, V>cacheAccess(cacheHandle).readAll();
    }

    @Override
    public <K, V> Map<K, V> computeIfAbsent(CacheHandle cacheHandle, Iterable<K> keys, long msecs) {
        return this.<K, V>cacheAccess(cacheHandle).read(keys);
    }

    @Override
    public <K, V> Map<K, V> computeIfAbsent(Class<V> cachedType, Iterable<K> keys, long msecs) {
        return this.<K, V>cacheAccess(CacheHandle.create(domainCacheId, masterCacheId, cachedType)).read(keys);
    }

    @Override
    public <K, V> Map<K, V> computeIf(CacheHandle cacheHandle, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return acceptCachedOrCompute(cacheHandle, keys, computeResolver);
    }

    @Override
    public <K, V> Map<K, V> computeIf(Class<V> cachedType, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return acceptCachedOrCompute(CacheHandle.create(domainCacheId, masterCacheId, cachedType), keys, computeResolver);
    }

    @Override
    public <K, V> Optional<V> computeIfAbsent(Class<V> cachedType, K key, long msecs) {
        return this.<K, V>cacheAccess(CacheHandle.create(domainCacheId, masterCacheId, cachedType)).read(key);
    }

    @Override
    public <K, V> Optional<V> computeIfAbsent(CacheHandle cacheHandle, K key, long msecs) {
        return this.<K, V>cacheAccess(cacheHandle).read(key);
    }

    @Override
    public <K, V> Optional<V> computeIf(Class<V> cachedType, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return acceptCachedOrCompute(CacheHandle.create(domainCacheId, masterCacheId, cachedType), key, computeResolver);
    }

    @Override
    public <K, V> Optional<V> computeIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return acceptCachedOrCompute(cacheHandle, key, computeResolver);
    }

    @Override
    public <K, V> V computeValueIfAbsent(CacheHandle cacheHandle, K key, long msecs) {
        return this.<K, V>acceptCachedOrCompute(cacheHandle, key, Objects::isNull).orElseThrow(() -> new ExecutionEndedWithNoResultException("No result for " + key));
    }

    @Override
    public <K, V> V computeValueIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return acceptCachedOrCompute(cacheHandle, key, computeResolver).orElseThrow(() -> new ExecutionEndedWithNoResultException("No result for " + key));
    }

    @Override
    public <V> List<V> computeList(Class<V> cachedType, long msecs) {
        return this.<Object, V>cacheAccess(CacheHandle.create(domainCacheId, masterCacheId, cachedType)).readAsList();
    }

    @Override
    public <V> List<V> computeList(CacheHandle cacheHandle, long msecs) {
        return this.<Object, V>cacheAccess(cacheHandle).readAsList();
    }

    @Override
    public <K, V> Map<K, V> compute(Class<V> cachedType, long msecs) {
        return this.<K, V>cacheAccess(CacheHandle.create(domainCacheId, masterCacheId, cachedType)).readAll();
    }

    @Override
    public <K, V> Map<K, V> compute(CacheHandle cacheHandle, long msecs) {
        return this.<K, V>cacheAccess(cacheHandle).readAll();
    }

    @Override
    public <K, V> Map<K, V> compute(CacheHandle cacheHandle, Iterable<K> keys, long msecs) {
        return this.<K, V>cacheAccess(cacheHandle).read(keys);
    }

    @Override
    public <K, V> Map<K, V> compute(Class<V> cachedType, Iterable<K> keys, long msecs) {
        return this.<K, V>cacheAccess(CacheHandle.create(domainCacheId, masterCacheId, cachedType)).read(keys);
    }

    @Override
    public <K, V> Optional<V> compute(Class<V> cachedType, K key, long msecs) {
        return this.<K, V>cacheAccess(CacheHandle.create(domainCacheId, masterCacheId, cachedType)).read(key);
    }

    @Override
    public <K, V> Optional<V> compute(CacheHandle cacheHandle, K key, long msecs) {
        return this.<K, V>cacheAccess(cacheHandle).read(key);
    }

    @Override
    public <K, V> V computeValue(CacheHandle cacheHandle, K key, long msecs) {
        return this.<K, V>acceptCachedOrCompute(cacheHandle, key, Objects::isNull).orElseThrow(() -> new ExecutionEndedWithNoResultException("No result for " + key));
    }

    @Override
    public RxStreamer clearCache() {
        RxCacheAccess.find(domainCacheId).ifPresent(CacheFactory::clearAllCaches);
        return this;
    }

    @Override
    public <V> RxStreamer clearCache(Class<V> cachedType) {
        RxCacheAccess.find(domainCacheId).ifPresent(cacheFactory -> cacheFactory.expire(masterCacheId, cachedType));
        return this;
    }

    @Override
    public RxStreamer clearCache(MasterCacheId masterCacheId) {
        RxCacheAccess.find(domainCacheId).ifPresent(cacheFactory -> cacheFactory.expire(masterCacheId));
        return this;
    }

    private <K, V> Optional<V> acceptCachedOrCompute(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver) {
        Optional<V> value = this.<K, V>cacheAccess(cacheHandle).read(key);
        if (!value.isPresent()) {
            return value;
        }

        return !computeResolver.perform(value.get())
                ? value
                : Optional.empty();
    }

    private <K, V> Map<K, V> acceptCachedOrCompute(CacheHandle cacheHandle, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver) {
        Map<K, V> values = this.<K, V>cacheAccess(cacheHandle).read(keys);
        if (values.isEmpty()) {
            return values;
        }

        return !computeResolver.perform(values)
                ? values
                : Collections.emptyMap();
    }

    private <K, V> RxCache<K, V> cacheAccess(CacheHandle cacheHandle) {
        return RxCacheAccess.cache(cacheHandle, cachePolicy);
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
