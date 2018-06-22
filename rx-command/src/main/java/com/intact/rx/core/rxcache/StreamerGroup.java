package com.intact.rx.core.rxcache;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.*;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.action.ContextStrategyAction0;
import com.intact.rx.core.command.action.FallbackAction;
import com.intact.rx.core.command.action.FallbackStrategyAction;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.core.rxcache.api.Act;
import com.intact.rx.core.rxcache.factory.GetComposer;
import com.intact.rx.core.rxcache.factory.RunComposer;
import com.intact.rx.core.rxcache.factory.ValueComposer;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.policy.Computation;

@SuppressWarnings("unused")
public class StreamerGroup {
    private final MasterCacheId masterCacheId;
    private final RxConfig rxConfig;
    private final ActGroup group;
    private final StreamerBuilder builder;

    public StreamerGroup(MasterCacheId masterCacheId, RxConfig rxConfig, ActGroup actGroup, StreamerBuilder streamerBuilder) {
        this.masterCacheId = requireNonNull(masterCacheId);
        this.rxConfig = requireNonNull(rxConfig);
        this.group = requireNonNull(actGroup);
        this.builder = requireNonNull(streamerBuilder);
    }

    public MasterCacheId getMasterCacheId() {
        return masterCacheId;
    }

    public RxConfig getRxConfig() {
        return rxConfig;
    }

    // --------------------------------------------------
    // Convenience methods for fluent API
    // --------------------------------------------------

    public Streamer parent() {
        return builder.streamer();
    }

//    public StreamerBuilder builder() {
//        return builder;
//    }

    public Streamer subscribe() {
        return builder.streamer().subscribe();
    }

    public StreamerGroup parallel() {
        return builder.parallel();
    }

    public StreamerGroup sequential() {
        return builder.sequential();
    }

    public StreamerGroup async() {
        return builder.async();
    }

    public StreamerBuilder syncpoint(long msecs) {
        return builder.syncpoint(msecs);
    }

    public StreamerBuilder syncpoint() {
        return builder.syncpoint();
    }

    public Streamer done() {
        return builder.streamer();
    }

    // --------------------------------------------------
    // Compose actors
    // --------------------------------------------------

    public RunComposer composeRun() {
        return new RunComposer(this, builder.streamer(), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy());
    }

    public <V> ValueComposer<V> composeValues(Class<V> clazz) {
        return new ValueComposer<>(this, builder.streamer(), CacheHandle.create(rxConfig.domainCacheId, masterCacheId, clazz), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy());
    }

    public <K, V> GetComposer<K, V> compose(Class<V> clazz) {
        return new GetComposer<>(this, builder.streamer(), CacheHandle.create(rxConfig.domainCacheId, masterCacheId, clazz), rxConfig.commandConfig.getCommandPolicy(), rxConfig.actPolicy);
    }

    public <K, V> GetComposer<K, V> compose(MasterCacheId masterCacheId, Class<V> clazz) {
        return new GetComposer<>(this, builder.streamer(), CacheHandle.create(rxConfig.domainCacheId, masterCacheId, clazz), rxConfig.commandConfig.getCommandPolicy(), rxConfig.actPolicy);
    }

    // --------------------------------------------------
    // Setters that execute lambda and updates cache if successful
    // --------------------------------------------------

    public <K, V> StreamerGroup set(K key, V value, Strategy2<Boolean, K, V> pusher) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, value.getClass()), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), createCacher(key, value, pusher), Optional.empty());
    }

    public <K, V> StreamerGroup set(CacheHandle cacheHandle, K key, V value, Strategy2<Boolean, K, V> pusher) {
        return addActor(cacheHandle, rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), createCacher(key, value, pusher), Optional.empty());
    }

    public <K, V> StreamerGroup set(Class<V> clazz, K key, Strategy0<V> computer, Strategy2<Boolean, K, V> pusher) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, clazz), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), createCacher(key, computer, pusher), Optional.empty());
    }

    public <K, V> StreamerGroup set(CacheHandle cacheHandle, K key, Strategy0<V> computer, Strategy2<Boolean, K, V> pusher) {
        return addActor(cacheHandle, rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), createCacher(key, computer, pusher), Optional.empty());
    }

    // --------------------------------------------------
    // Getters with no input
    // --------------------------------------------------

    public <K, V> StreamerGroup get(Class<V> cachedType, Strategy0<Map<K, V>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), strategy, Optional.empty());
    }

    public <K, V> StreamerGroup get(Class<V> cachedType, Strategy0<Map<K, V>> strategy, RxObserver<Map<K, V>> rxObserver) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), strategy, Optional.of(rxObserver));
    }

    public <K, V> StreamerGroup get(CacheHandle cacheHandle, Strategy0<Map<K, V>> strategy) {
        return addActor(cacheHandle, rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), strategy, Optional.empty());
    }

    public <K, V> StreamerGroup get(CacheHandle cacheHandle, Strategy0<Map<K, V>> strategy, RxObserver<Map<K, V>> rxObserver) {
        return addActor(cacheHandle, rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), strategy, Optional.of(rxObserver));
    }

    // --------------------------------------------------
    // Actors with value as key
    // --------------------------------------------------

    public <V> StreamerGroup getValue(Class<V> cachedType, Strategy0<V> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> toMap(strategy.perform()), Optional.empty());
    }

    public <V> StreamerGroup getValues(Class<V> cachedType, Strategy0<Collection<V>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> toMap(strategy.perform()), Optional.empty());
    }

    // --------------------------------------------------
    // Mappers from, to same cached type
    // --------------------------------------------------

    public <K, V> StreamerGroup map(Class<V> cachedType, Strategy1<Map<K, V>, RxCache<K, V>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> strategy.perform(parent().cache(cachedType)), Optional.empty());
    }

    public <K, V> StreamerGroup map(Class<V> cachedType, K key, Strategy2<Map<K, V>, K, Optional<V>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, DataCacheId.class), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> strategy.perform(key, parent().<K, V>cache(cachedType).read(key)), Optional.empty());
    }

    public <K, V> StreamerGroup mapValues(Class<V> cachedType, Strategy1<Map<K, V>, Collection<V>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> strategy.perform(parent().cache(cachedType).readAsList()), Optional.empty());
    }

    public <K, V> StreamerGroup mapKeys(Class<V> cachedType, Strategy1<Map<K, V>, Set<K>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> strategy.perform(parent().<K, V>cache(cachedType).readAll().keySet()), Optional.empty());
    }

    // --------------------------------------------------
    // Mappers from type to cached type
    // --------------------------------------------------

    public <K, V, KA, A> StreamerGroup map(Class<V> cachedType, Class<A> fromA, Strategy1<Map<K, V>, RxCache<KA, A>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> strategy.perform(parent().cache(fromA)), Optional.empty());
    }

    public <K, V, KA, A> StreamerGroup map(Class<V> cachedType, Class<A> fromA, KA key, Strategy2<Map<K, V>, KA, Optional<A>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, DataCacheId.class), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> strategy.perform(key, parent().<KA, A>cache(fromA).read(key)), Optional.empty());
    }

    public <K, V, KA, A> StreamerGroup mapValues(Class<V> cachedType, Class<A> fromA, Strategy1<Map<K, V>, Collection<A>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> strategy.perform(parent().cache(fromA).readAsList()), Optional.empty());
    }

    public <K, V, KA, A> StreamerGroup mapKeys(Class<V> cachedType, Class<A> fromA, Strategy1<Map<K, V>, Set<KA>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> strategy.perform(parent().<KA, A>cache(fromA).readAll().keySet()), Optional.empty());
    }

    // -----------------------------------------------------------
    // Batch command: clear caches based on types
    // -----------------------------------------------------------

    public StreamerGroup clearCaches(Class<?>... cachedTypes) {
        for (Class<?> cachedType : cachedTypes) {
            addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType),
                    rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), () -> {
                        parent().cache(cachedType).clear();
                        return emptyMap();
                    },
                    Optional.empty()
            );
        }
        return this;
    }

    // -----------------------------------------------------------
    // Batch commands: Create getter for each input value
    // -----------------------------------------------------------

    public <K, V, KT, T> StreamerGroup getEachValue(Class<V> cachedType, Iterable<T> values, Strategy1<Map<K, V>, T> strategy) {
        values.forEach(v -> get(cachedType, () -> strategy.perform(v)));
        return this;
    }

    public <K, V, T> StreamerGroup mapEachValue(Class<V> cachedType, Iterable<T> values, Strategy2<Map<K, V>, RxCache<K, V>, T> strategy) {
        values.forEach(v -> get(cachedType, () -> strategy.perform(parent().cache(cachedType), v)));
        return this;
    }

    public <K, V, KA, A, T> StreamerGroup mapEachValue(Class<V> cachedType, Class<A> fromA, Iterable<T> values, Strategy2<Map<K, V>, RxCache<KA, A>, T> strategy) {
        values.forEach(v -> get(cachedType, () -> strategy.perform(parent().cache(fromA), v)));
        return this;
    }

    public <K, V, KA, A, KB, B, T> StreamerGroup mapEachValue(Class<V> cachedType, Class<A> fromA, Class<B> fromB, Iterable<T> values, Strategy3<Map<K, V>, RxCache<KA, A>, RxCache<KB, B>, T> strategy) {
        values.forEach(v -> get(cachedType, () -> strategy.perform(parent().cache(fromA), parent().cache(fromB), v)));
        return this;
    }

    // -----------------------------------------------------------
    // Batch commands: Act for each input key and optional cached value
    // -----------------------------------------------------------

    public <K, V> StreamerGroup mapEachKey(Class<V> cachedType, Iterable<K> keys, Strategy2<Map<K, V>, K, Optional<V>> strategy) {
        keys.forEach(key -> get(cachedType, () -> strategy.perform(key, parent().<K, V>cache(cachedType).read(key))));
        return this;
    }

    public <K, V, KA, A> StreamerGroup mapEachKey(Class<V> cachedType, Class<A> fromA, Iterable<KA> keysForA, Strategy2<Map<K, V>, KA, Optional<A>> strategy) {
        keysForA.forEach(keyA -> get(cachedType, () -> strategy.perform(keyA, parent().<KA, A>cache(fromA).read(keyA))));
        return this;
    }

    public <K, V> StreamerGroup mapEachKeyTake(Class<V> cachedType, Iterable<K> keys, Strategy2<Map<K, V>, K, Optional<V>> strategy) {
        keys.forEach(key -> get(cachedType, () -> strategy.perform(key, parent().<K, V>cache(cachedType).take(key))));
        return this;
    }

    public <K, V, KA, A> StreamerGroup mapEachKeyTake(Class<V> cachedType, Class<A> fromA, Iterable<KA> keysForA, Strategy2<Map<K, V>, KA, Optional<A>> strategy) {
        keysForA.forEach(keyA -> get(cachedType, () -> strategy.perform(keyA, parent().<KA, A>cache(fromA).take(keyA))));
        return this;
    }

    // -----------------------------------------------------------
    // Batch commands: act for each cached value
    // -----------------------------------------------------------

    public <K, V> StreamerGroup mapEach(Class<V> cachedType, Strategy1<Map<K, V>, Map.Entry<K, V>> strategy) {
        return addActorForEach(cachedType, cachedType, strategy, () -> parent().<K, V>cache(cachedType).readAll());
    }

    public <K, V, KA, A> StreamerGroup mapEach(Class<V> cachedType, Class<A> fromA, Strategy2<Map<K, V>, RxCache<K, V>, Map.Entry<KA, A>> strategy) {
        return addActorForEach(cachedType, fromA, strategy, () -> parent().<KA, A>cache(fromA).readAll());
    }

    public <K, V> StreamerGroup mapEachTake(Class<V> cachedType, Strategy1<Map<K, V>, Map.Entry<K, V>> strategy) {
        return addActorForEach(cachedType, cachedType, strategy, () -> parent().<K, V>cache(cachedType).takeAll());
    }

    public <K, V, KA, A> StreamerGroup mapEachTake(Class<V> cachedType, Class<A> fromA, Strategy2<Map<K, V>, RxCache<K, V>, Map.Entry<KA, A>> strategy) {
        return addActorForEach(cachedType, fromA, strategy, () -> parent().<KA, A>cache(fromA).takeAll());
    }

    // --------------------------------------------------
    // Runners with no return values
    // --------------------------------------------------

    public StreamerGroup run(VoidStrategy0 strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, DataCacheId.class), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), createVoidCommand(strategy), Optional.empty());
    }

    public <K, V> StreamerGroup runOn(Class<V> cachedType, VoidStrategy1<RxCache<K, V>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, DataCacheId.class), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), createVoidCommand(() -> strategy.perform(parent().cache(cachedType))), Optional.empty());
    }

    public <K, V> StreamerGroup runOn(Class<V> cachedType, K key, VoidStrategy2<K, Optional<V>> strategy) {
        return addActor(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, DataCacheId.class), rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), createVoidCommand(() -> strategy.perform(key, parent().<K, V>cache(cachedType).read(key))), Optional.empty());
    }

    // -----------------------------------------------------------
    // Batch commands: Act for each input values
    // -----------------------------------------------------------

    public <V> StreamerGroup runEachValue(Iterable<V> values, VoidStrategy1<V> strategy) {
        values.forEach(v -> run(() -> strategy.perform(v)));
        return this;
    }

    // -----------------------------------------------------------
    // Void batch commands: act for each cached value
    // -----------------------------------------------------------

    public <K, V> StreamerGroup runEach(Class<V> cachedType, VoidStrategy1<Map.Entry<K, V>> strategy) {
        return addActorForEach(
                cachedType,
                cachedType,
                (Map.Entry<K, V> a) -> {
                    strategy.perform(a);
                    return emptyMap();
                },
                () -> parent().<K, V>cache(cachedType).readAll()
        );
    }

    public <K, V, KA, A> StreamerGroup runEach(Class<V> cachedType, Class<A> fromA, VoidStrategy2<RxCache<K, V>, Map.Entry<KA, A>> strategy) {
        return addActorForEach(
                cachedType,
                fromA,
                (RxCache<K, V> cache, Map.Entry<KA, A> a) -> {
                    strategy.perform(cache, a);
                    return emptyMap();
                },
                () -> parent().<KA, A>cache(fromA).readAll()
        );
    }

    // --------------------------------------------------
    // protected
    // --------------------------------------------------

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public <K, V> StreamerGroup addActor(CacheHandle cacheHandle, CircuitId circuitBreakerId, RateLimiterId rateLimiterId, Strategy0<Map<K, V>> strategy, Optional<RxObserver<Map<K, V>>> rxObserver, ActPolicy actPolicy, CommandPolicy commandPolicy) {
        requireNonNull(cacheHandle);
        requireNonNull(strategy);
        requireNonNull(circuitBreakerId);
        requireNonNull(commandPolicy);
        requireNonNull(actPolicy);
        requireNonNull(rxObserver);

        Act<K, V> act = group.act(cacheHandle, circuitBreakerId, rateLimiterId, rxConfig.rxThreadPoolConfig, actPolicy, commandPolicy, new ContextStrategyAction0<>(rxConfig.rxContext, strategy));
        if (act != null) {
            rxObserver.map(act::connect);
        }
        return this;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public <K, V> StreamerGroup addActor(CacheHandle cacheHandle, CircuitId circuitBreakerId, RateLimiterId rateLimiterId, Strategy0<Map<K, V>> strategy, Strategy0<Map<K, V>> fallback, Optional<RxObserver<Map<K, V>>> rxObserver, Optional<RxObserver<Map<K, V>>> fallbackObserver, ActPolicy actPolicy, CommandPolicy commandPolicy) {
        requireNonNull(cacheHandle);
        requireNonNull(strategy);
        requireNonNull(circuitBreakerId);
        requireNonNull(commandPolicy);
        requireNonNull(actPolicy);
        requireNonNull(rxObserver);
        requireNonNull(fallbackObserver);

        Act<K, V> act = group.act(cacheHandle, circuitBreakerId, rateLimiterId, rxConfig.rxThreadPoolConfig, actPolicy, commandPolicy, new FallbackStrategyAction<>(rxConfig.rxContext, strategy, new FallbackAction<>(rxConfig.rxContext, fallback)));

        if (act != null) {
            rxObserver.map(act::connect);
            fallbackObserver.map(act::connect);
        }
        return this;
    }

    public static <V> Map<V, V> toMap(Collection<V> data) {
        if (data == null) {
            return null;
        }
        else if(data.isEmpty()) {
            return emptyMap();
        }

        Map<V, V> map = new HashMap<>(data.size());
        data.forEach(v -> map.put(v, v));
        return map;
    }

    private static <V> Map<V, V> toMap(V value) {
        return value != null
                ? Map.of(value, value)
                : null;
    }

    public static <K, V> Strategy0<Map<K, V>> createVoidCommand(VoidStrategy0 strategy) {
        return () -> {
            strategy.perform();
            return emptyMap();
        };
    }

    // --------------------------------------------------
    // private
    // --------------------------------------------------

    private <K, V, KA, A> StreamerGroup addActorForEach(Class<V> cachedType, Class<A> fromA, Strategy1<Map<K, V>, Map.Entry<KA, A>> strategy, Strategy0<Map<KA, A>> fromCache) {
        run(() -> {
                    StreamerBuilder builder = Streamer.forCache(parent().getMasterCacheId()).withRxConfig(parent().getRxConfig()).build();
                    StreamerGroup newGroup = Objects.equals(group.config().getComputation(), Computation.PARALLEL) ? builder.parallel() : builder.sequential();

                    Map<KA, A> objects = fromCache.perform();
                    objects.entrySet()
                            .forEach(entry -> newGroup.get(cachedType, () -> strategy.perform(entry)));
                    builder.streamer().subscribe()
                            .waitFor(parent().getRxConfig().commandConfig.getCommandPolicy().getTimeout().toMillis() * objects.size());
                }
        );
        return this;
    }

    private <K, V, KA, A> StreamerGroup addActorForEach(Class<V> cachedType, Class<A> fromA, Strategy2<Map<K, V>, RxCache<K, V>, Map.Entry<KA, A>> strategy, Strategy0<Map<KA, A>> fromCache) {
        run(() -> {
                    StreamerBuilder builder = Streamer.forCache(parent().getMasterCacheId()).withRxConfig(parent().getRxConfig()).build();
                    StreamerGroup newGroup = Objects.equals(group.config().getComputation(), Computation.PARALLEL) ? builder.parallel() : builder.sequential();

                    Map<KA, A> objects = fromCache.perform();
                    objects.entrySet()
                            .forEach(entry -> newGroup.get(cachedType, () -> strategy.perform(parent().cache(cachedType), entry)));
                    builder.streamer().subscribe()
                            .waitFor(parent().getRxConfig().commandConfig.getCommandPolicy().getTimeout().toMillis() * objects.size());
                }
        );
        return this;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <K, V> StreamerGroup addActor(CacheHandle cacheHandle, ActPolicy actPolicy, CommandPolicy commandPolicy, Strategy0<Map<K, V>> strategy, Optional<RxObserver<Map<K, V>>> rxObserver) {
        return addActor(cacheHandle, rxConfig.commandConfig.getCircuitBreakerId(), rxConfig.commandConfig.getRateLimiterId(), strategy, rxObserver, actPolicy, commandPolicy);
    }

    private static <K, V> Strategy0<Map<K, V>> createCacher(K key, V value, Strategy2<Boolean, K, V> pusher) {
        return () -> pusher.perform(key, value)
                ? Map.of(key, value)
                : emptyMap();
    }

    private static <K, V> Strategy0<Map<K, V>> createCacher(K key, Strategy0<V> computer, Strategy2<Boolean, K, V> pusher) {
        return () -> {
            V value = computer.perform();
            return pusher.perform(key, value)
                    ? Map.of(key, value)
                    : emptyMap();
        };
    }
}
