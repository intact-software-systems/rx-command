package com.intact.rx.api.rxcache;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.core.rxcache.api.ActsController;
import com.intact.rx.templates.api.RxObserverSupport;

public interface RxStreamer extends RxObserver<ActGroup>, RxObserverSupport<RxStreamer, ActGroup> {
    MasterCacheId getMasterCacheId();

    RxConfig getRxConfig();

    RxStreamer subscribe();

    boolean unsubscribe();

    boolean isSubscribed();

    void cancel();

    boolean isSuccess();

    boolean isCancelled();

    RxStreamer waitFor(long msecs);

    FutureStatus waitForResult(long msecs);

    RxStreamerStatus getStatus();

    RxStreamerStatus getAsyncStatus();

    ActsController controller();

    <K, V> RxStreamer onNextDo(VoidStrategy1<Map<K, V>> observer, Class<V> cachedType);

    <K, V> RxStreamer connect(RxObserver<Map<K, V>> subject, Class<V> cachedType);

    <K1, T> RxCache<K1, T> cache(CacheHandle cacheHandle);

    <K1, T> RxCache<K1, T> cache(Class<T> from);

    <V> List<V> computeListIfEmpty(Class<V> cachedType, long msecs);

    <V> List<V> computeListIfEmpty(CacheHandle cacheHandle, long msecs);

    <K, V> Map<K, V> computeIfEmpty(Class<V> cachedType, long msecs);

    <K, V> Map<K, V> computeIfEmpty(CacheHandle cacheHandle, long msecs);

    <K, V> Map<K, V> computeIfAbsent(CacheHandle cacheHandle, Iterable<K> keys, long msecs);

    <K, V> Map<K, V> computeIfAbsent(Class<V> cachedType, Iterable<K> keys, long msecs);

    <K, V> Map<K, V> computeIf(CacheHandle cacheHandle, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs);

    <K, V> Map<K, V> computeIf(Class<V> cachedType, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs);

    <K, V> Optional<V> computeIfAbsent(Class<V> cachedType, K key, long msecs);

    <K, V> Optional<V> computeIfAbsent(CacheHandle cacheHandle, K key, long msecs);

    <K, V> Optional<V> computeIf(Class<V> cachedType, K key, Strategy1<Boolean, V> computeResolver, long msecs);

    <K, V> Optional<V> computeIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs);

    <K, V> V computeValueIfAbsent(CacheHandle cacheHandle, K key, long msecs);

    <K, V> V computeValueIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs);

    <V> List<V> computeList(Class<V> cachedType, long msecs);

    <V> List<V> computeList(CacheHandle cacheHandle, long msecs);

    <K, V> Map<K, V> compute(Class<V> cachedType, long msecs);

    <K, V> Map<K, V> compute(CacheHandle cacheHandle, long msecs);

    <K, V> Map<K, V> compute(CacheHandle cacheHandle, Iterable<K> keys, long msecs);

    <K, V> Map<K, V> compute(Class<V> cachedType, Iterable<K> keys, long msecs);

    <K, V> Optional<V> compute(Class<V> cachedType, K key, long msecs);

    <K, V> Optional<V> compute(CacheHandle cacheHandle, K key, long msecs);

    <K, V> V computeValue(CacheHandle cacheHandle, K key, long msecs);

    RxStreamer clearCache();

    <V> RxStreamer clearCache(Class<V> cachedType);

    RxStreamer clearCache(MasterCacheId masterCacheId);
}
