package com.intact.rx.core.rxrepo.writer;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxContext;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.*;
import com.intact.rx.api.rxrepo.RxRepositoryWriter;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.rxcache.Streamer;
import com.intact.rx.core.rxcache.StreamerBuilder;
import com.intact.rx.core.rxcache.controller.ActsControllerConfig;
import com.intact.rx.core.rxcache.controller.ActsControllerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;

public class RepositoryStreamWriter<K, V> implements RxRepositoryWriter<K, V>, RxObserver<Map<K, V>> {
    private final CacheHandle cacheHandle;
    private final RxContext rxContext;
    private final RxConfig rxConfig;
    private final Strategy2<Boolean, K, V> pusher;

    // Local state
    private final AtomicReference<Streamer> lastStreamer;
    private final RxSubjectCombi<Map<K, V>> subject;

    public RepositoryStreamWriter(CacheHandle cacheHandle, RxContext rxContext, RxConfig rxConfig, Strategy2<Boolean, K, V> pusher) {
        this.cacheHandle = cacheHandle;
        this.rxConfig = rxConfig;
        this.pusher = pusher;
        this.rxContext = rxContext;

        this.lastStreamer = new AtomicReference<>(null);
        this.subject = new RxSubjectCombi<>();
    }

    @Override
    public RxCache<K, V> cache() {
        return RxCacheAccess.cache(cacheHandle, rxConfig.actPolicy.getCachePolicy());
    }

    @Override
    public boolean put(K key, V value, long msecs) {
        return computeAndGetPrivate(key, () -> value, msecs).isPresent();
    }

    @Override
    public boolean putAndRemoveLocal(K key, V value, long msecs) {
        return putPusherAndGetPrivate(
                key,
                () -> {
                    if (pusher.perform(key, value)) {
                        cache().take(key);
                    }
                    return Collections.emptyMap();
                },
                msecs).isPresent();
    }

    @Override
    public Optional<V> putAndGet(K key, V value, long msecs) {
        return computeAndGetPrivate(key, () -> value, msecs);
    }

    @Override
    public Optional<V> preComputeAndPut(K key, V value, final Strategy3<V, K, V, Optional<V>> preComputer, long msecs) {
        Strategy0<V> computer = () -> {
            Optional<V> cachedValue = cache().read(key);
            return preComputer.perform(key, value, cachedValue);
        };

        return computeAndGetPrivate(key, computer, msecs);
    }

    @Override
    public Optional<V> putAndPostCompute(K key, V value, final Strategy4<V, Boolean, K, V, Optional<V>> postComputer, long msecs) {
        Strategy0<Map<K, V>> cacher = () -> {
            Boolean isAccepted = pusher.perform(key, value);
            Optional<V> cachedValue = cache().read(key);
            V postComputedValue = postComputer.perform(isAccepted, key, value, cachedValue);
            return Map.of(key, postComputedValue);
        };

        return putPusherAndGetPrivate(key, cacher, msecs);
    }

    @Override
    public boolean compute(K key, Strategy0<V> computer, long msecs) {
        return computeAndGetPrivate(key, computer, msecs).isPresent();
    }

    @Override
    public boolean putIfAbsent(K key, V value, long msecs) {
        Optional<V> previous = cache().read(key);

        boolean absent = !previous.isPresent();

        return absent && computeAndGetPrivate(key, () -> value, msecs).isPresent();
    }

    @Override
    public boolean putIfPresent(K key, V value, long msecs) {
        Optional<V> previous = cache().read(key);

        boolean present = previous.isPresent();

        return present && computeAndGetPrivate(key, () -> value, msecs).isPresent();
    }

    @Override
    public boolean putIfNotEqual(K key, V value, long msecs) {
        Optional<V> previous = cache().read(key);

        boolean notEqual = !previous.isPresent() || !Objects.equals(previous.get(), value);

        return notEqual && computeAndGetPrivate(key, () -> value, msecs).isPresent();
    }

    @Override
    public boolean putIf(Strategy2<Boolean, K, V> criterion, K key, V value, long msecs) {
        return criterion.perform(key, value) && computeAndGetPrivate(key, () -> value, msecs).isPresent();
    }

    @Override
    public RepositoryRequestStatus lastRequestStatus() {
        Streamer streamer = lastStreamer.get();
        return streamer != null
                ? RepositoryRequestStatus.create(streamer.getStatus())
                : RepositoryRequestStatus.no();
    }

    // -----------------------------------------------------------
    // Callback functions
    // -----------------------------------------------------------

    @Override
    public RxRepositoryWriter<K, V> connect(RxObserver<Map<K, V>> observer) {
        subject.connect(observer);
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> disconnect(RxObserver<Map<K, V>> observer) {
        subject.disconnect(observer);
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> disconnectAll() {
        subject.disconnectAll();
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> onCompleteDo(VoidStrategy0 completedFunction) {
        subject.onCompleteDo(completedFunction);
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        subject.onErrorDo(errorFunction);
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> onNextDo(VoidStrategy1<Map<K, V>> nextFunction) {
        subject.onNextDo(nextFunction);
        return this;
    }

    @Override
    public RxRepositoryWriter<K, V> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        subject.onSubscribeDo(subscribeFunction);
        return this;
    }

    // -----------------------------------------------------------
    // RxObserver<Map<K, V>>
    // -----------------------------------------------------------

    @Override
    public void onComplete() {
        subject.onComplete();
    }

    @Override
    public void onError(Throwable throwable) {
        subject.onError(throwable);
    }

    @Override
    public void onNext(Map<K, V> value) {
        subject.onNext(value);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subject.onSubscribe(subscription);
    }

    // -----------------------------------------------------------
    // private implementation
    // -----------------------------------------------------------

    private Optional<V> computeAndGetPrivate(K key, Strategy0<V> computer, long msecs) {
        if (key == null || computer == null) {
            return Optional.empty();
        }

        Streamer streamer =
                createStreamBuilder()
                        .sequential()
                        .set(cacheHandle, key, computer, pusher)
                        .done();

        lastStreamer.set(streamer);

        boolean success = streamer.subscribe()
                .waitFor(msecs)
                .isSuccess();

        return success ?
                streamer.computeIfAbsent(cacheHandle, key, 1)
                : Optional.empty();
    }

    private Optional<V> putPusherAndGetPrivate(K key, Strategy0<Map<K, V>> pusher, long msecs) {
        if (key == null || pusher == null) {
            return Optional.empty();
        }

        Streamer streamer =
                createStreamBuilder()
                        .sequential()
                        .get(cacheHandle, pusher)
                        .done();

        lastStreamer.set(streamer);

        boolean success = streamer.subscribe()
                .waitFor(msecs)
                .isSuccess();

        return success ?
                streamer.computeIfAbsent(cacheHandle, key, 1)
                : Optional.empty();
    }

    private StreamerBuilder createStreamBuilder() {
        return Streamer
                .forCache(cacheHandle.getMasterCacheId())
                .withExecuteAround(rxContext)
                .withThreadPoolConfig(rxConfig.rxThreadPoolConfig)
                .withControllerConfig(
                        ActsControllerConfig.builder()
                                .withActsControllerPolicy(ActsControllerPolicy.from(rxConfig.actsControllerConfig.getActsControllerPolicy(), CircuitBreakerPolicy.unlimited, RateLimiterPolicy.unlimited))
                                .withCircuitBreakerId(CircuitId.none())
                                .withRateLimiterId(RateLimiterId.none())
                                .build())
                .withActPolicy(rxConfig.actPolicy)
                .withCommandConfig(rxConfig.commandConfig)
                .withFaultPolicy(rxConfig.faultPolicy)
                .withDomainCacheId(rxConfig.domainCacheId)
                .build()
                .onSubscribeDo(this::onSubscribe)
                .onNextDo(this::onNext, CacheHandle.<V>findCachedType(cacheHandle).orElseThrow(() -> new IllegalStateException("CacheHandle without class information")))
                .onErrorDo(this::onError)
                .onCompleteDo(this::onComplete);
    }
}
