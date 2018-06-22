package com.intact.rx.core.rxcache.factory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.*;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.subject.RxLambdaSubject;
import com.intact.rx.core.rxcache.api.RunComposerApi;

/**
 * Use this to create main act and fallback act
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class FunctionRunComposer {
    private Strategy0<Boolean> executeMainStrategy;
    private VoidStrategy0 createdRun;

    private final RxStreamer rxStreamer;
    private final RunComposerApi composer;
    private final RxLambdaSubject<Map<Void, Void>> rxLambdaSubject;

    FunctionRunComposer(RxStreamer rxStreamer, RunComposerApi composer) {
        this.executeMainStrategy = () -> true;
        this.createdRun = null;
        this.rxStreamer = requireNonNull(rxStreamer);
        this.composer = requireNonNull(composer);
        this.rxLambdaSubject = new RxLambdaSubject<>();
    }

    public boolean isEmpty() {
        return createdRun == null;
    }

    public VoidStrategy0 getRun() {
        return createdRun;
    }

    public RxLambdaSubject<Map<Void, Void>> getRxLambdaSubject() {
        return rxLambdaSubject;
    }

    // -----------------------------------------------------------
    // fill in execution algorithm
    // -----------------------------------------------------------

    /**
     * Perform a check key check. Only one.
     *
     * @param acceptor the acceptor function
     * @return this
     */
    public FunctionRunComposer performRunIf(Strategy0<Boolean> acceptor) {
        executeMainStrategy = requireNonNull(acceptor);
        return this;
    }

    /**
     * If a value is found in the cache, acceptor checks if it is acceptable. Only one.
     *
     * @param cachedType type cached
     * @param key        the key to locate value
     * @param acceptor   the acceptor function
     * @return this
     */
    public <K, V> FunctionRunComposer performRunIf(Class<V> cachedType, K key, Strategy1<Boolean, Optional<V>> acceptor) {
        requireNonNull(key);
        requireNonNull(acceptor);

        executeMainStrategy = () -> acceptor.perform(rxStreamer.<K, V>cache(CacheHandle.create(rxStreamer.getRxConfig().domainCacheId, rxStreamer.getMasterCacheId(), cachedType)).read(key));
        return this;
    }

    // -----------------------------------------------------------
    // Callback functions
    // -----------------------------------------------------------

    public FunctionRunComposer onCompleteDo(VoidStrategy0 onCompleted) {
        rxLambdaSubject.onCompleteDo(onCompleted);
        return this;
    }

    public FunctionRunComposer onErrorDo(VoidStrategy1<Throwable> onError) {
        rxLambdaSubject.onErrorDo(onError);
        return this;
    }

    public FunctionRunComposer onSubscribeDo(VoidStrategy1<Subscription> onSubscribe) {
        rxLambdaSubject.onSubscribeDo(onSubscribe);
        return this;
    }

    // -----------------------------------------------------------
    // Act functions
    // -----------------------------------------------------------

    public RunComposerApi run(VoidStrategy0 strategy) {
        createdRun = createActorLambda(strategy);
        return composer;
    }

    public <T> RunComposerApi runEachValueParallel(Class<T> from, VoidStrategy1<T> strategy) {
        createdRun = createActorLambda(
                () -> rxStreamer.cache(from).readAll().entrySet()
                        .parallelStream()
                        .forEach(entry -> strategy.perform(entry.getValue()))
        );
        return composer;
    }

    public <T> RunComposerApi runEachValueSequential(Class<T> from, VoidStrategy1<T> strategy) {
        createdRun = createActorLambda(
                () -> rxStreamer.cache(from).readAll().forEach((key, value) -> strategy.perform(value))
        );
        return composer;
    }

    // -----------------------------------------------------------
    // Act on value collection
    // -----------------------------------------------------------

    public <A> RunComposerApi runOnValues(Class<A> from, VoidStrategy1<Collection<A>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(from).readAsList()));
        return composer;
    }

    public <A, B> RunComposerApi runOnValues(Class<A> fromA, Class<B> fromB, VoidStrategy2<Collection<A>, Collection<B>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList()));
        return composer;
    }

    public <A, B, C> RunComposerApi runOnValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, VoidStrategy3<Collection<A>, Collection<B>, Collection<C>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList(), rxStreamer.cache(fromC).readAsList()));
        return composer;
    }

    public <A, B, C, D> RunComposerApi runOnValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, VoidStrategy4<Collection<A>, Collection<B>, Collection<C>, Collection<D>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList(), rxStreamer.cache(fromC).readAsList(), rxStreamer.cache(fromD).readAsList()));
        return composer;
    }

    public <A, B, C, D, E> RunComposerApi runOnValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, VoidStrategy5<Collection<A>, Collection<B>, Collection<C>, Collection<D>, Collection<E>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA).readAsList(), rxStreamer.cache(fromB).readAsList(), rxStreamer.cache(fromC).readAsList(), rxStreamer.cache(fromD).readAsList(), rxStreamer.cache(fromE).readAsList()));
        return composer;
    }

    // -----------------------------------------------------------
    // Act on caches
    // -----------------------------------------------------------

    public <K1, T> RunComposerApi runOn(Class<T> from, VoidStrategy1<RxCache<K1, T>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(from)));
        return composer;
    }

    public <KA, A, KB, B> RunComposerApi runOn(Class<A> fromA, Class<B> fromB, VoidStrategy2<RxCache<KA, A>, RxCache<KB, B>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA), rxStreamer.cache(fromB)));
        return composer;
    }

    public <KA, A, KB, B, KC, C> RunComposerApi runOn(Class<A> fromA, Class<B> fromB, Class<C> fromC, VoidStrategy3<RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA), rxStreamer.cache(fromB), rxStreamer.cache(fromC)));
        return composer;
    }

    public <KA, A, KB, B, KC, C, KD, D> RunComposerApi runOn(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, VoidStrategy4<RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>, RxCache<KD, D>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA), rxStreamer.cache(fromB), rxStreamer.cache(fromC), rxStreamer.cache(fromD)));
        return composer;
    }

    public <KA, A, KB, B, KC, C, KD, D, KE, E> RunComposerApi runOn(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, VoidStrategy5<RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>, RxCache<KD, D>, RxCache<KE, E>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(rxStreamer.cache(fromA), rxStreamer.cache(fromB), rxStreamer.cache(fromC), rxStreamer.cache(fromD), rxStreamer.cache(fromE)));
        return composer;
    }

    // -----------------------------------------------------------
    // Act on key
    // -----------------------------------------------------------

    public <KeyForT, T> RunComposerApi runOnKey(Class<T> from, KeyForT keyForT, VoidStrategy2<KeyForT, Optional<T>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(keyForT, rxStreamer.<KeyForT, T>cache(from).read(keyForT)));
        return composer;
    }

    public <KA, A, KB, B> RunComposerApi runOnKeys(Class<A> fromA, Class<B> fromB, KA keyA, KB keyB, VoidStrategy4<KA, Optional<A>, KB, Optional<B>> strategy) {
        createdRun = createActorLambda(() -> strategy.perform(keyA, rxStreamer.<KA, A>cache(fromA).read(keyA), keyB, rxStreamer.<KB, B>cache(fromB).read(keyB)));
        return composer;
    }

    // -----------------------------------------------------------
    // Default act execution based on composer inputs
    // -----------------------------------------------------------

    private VoidStrategy0 createActorLambda(VoidStrategy0 strategy) {
        return () -> {
            if (executeMainStrategy.perform()) {
                strategy.perform();
            }
        };
    }
}
