package com.intact.rx.core.rxcache.factory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.*;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.subject.RxLambdaSubject;
import com.intact.rx.core.rxcache.api.GetComposerApi;

/**
 * Use this to create main act and fallback act
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FunctionComposer<K, V> {
    private Strategy0<Boolean> executeMainStrategy = () -> true;
    private Strategy1<Boolean, Optional<Map<K, V>>> elseIfCondition = data -> data.map(Map::isEmpty).orElse(true);
    private Strategy0<Map<K, V>> elseAct = Collections::emptyMap;
    private Strategy1<Map<K, V>, Map<K, V>> postComputer = data -> data;
    private final Collection<Strategy2<Boolean, K, V>> filters = new ArrayList<>();
    private final RxLambdaSubject<Map<K, V>> rxLambdaSubject;

    private final RxStreamer rxStreamer;
    private final CacheHandle cacheHandle;
    private final GetComposerApi<K, V> composer;

    private Strategy0<Map<K, V>> createdGetter;

    FunctionComposer(RxStreamer rxStreamer, CacheHandle cacheHandle, GetComposerApi<K, V> composer) {
        this.rxStreamer = requireNonNull(rxStreamer);
        this.cacheHandle = requireNonNull(cacheHandle);
        this.composer = requireNonNull(composer);
        this.rxLambdaSubject = new RxLambdaSubject<>();
    }

    public boolean isEmpty() {
        return createdGetter == null;
    }

    public Strategy0<Map<K, V>> getGetter() {
        return createdGetter;
    }

    public RxLambdaSubject<Map<K, V>> getRxLambdaSubject() {
        return rxLambdaSubject;
    }

    // -----------------------------------------------------------
    // fill in execution algorithm
    // -----------------------------------------------------------

    /**
     * @param elseIfCondition represents the condition to decide whether to execute the optional
     *                        elseAct function, based on action return value. Only one.
     *                        Default: data -> data == null || data.isEmpty();
     * @return this
     */
    public FunctionComposer<K, V> elseIf(Strategy1<Boolean, Optional<Map<K, V>>> elseIfCondition) {
        this.elseIfCondition = requireNonNull(elseIfCondition);
        return this;
    }

    /**
     * @param elseIfCondition represents the condition to decide whether to execute the optional
     *                        elseAct function, based on main act's return value.
     * @param elseAct         lambda to execute if elseIfCondition returns true. Overrides fallbackData. Only one.
     * @return this
     */
    public FunctionComposer<K, V> elseIfGet(Strategy1<Boolean, Optional<Map<K, V>>> elseIfCondition, Strategy0<Map<K, V>> elseAct) {
        this.elseIfCondition = requireNonNull(elseIfCondition);
        this.elseAct = requireNonNull(elseAct);
        return this;
    }

    /**
     * @param elseAct lambda to execute if elseIfCondition returns true. Overrides fallbackData. Only one.
     * @return this
     */
    public FunctionComposer<K, V> elseGet(Strategy0<Map<K, V>> elseAct) {
        this.elseAct = requireNonNull(elseAct);
        return this;
    }

    /**
     * @param elseAct lambda to execute if elseIfCondition returns true. Overrides fallbackData. Only one.
     * @return this
     */
    public FunctionComposer<K, V> elseRun(VoidStrategy0 elseAct) {
        requireNonNull(elseAct);
        this.elseAct = () -> {
            elseAct.perform();
            return Collections.emptyMap();
        };
        return this;
    }

    /**
     * @param elseData data to return if elseIfCondition returns true. Overrides fallback. Only one.
     * @return this
     */
    public FunctionComposer<K, V> elseReturn(Map<K, V> elseData) {
        requireNonNull(elseData);
        this.elseAct = () -> elseData;
        return this;
    }

    /**
     * If key is present in cache, then skip execution of main "act". Only one.
     *
     * @param key the key to locate value
     * @return this
     */
    public FunctionComposer<K, V> skipGetIfCached(K key) {
        requireNonNull(key);
        executeMainStrategy = () -> !rxStreamer.cache(cacheHandle).read(key).isPresent();
        return this;
    }

    /**
     * If a value is found in the cache, acceptor checks if it is acceptable. Only one.
     *
     * @param key      the key to locate value
     * @param acceptor the acceptor function
     * @return this
     */
    public FunctionComposer<K, V> performGetIf(K key, Strategy1<Boolean, Optional<V>> acceptor) {
        requireNonNull(key);
        requireNonNull(acceptor);

        executeMainStrategy = () -> acceptor.perform(rxStreamer.<K, V>cache(cacheHandle).read(key));
        return this;
    }

    /**
     * Perform a check key check. Only one.
     *
     * @param acceptor the acceptor function
     * @return this
     */
    public FunctionComposer<K, V> performGetIf(Strategy0<Boolean> acceptor) {
        executeMainStrategy = requireNonNull(acceptor);
        return this;
    }

    /**
     * @param postComputer lambda to execute on return value of main "act". Only one.
     * @return this
     */
    public FunctionComposer<K, V> postCompute(Strategy1<Map<K, V>, Map<K, V>> postComputer) {
        this.postComputer = requireNonNull(postComputer);
        return this;
    }

    /**
     * @param filter apply filter function on data returned by main "act". Every call adds new filter.
     * @return this
     */
    public FunctionComposer<K, V> filter(Strategy2<Boolean, K, V> filter) {
        filters.add(requireNonNull(filter));
        return this;
    }

    // -----------------------------------------------------------
    // Callback functions
    // -----------------------------------------------------------

    public FunctionComposer<K, V> onCompleteDo(VoidStrategy0 onCompleted) {
        rxLambdaSubject.onCompleteDo(onCompleted);
        return this;
    }

    public FunctionComposer<K, V> onNextDo(VoidStrategy1<Map<K, V>> onNext) {
        rxLambdaSubject.onNextDo(onNext);
        return this;
    }

    public FunctionComposer<K, V> onErrorDo(VoidStrategy1<Throwable> onError) {
        rxLambdaSubject.onErrorDo(onError);
        return this;
    }

    public FunctionComposer<K, V> onSubscribeDo(VoidStrategy1<Subscription> onSubscribe) {
        rxLambdaSubject.onSubscribeDo(onSubscribe);
        return this;
    }

    // -----------------------------------------------------------
    // Act functions
    // -----------------------------------------------------------

    public GetComposerApi<K, V> get(Strategy0<Map<K, V>> strategy) {
        createdGetter = createActorLambda(strategy);
        return composer;
    }

    public <T> GetComposerApi<K, V> mapEachValueParallel(Class<T> from, Strategy1<Map<K, V>, T> strategy) {
        createdGetter = createActorLambda(
                () -> {
                    Map<K, V> all = new ConcurrentHashMap<>();
                    rxStreamer.cache(from).readAll().entrySet()
                            .parallelStream()
                            .forEach(entry -> all.putAll(strategy.perform(entry.getValue())));
                    return all;
                }
        );
        return composer;
    }

    public <T> GetComposerApi<K, V> mapEachValueSequential(Class<T> from, Strategy1<Map<K, V>, T> strategy) {
        createdGetter = createActorLambda(
                () -> {
                    Map<K, V> all = new HashMap<>();
                    rxStreamer.cache(from).readAll().forEach((key, value) -> all.putAll(strategy.perform(value)));
                    return all;
                }
        );
        return composer;
    }

    // -----------------------------------------------------------
    // Act on value collection
    // -----------------------------------------------------------

    public GetComposerApi<K, V> mapValues(Strategy1<Map<K, V>, Collection<V>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.<K, V>cache(cacheHandle).readAsList()));
        return composer;
    }

    public <A> GetComposerApi<K, V> mapValues(Class<A> from, Strategy1<Map<K, V>, Collection<A>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(from).readAsList()));
        return composer;
    }

    public <A, B> GetComposerApi<K, V> mapValues(Class<A> fromA, Class<B> fromB, Strategy2<Map<K, V>, Collection<A>, Collection<B>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList()));
        return composer;
    }

    public <A, B, C> GetComposerApi<K, V> mapValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Strategy3<Map<K, V>, Collection<A>, Collection<B>, Collection<C>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList(), rxStreamer.cache(fromC).readAsList()));
        return composer;
    }

    public <A, B, C, D> GetComposerApi<K, V> mapValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Strategy4<Map<K, V>, Collection<A>, Collection<B>, Collection<C>, Collection<D>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList(), rxStreamer.cache(fromC).readAsList(), rxStreamer.cache(fromD).readAsList()));
        return composer;
    }

    public <A, B, C, D, E> GetComposerApi<K, V> mapValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, Strategy5<Map<K, V>, Collection<A>, Collection<B>, Collection<C>, Collection<D>, Collection<E>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList(), rxStreamer.cache(fromC).readAsList(), rxStreamer.cache(fromD).readAsList(), rxStreamer.cache(fromE).readAsList()));
        return composer;
    }

    // -----------------------------------------------------------
    // Act on caches
    // -----------------------------------------------------------

    public GetComposerApi<K, V> map(Strategy1<Map<K, V>, RxCache<K, V>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(cacheHandle)));
        return composer;
    }

    public <KA, A> GetComposerApi<K, V> map(Class<A> from, Strategy1<Map<K, V>, RxCache<KA, A>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(from)));
        return composer;
    }

    public <KA, A, KB, B> GetComposerApi<K, V> map(Class<A> fromA, Class<B> fromB, Strategy2<Map<K, V>, RxCache<KA, A>, RxCache<KB, B>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA), rxStreamer.cache(fromB)));
        return composer;
    }

    public <KA, A, KB, B, KC, C> GetComposerApi<K, V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Strategy3<Map<K, V>, RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA), rxStreamer.cache(fromB), rxStreamer.cache(fromC)));
        return composer;
    }

    public <KA, A, KB, B, KC, C, KD, D> GetComposerApi<K, V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Strategy4<Map<K, V>, RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>, RxCache<KD, D>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA), rxStreamer.cache(fromB), rxStreamer.cache(fromC), rxStreamer.cache(fromD)));
        return composer;
    }

    public <KA, A, KB, B, KC, C, KD, D, KE, E> GetComposerApi<K, V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, Strategy5<Map<K, V>, RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>, RxCache<KD, D>, RxCache<KE, E>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA), rxStreamer.cache(fromB), rxStreamer.cache(fromC), rxStreamer.cache(fromD), rxStreamer.cache(fromE)));
        return composer;
    }

    // -----------------------------------------------------------
    // Act on key
    // -----------------------------------------------------------

    public GetComposerApi<K, V> mapKey(K key, Strategy2<Map<K, V>, K, Optional<V>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(key, rxStreamer.<K, V>cache(cacheHandle).read(key)));
        return composer;
    }

    public <KA, A> GetComposerApi<K, V> mapKey(Class<A> fromA, KA keyA, Strategy2<Map<K, V>, KA, Optional<A>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(keyA, rxStreamer.<KA, A>cache(fromA).read(keyA)));
        return composer;
    }

    public <KA, A, KB, B> GetComposerApi<K, V> mapKeys(Class<A> fromA, Class<B> fromB, KA keyA, KB keyB, Strategy4<Map<K, V>, KA, Optional<A>, KB, Optional<B>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(keyA, rxStreamer.<KA, A>cache(fromA).read(keyA), keyB, rxStreamer.<KB, B>cache(fromB).read(keyB)));
        return composer;
    }

    // -----------------------------------------------------------
    // Default act execution based on composer inputs
    // -----------------------------------------------------------

    private Strategy0<Map<K, V>> createActorLambda(Strategy0<Map<K, V>> strategy) {
        return () -> executeMainStrategy.perform()
                ?
                applyFallbackCondition(
                        applyFilters(
                                postComputer.perform(
                                        strategy.perform()
                                )
                        )
                )
                : Collections.emptyMap();
    }

    private Map<K, V> applyFilters(Map<K, V> data) {
        if (data == null) {
            return null;
        }
        else if (data.isEmpty() || filters.isEmpty()) {
            return data;
        }

        Map<K, V> filteredData = new HashMap<>();
        for (Map.Entry<K, V> entry : data.entrySet()) {
            filters.stream()
                    .filter(filter -> filter.perform(entry.getKey(), entry.getValue()))
                    .forEach(filter -> filteredData.put(entry.getKey(), entry.getValue()));
        }
        return filteredData;
    }

    private Map<K, V> applyFallbackCondition(Map<K, V> data) {
        return elseIfCondition.perform(Optional.ofNullable(data))
                ? elseAct.perform()
                : data;
    }
}
