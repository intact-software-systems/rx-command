package com.intact.rx.core.rxcache;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.*;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxcache.RxStreamerStatus;
import com.intact.rx.api.subject.RxLambdaSubject;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.core.rxcache.acts.ActGroupChain;
import com.intact.rx.core.rxcache.api.Act;
import com.intact.rx.core.rxcache.api.ActsController;
import com.intact.rx.core.rxcache.controller.ActsCommandsController;
import com.intact.rx.core.rxcache.controller.ActsControllerConfig;
import com.intact.rx.core.rxcache.factory.RxCacheFactory;
import com.intact.rx.core.rxcache.noop.ActsControllerNoOp;
import com.intact.rx.templates.AtomicSupplierWithInitial;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.api.Context;

import static com.intact.rx.core.rxcache.StreamerAlgorithms.*;

public class Streamer implements RxStreamer {
    private static final Async emptyAsync = new Async(ActsControllerNoOp.instance, ActGroupChain.create(), actGroup -> new StreamerGroup(MasterCacheId.uuid(), RxConfig.fromRxDefault(), actGroup, forUUIDCache().build()));

    private final Context<RxConfig, State> context;

    private Streamer(MasterCacheId masterCacheId, RxConfig rxConfig) {
        //noinspection ThisEscapedInObjectConstruction
        this.context = new ContextObject<>(
                rxConfig,
                new State(
                        masterCacheId,
                        this,
                        rxConfig.actsControllerConfig,
                        () -> {
                            ActGroupChain asyncChain = ActGroupChain.create();
                            ActsCommandsController actsController = RxCacheFactory.createActsController(rxConfig.actsControllerConfig.getActsControllerPolicy(), asyncChain, rxConfig.actsControllerConfig.getCircuitBreakerId(), rxConfig.actsControllerConfig.getRateLimiterId(), Optional.empty());
                            Async async = new Async(actsController, asyncChain, actGroup -> new StreamerGroup(masterCacheId, rxConfig, actGroup, new StreamerBuilder(this)));
                            actsController.connect(async);
                            return async;
                        }
                )
        );
    }

    // --------------------------------------------------
    // State class
    // --------------------------------------------------

    @SuppressWarnings("PackageVisibleField")
    static final class State {
        final ActsController controller;
        final MasterCacheId masterCacheId;
        final StreamerStatus streamerStatus;
        final RxLambdaSubject<ActGroup> subject;
        final ActGroupChain chain;
        final AtomicSupplierWithInitial<Async> async;

        State(MasterCacheId masterCacheId, RxObserver<ActGroup> observer, ActsControllerConfig actsControllerConfig, Supplier<Async> asyncSupplier) {
            this.masterCacheId = requireNonNull(masterCacheId);
            this.streamerStatus = new StreamerStatus();
            this.subject = new RxLambdaSubject<>();
            this.chain = ActGroupChain.create();
            this.controller = RxCacheFactory.createActsController(actsControllerConfig.getActsControllerPolicy(), this.chain, actsControllerConfig.getCircuitBreakerId(), actsControllerConfig.getRateLimiterId(), Optional.of(observer));
            this.async = new AtomicSupplierWithInitial<>(requireNonNull(emptyAsync), requireNonNull(asyncSupplier));
        }
    }

    @SuppressWarnings("PackageVisibleField")
    static final class Async implements RxObserver<ActGroup> {
        final ActsController controller;
        final ActGroupChain asyncChain;
        final Strategy1<StreamerGroup, ActGroup> groupFactory;
        final AtomicReference<StreamerGroup> currentGroup;
        final StreamerStatus streamerStatus;

        private Async(ActsController controller, ActGroupChain asyncChain, Strategy1<StreamerGroup, ActGroup> groupFactory) {
            this.controller = requireNonNull(controller);
            this.asyncChain = requireNonNull(asyncChain);
            this.groupFactory = requireNonNull(groupFactory);
            this.streamerStatus = new StreamerStatus();
            this.currentGroup = new AtomicReference<>(null);
        }

        StreamerGroup asyncGroup() {
            ActGroup asyncGroup = ActGroup.parallel();
            asyncChain.add(asyncGroup);

            StreamerGroup streamerGroup = groupFactory.perform(asyncGroup);
            currentGroup.set(streamerGroup);
            return streamerGroup;
        }

        @Override
        public void onComplete() {
            streamerStatus.onComplete();
        }

        @Override
        public void onError(Throwable throwable) {
            streamerStatus.onError(throwable);
        }

        @Override
        public void onNext(ActGroup value) {
            streamerStatus.onNext(value);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            streamerStatus.onSubscribe(subscription);
        }
    }

    // --------------------------------------------
    // Builder with fluent API
    // --------------------------------------------

    public static Builder forCache(MasterCacheId masterCacheId) {
        return new Builder(masterCacheId);
    }

    public static Builder forUUIDCache() {
        return new Builder(MasterCacheId.uuid());
    }

    @SuppressWarnings("WeakerAccess")
    public static class Builder extends RxConfigBuilder<Builder> {
        private final MasterCacheId masterCacheId;
        private final State state;

        Builder(MasterCacheId masterCacheId) {
            this.masterCacheId = requireNonNull(masterCacheId);
            //noinspection ThisEscapedInObjectConstruction
            this.state = new State(this);
        }

        public StreamerBuilder build() {
            return new StreamerBuilder(new Streamer(masterCacheId, buildRxConfig()));
        }

        @Override
        protected State state() {
            return state;
        }
    }

    // --------------------------------------------------
    // Getters
    // --------------------------------------------------

    @Override
    public MasterCacheId getMasterCacheId() {
        return state().masterCacheId;
    }

    @Override
    public RxConfig getRxConfig() {
        return config();
    }

    // --------------------------------------------------
    // Fluent API
    // --------------------------------------------------

    @Override
    public Streamer subscribe() {
        computeSyncPoint(new StreamerBuilder(this), getRxConfig().commandConfig.getCommandPolicy().getTimeout().toMillis());
        asyncController().subscribe();
        controller().subscribe();
        return this;
    }

    @Override
    public boolean unsubscribe() {
        asyncController().unsubscribe();
        return controller().unsubscribe();
    }

    @Override
    public boolean isSubscribed() {
        return controller().isSubscribed();
    }

    @Override
    public void cancel() {
        asyncController().cancel();
        controller().cancel();
    }

    @Override
    public boolean isSuccess() {
        return controller().isSuccess();
    }

    @Override
    public boolean isCancelled() {
        return controller().isCancelled();
    }

    @Override
    public Streamer waitFor(long msecs) {
        controller().waitFor(msecs);
        return this;
    }

    @Override
    public FutureStatus waitForResult(long msecs) {
        return controller().waitFor(msecs);
    }

    @Override
    public RxStreamerStatus getStatus() {
        return state().streamerStatus;
    }

    @Override
    public RxStreamerStatus getAsyncStatus() {
        return state().async.immutableGet().streamerStatus;
    }

    @Override
    public ActsController controller() {
        return state().controller;
    }

    // --------------------------------------------------
    // RxCache access
    // --------------------------------------------------

    @Override
    public <K1, T> RxCache<K1, T> cache(CacheHandle cacheHandle) {
        return cacheAccess(cacheHandle);
    }

    @Override
    public <K1, T> RxCache<K1, T> cache(Class<T> from) {
        return cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, from));
    }

    @Override
    public RxStreamer clearCache() {
        RxCacheAccess.clearCache(config().domainCacheId, state().masterCacheId);
        return this;
    }

    @Override
    public <V> RxStreamer clearCache(Class<V> cachedType) {
        RxCacheAccess.find(config().domainCacheId)
                .ifPresent(
                        cacheFactory -> Optional.ofNullable(cacheFactory.findCacheMaster(state().masterCacheId))
                                .ifPresent(
                                        cacheMaster -> {
                                            DataCache<Object, V> cache = cacheMaster.findCacheByType(cachedType);
                                            if (cache != null) {
                                                cache.clear();
                                            }
                                        }
                                )
                );
        return this;
    }

    @Override
    public RxStreamer clearCache(MasterCacheId masterCacheId) {
        RxCacheAccess.find(config().domainCacheId).ifPresent(cacheFactory -> cacheFactory.expire(masterCacheId));
        return this;
    }

    // --------------------------------------------------
    // Functions that start execution depending on cache state
    // --------------------------------------------------

    @Override
    public <V> List<V> computeListIfEmpty(Class<V> cachedType, long msecs) {
        return computeListIfEmptyAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, msecs);
    }

    @Override
    public <V> List<V> computeListIfEmpty(CacheHandle cacheHandle, long msecs) {
        return computeListIfEmptyAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, msecs);
    }

    @Override
    public <K, V> Map<K, V> computeIfEmpty(Class<V> cachedType, long msecs) {
        return computeIfEmptyAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, msecs);
    }

    @Override
    public <K, V> Map<K, V> computeIfEmpty(CacheHandle cacheHandle, long msecs) {
        return computeIfEmptyAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, msecs);
    }

    @Override
    public <K, V> Map<K, V> computeIfAbsent(CacheHandle cacheHandle, Iterable<K> keys, long msecs) {
        return StreamerAlgorithms.computeIfAbsentAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, kvMap -> {}, keys, msecs);
    }

    @Override
    public <K, V> Map<K, V> computeIfAbsent(Class<V> cachedType, Iterable<K> keys, long msecs) {
        return StreamerAlgorithms.computeIfAbsentAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, kvMap -> {}, keys, msecs);
    }

    @Override
    public <K, V> Map<K, V> computeIf(CacheHandle cacheHandle, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return StreamerAlgorithms.<K, V>computeIfAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, kvMap -> {}, keys, computeResolver, msecs);
    }

    @Override
    public <K, V> Map<K, V> computeIf(Class<V> cachedType, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return StreamerAlgorithms.<K, V>computeIfAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, kvMap -> {}, keys, computeResolver, msecs);
    }

    @Override
    public <K, V> Optional<V> computeIfAbsent(Class<V> cachedType, K key, long msecs) {
        return computeIfAbsentAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, v -> {}, key, Objects::nonNull, msecs);
    }

    @Override
    public <K, V> Optional<V> computeIfAbsent(CacheHandle cacheHandle, K key, long msecs) {
        return computeIfAbsentAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, v -> {}, key, Objects::nonNull, msecs);
    }

    @Override
    public <K, V> Optional<V> computeIf(Class<V> cachedType, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return computeIfAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, v -> {}, key, computeResolver, msecs);
    }

    @Override
    public <K, V> Optional<V> computeIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return computeIfAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, v -> {}, key, computeResolver, msecs);
    }

    @Override
    public <K, V> V computeValueIfAbsent(CacheHandle cacheHandle, K key, long msecs) {
        return computeValueIfAbsentAlgorithm(() -> this, cacheAccess(cacheHandle), v -> {}, key, Objects::nonNull, msecs);
    }

    @Override
    public <K, V> V computeValueIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return computeValueIfAlgorithm(() -> this, cacheAccess(cacheHandle), v -> {}, key, computeResolver, msecs);
    }

    // --------------------------------------------------
    // Functions that compute regardless
    // --------------------------------------------------

    @Override
    public <V> List<V> computeList(Class<V> cachedType, long msecs) {
        return computeListAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, msecs);
    }

    @Override
    public <V> List<V> computeList(CacheHandle cacheHandle, long msecs) {
        return computeListAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, msecs);
    }

    @Override
    public <K, V> Map<K, V> compute(Class<V> cachedType, long msecs) {
        return computeAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, msecs);
    }

    @Override
    public <K, V> Map<K, V> compute(CacheHandle cacheHandle, long msecs) {
        return computeAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, msecs);
    }

    @Override
    public <K, V> Map<K, V> compute(CacheHandle cacheHandle, Iterable<K> keys, long msecs) {
        return StreamerAlgorithms.<K, V>computeAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, kvMap -> {}, keys, msecs);
    }

    @Override
    public <K, V> Map<K, V> compute(Class<V> cachedType, Iterable<K> keys, long msecs) {
        return StreamerAlgorithms.<K, V>computeAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, kvMap -> {}, keys, msecs);
    }

    @Override
    public <K, V> Optional<V> compute(Class<V> cachedType, K key, long msecs) {
        return computeAlgorithm(() -> this, cacheAccess(CacheHandle.create(config().domainCacheId, state().masterCacheId, cachedType)), this.config().faultPolicy, v -> {}, key, msecs);
    }

    @Override
    public <K, V> Optional<V> compute(CacheHandle cacheHandle, K key, long msecs) {
        return computeAlgorithm(() -> this, cacheAccess(cacheHandle), this.config().faultPolicy, v -> {}, key, msecs);
    }

    @Override
    public <K, V> V computeValue(CacheHandle cacheHandle, K key, long msecs) {
        return computeValueAlgorithm(() -> this, cacheAccess(cacheHandle), v -> {}, key, msecs);
    }

    // --------------------------------------------
    // Setup callbacks when execution finishes
    // --------------------------------------------

    @Override
    public <K, V> Streamer onNextDo(VoidStrategy1<Map<K, V>> observer, Class<V> cachedType) {
        return this.<K, V>performOnAct(cachedType, act -> act.onNextDo(observer));
    }

    @Override
    public <K, V> Streamer connect(RxObserver<Map<K, V>> subject, Class<V> cachedType) {
        return this.<K, V>performOnAct(cachedType, act -> act.connect(subject));
    }

    // --------------------------------------------
    // Setup callbacks when execution finishes
    // --------------------------------------------

    @Override
    public Streamer onCompleteDo(VoidStrategy0 completedFunction) {
        state().subject.onCompleteDo(completedFunction);
        return this;
    }

    @Override
    public Streamer onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        state().subject.onErrorDo(errorFunction);
        return this;
    }

    @Override
    public Streamer onNextDo(VoidStrategy1<ActGroup> nextFunction) {
        state().subject.onNextDo(nextFunction);
        return this;
    }

    @Override
    public Streamer onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        state().subject.onSubscribeDo(subscribeFunction);
        return this;
    }

    // --------------------------------------------
    // Interface RxObserver
    // --------------------------------------------

    @Override
    public void onComplete() {
        state().streamerStatus.onComplete();
        state().subject.onComplete();
    }

    @Override
    public void onError(Throwable throwable) {
        state().streamerStatus.setExceptions(state().controller.getExceptions());
        state().streamerStatus.onError(throwable);
        state().subject.onError(throwable);
    }

    @Override
    public void onNext(ActGroup value) {
        state().streamerStatus.onNext(value);
        state().subject.onNext(value);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        state().streamerStatus.onSubscribe(subscription);
        state().subject.onSubscribe(subscription);
    }

    // --------------------------------------------------
    // Private functions
    // --------------------------------------------------

    ActsController asyncController() {
        return state().async.immutableGet().controller;
    }

    private <K, V> Streamer performOnAct(Class<V> cachedType, VoidStrategy1<Act<K, V>> strategy) {
        state().chain.stream()
                .flatMap(actGroup -> actGroup.getList().stream())
                .filter(act -> act.getCacheHandle().getDataCacheId().getId().getClazz().map(aClass -> Objects.equals(aClass, cachedType)).orElse(false))
                .forEach(act -> {
                    //noinspection unchecked
                    Optional.of((Act<K, V>) act).ifPresent(strategy::perform);
                });
        return this;
    }

    private <K, V> RxCache<K, V> cacheAccess(CacheHandle cacheHandle) {
        return RxCacheAccess.cache(cacheHandle, config().actPolicy.getCachePolicy());
    }

    State state() {
        return context.state();
    }

    private RxConfig config() {
        return context.config();
    }
}
