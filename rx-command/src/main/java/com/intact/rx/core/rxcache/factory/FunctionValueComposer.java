package com.intact.rx.core.rxcache.factory;

import java.util.*;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.command.*;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.subject.RxLambdaSubject;
import com.intact.rx.core.rxcache.api.ValueComposerApi;

import static com.intact.rx.core.rxcache.StreamerGroup.toMap;

/**
 * Use this to create main act and fallback act
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FunctionValueComposer<V> {
    private Strategy0<Boolean> executeMainStrategy = () -> true;
    private Strategy1<Boolean, Optional<Collection<V>>> elseIfCondition = data -> data.map(Collection::isEmpty).orElse(true);
    private Strategy0<Collection<V>> elseAct = Collections::emptyList;
    private Strategy1<Collection<V>, Collection<V>> postComputer = data -> data;
    private final Collection<Strategy1<Boolean, V>> filters = new ArrayList<>();
    private final RxLambdaSubject<Map<V, V>> rxLambdaSubject;

    private final RxStreamer rxStreamer;
    private final CacheHandle cacheHandle;
    private final ValueComposerApi<V> composer;

    private Strategy0<Map<V, V>> createdGetter;

    FunctionValueComposer(RxStreamer rxStreamer, CacheHandle cacheHandle, ValueComposerApi<V> composer) {
        this.rxStreamer = requireNonNull(rxStreamer);
        this.cacheHandle = requireNonNull(cacheHandle);
        this.composer = requireNonNull(composer);
        this.rxLambdaSubject = new RxLambdaSubject<>();
    }

    public boolean isEmpty() {
        return createdGetter == null;
    }

    public Strategy0<Map<V, V>> getGetter() {
        return createdGetter;
    }

    public RxLambdaSubject<Map<V, V>> getRxLambdaSubject() {
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
    public FunctionValueComposer<V> elseIf(Strategy1<Boolean, Optional<Collection<V>>> elseIfCondition) {
        this.elseIfCondition = requireNonNull(elseIfCondition);
        return this;
    }

    /**
     * @param elseIfCondition represents the condition to decide whether to execute the optional
     *                        elseAct function, based on main act's return value.
     * @param elseAct         lambda to execute if elseIfCondition returns true. Overrides fallbackData. Only one.
     * @return this
     */
    public FunctionValueComposer<V> elseIfGet(Strategy1<Boolean, Optional<Collection<V>>> elseIfCondition, Strategy0<Collection<V>> elseAct) {
        this.elseIfCondition = requireNonNull(elseIfCondition);
        this.elseAct = requireNonNull(elseAct);
        return this;
    }

    /**
     * @param elseAct lambda to execute if elseIfCondition returns true. Overrides fallbackData. Only one.
     * @return this
     */
    public FunctionValueComposer<V> elseGet(Strategy0<Collection<V>> elseAct) {
        this.elseAct = requireNonNull(elseAct);
        return this;
    }

    /**
     * @param elseAct lambda to execute if elseIfCondition returns true. Overrides fallbackData. Only one.
     * @return this
     */
    public FunctionValueComposer<V> elseRun(VoidStrategy0 elseAct) {
        this.elseAct = () -> {
            elseAct.perform();
            return Collections.emptyList();
        };
        return this;
    }

    /**
     * @param elseData data to return if elseIfCondition returns true. Overrides fallback. Only one.
     * @return this
     */
    public FunctionValueComposer<V> elseReturn(Collection<V> elseData) {
        this.elseAct = () -> elseData;
        return this;
    }

    /**
     * If key is present in cache, then skip execution of main "act". Only one.
     *
     * @param value the key to locate value
     * @return this
     */
    public FunctionValueComposer<V> skipGetIfCached(V value) {
        executeMainStrategy = () -> !rxStreamer.cache(cacheHandle).read(value).isPresent();
        return this;
    }

    /**
     * If a value is found in the cache, acceptor checks if it is acceptable. Only one.
     *
     * @param value    the key to locate value
     * @param acceptor the acceptor function
     * @return this
     */
    public FunctionValueComposer<V> performGetIf(V value, Strategy1<Boolean, Optional<V>> acceptor) {
        executeMainStrategy = () -> acceptor.perform(rxStreamer.<V, V>cache(cacheHandle).read(value));
        return this;
    }

    /**
     * Perform a check key check. Only one.
     *
     * @param acceptor the acceptor function
     * @return this
     */
    public FunctionValueComposer<V> performGetIf(Strategy0<Boolean> acceptor) {
        executeMainStrategy = acceptor;
        return this;
    }

    /**
     * @param postComputer lambda to execute on return value of main "act". Only one.
     * @return this
     */
    public FunctionValueComposer<V> postCompute(Strategy1<Collection<V>, Collection<V>> postComputer) {
        this.postComputer = postComputer;
        return this;
    }

    /**
     * @param filter apply filter function on data returned by main "act". Every call adds new filter.
     * @return this
     */
    public FunctionValueComposer<V> filter(Strategy1<Boolean, V> filter) {
        filters.add(requireNonNull(filter));
        return this;
    }

    // -----------------------------------------------------------
    // Callback functions
    // -----------------------------------------------------------

    public FunctionValueComposer<V> onCompleteDo(VoidStrategy0 onCompleted) {
        rxLambdaSubject.onCompleteDo(onCompleted);
        return this;
    }

    public FunctionValueComposer<V> onNextDo(VoidStrategy1<Map<V, V>> onNext) {
        rxLambdaSubject.onNextDo(onNext);
        return this;
    }

    public FunctionValueComposer<V> onErrorDo(VoidStrategy1<Throwable> onError) {
        rxLambdaSubject.onErrorDo(onError);
        return this;
    }

    public FunctionValueComposer<V> onSubscribeDo(VoidStrategy1<Subscription> onSubscribe) {
        rxLambdaSubject.onSubscribeDo(onSubscribe);
        return this;
    }

    // -----------------------------------------------------------
    // Act functions
    // -----------------------------------------------------------

    public ValueComposerApi<V> get(Strategy0<Collection<V>> strategy) {
        createdGetter = createActorLambda(strategy);
        return composer;
    }

    // -----------------------------------------------------------
    // Act on value collection
    // -----------------------------------------------------------

    public ValueComposerApi<V> map(Strategy1<Collection<V>, Collection<V>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.<V, V>cache(cacheHandle).readAsList()));
        return composer;
    }

    public <A> ValueComposerApi<V> map(Class<A> from, Strategy1<Collection<V>, Collection<A>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(from).readAsList()));
        return composer;
    }

    public <A, B> ValueComposerApi<V> map(Class<A> fromA, Class<B> fromB, Strategy2<Collection<V>, Collection<A>, Collection<B>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList()));
        return composer;
    }

    public <A, B, C> ValueComposerApi<V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Strategy3<Collection<V>, Collection<A>, Collection<B>, Collection<C>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList(), rxStreamer.cache(fromC).readAsList()));
        return composer;
    }

    public <A, B, C, D> ValueComposerApi<V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Strategy4<Collection<V>, Collection<A>, Collection<B>, Collection<C>, Collection<D>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList(), rxStreamer.cache(fromC).readAsList(), rxStreamer.cache(fromD).readAsList()));
        return composer;
    }

    public <A, B, C, D, E> ValueComposerApi<V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, Strategy5<Collection<V>, Collection<A>, Collection<B>, Collection<C>, Collection<D>, Collection<E>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList(), rxStreamer.cache(fromC).readAsList(), rxStreamer.cache(fromD).readAsList(), rxStreamer.cache(fromE).readAsList()));
        return composer;
    }

    // -----------------------------------------------------------
    // Act on keys
    // -----------------------------------------------------------

    public <KA, A> ValueComposerApi<V> mapKeys(Class<A> from, Strategy1<Collection<V>, Set<KA>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.<KA, A>cache(from).keySet()));
        return composer;
    }

    public <KA, A, KB, B> ValueComposerApi<V> mapKeys(Class<A> fromA, Class<B> fromB, Strategy2<Collection<V>, Set<KA>, Set<KB>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.<KA, A>cache(fromA).keySet(), rxStreamer.<KB, B>cache(fromB).keySet()));
        return composer;
    }

    public <KA, A, KB, B, KC, C> ValueComposerApi<V> mapKeys(Class<A> fromA, Class<B> fromB, Class<C> fromC, Strategy3<Collection<V>, Set<KA>, Set<KB>, Set<KC>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.<KA, A>cache(fromA).keySet(), rxStreamer.<KB, B>cache(fromB).keySet(), rxStreamer.<KC, C>cache(fromC).keySet()));
        return composer;
    }

    public <KA, A, KB, B, KC, C, KD, D> ValueComposerApi<V> mapKeys(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Strategy4<Collection<V>, Set<KA>, Set<KB>, Set<KC>, Set<KD>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.<KA, A>cache(fromA).keySet(), rxStreamer.<KB, B>cache(fromB).keySet(), rxStreamer.<KC, C>cache(fromC).keySet(), rxStreamer.<KD, D>cache(fromD).keySet()));
        return composer;
    }

    public <KA, A, KB, B, KC, C, KD, D, KE, E> ValueComposerApi<V> mapKeys(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, Strategy5<Collection<V>, Set<KA>, Set<KB>, Set<KC>, Set<KD>, Set<KE>> strategy) {
        createdGetter = createActorLambda(() -> strategy.perform(rxStreamer.<KA, A>cache(fromA).keySet(), rxStreamer.<KB, B>cache(fromB).keySet(), rxStreamer.<KC, C>cache(fromC).keySet(), rxStreamer.<KD, D>cache(fromD).keySet(), rxStreamer.<KE, E>cache(fromE).keySet()));
        return composer;
    }

    // -----------------------------------------------------------
    // Default act execution based on composer inputs
    // -----------------------------------------------------------

    private Strategy0<Map<V, V>> createActorLambda(Strategy0<Collection<V>> strategy) {
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

    private Map<V, V> applyFilters(Collection<V> data) {
        if (data == null) {
            return null;
        } else if (data.isEmpty() || filters.isEmpty()) {
            return toMap(data);
        }

        Collection<V> filteredData = new ArrayList<>();
        for (V entry : data) {
            filters.stream()
                    .filter(filter -> filter.perform(entry))
                    .forEach(filter -> filteredData.add(entry));
        }
        return toMap(filteredData);
    }

    private Map<V, V> applyFallbackCondition(Map<V, V> data) {
        return elseIfCondition.perform(Optional.ofNullable(data).map(Map::values))
                ? toMap(elseAct.perform())
                : data;
    }
}
