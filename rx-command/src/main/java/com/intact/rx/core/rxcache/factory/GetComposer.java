package com.intact.rx.core.rxcache.factory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.*;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.core.cache.factory.DataCachePolicyBuilder;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.factory.CommandPolicyBuilder;
import com.intact.rx.core.rxcache.StreamerGroup;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.api.GetComposerApi;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.*;
import com.intact.rx.templates.Validate;

/**
 * Assists in creating lambda functions with similar characteristics and attach main strategy to a StreamerGroup.
 *
 * @param <K> key type in cache
 * @param <V> value type in cache
 */
@SuppressWarnings({"WeakerAccess", "unused", "PublicMethodNotExposedInInterface"})
public class GetComposer<K, V> implements GetComposerApi<K, V> {
    private final State state;
    private final FunctionComposer<K, V> getComposer;
    private final FunctionComposer<K, V> fallbackComposer;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public GetComposer(StreamerGroup group, RxStreamer rxStreamer, CacheHandle cacheHandle, CommandPolicy commandPolicy, ActPolicy actPolicy) {
        this.state = new State(group, cacheHandle, commandPolicy, actPolicy);
        this.getComposer = new FunctionComposer<>(rxStreamer, cacheHandle, this);
        this.fallbackComposer = new FunctionComposer<>(rxStreamer, cacheHandle, this);
    }

    @SuppressWarnings("PackageVisibleField")
    private static class State {
        final StreamerGroup group;
        final CacheHandle cacheHandle;

        final ActPolicy actPolicy;
        final CommandPolicyBuilder commandPolicyBuilder;
        final DataCachePolicyBuilder dataCachePolicyBuilder;
        CircuitId circuitBreakerId;
        RateLimiterId rateLimiterId;

        State(StreamerGroup group, CacheHandle cacheHandle, CommandPolicy commandPolicy, ActPolicy actPolicy) {
            this.group = requireNonNull(group);
            this.cacheHandle = requireNonNull(cacheHandle);
            this.actPolicy = requireNonNull(actPolicy);
            this.commandPolicyBuilder = CommandPolicyBuilder.from(commandPolicy);
            this.dataCachePolicyBuilder = DataCachePolicyBuilder.from(actPolicy.getCachePolicy().getDataCachePolicy());
            this.circuitBreakerId = CircuitId.none();
            this.rateLimiterId = RateLimiterId.none();
        }
    }

    // -----------------------------------------------------------
    // Create actor one-off
    // -----------------------------------------------------------

    public GetComposerApi<K, V> get(Strategy0<Map<K, V>> actor) {
        return getComposer.get(actor);
    }

    public <T> GetComposerApi<K, V> mapEachValueParallel(Class<T> from, Strategy1<Map<K, V>, T> strategy) {
        return getComposer.mapEachValueParallel(from, strategy);
    }

    public <T> GetComposerApi<K, V> mapEachValueSequential(Class<T> from, Strategy1<Map<K, V>, T> strategy) {
        return getComposer.mapEachValueSequential(from, strategy);
    }

    // -----------------------------------------------------------
    // Act on collections
    // -----------------------------------------------------------

    public GetComposerApi<K, V> mapValues(Strategy1<Map<K, V>, Collection<V>> strategy) {
        return getComposer.mapValues(strategy);
    }

    public <A> GetComposerApi<K, V> mapValues(Class<A> from, Strategy1<Map<K, V>, Collection<A>> strategy) {
        return getComposer.mapValues(from, strategy);
    }

    public <A, B> GetComposerApi<K, V> mapValues(Class<A> fromA, Class<B> fromB, Strategy2<Map<K, V>, Collection<A>, Collection<B>> strategy) {
        return getComposer.mapValues(fromA, fromB, strategy);
    }

    public <A, B, C> GetComposerApi<K, V> mapValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Strategy3<Map<K, V>, Collection<A>, Collection<B>, Collection<C>> strategy) {
        return getComposer.mapValues(fromA, fromB, fromC, strategy);
    }

    public <A, B, C, D> GetComposerApi<K, V> mapValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Strategy4<Map<K, V>, Collection<A>, Collection<B>, Collection<C>, Collection<D>> strategy) {
        return getComposer.mapValues(fromA, fromB, fromC, fromD, strategy);
    }

    public <A, B, C, D, E> GetComposerApi<K, V> mapValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, Strategy5<Map<K, V>, Collection<A>, Collection<B>, Collection<C>, Collection<D>, Collection<E>> strategy) {
        return getComposer.mapValues(fromA, fromB, fromC, fromD, fromE, strategy);
    }

    // -----------------------------------------------------------
    // Act on caches
    // -----------------------------------------------------------

    public GetComposerApi<K, V> map(Strategy1<Map<K, V>, RxCache<K, V>> strategy) {
        return getComposer.map(strategy);
    }

    public <KA, A> GetComposerApi<K, V> map(Class<A> from, Strategy1<Map<K, V>, RxCache<KA, A>> strategy) {
        return getComposer.map(from, strategy);
    }

    public <KA, A, KB, B> GetComposerApi<K, V> map(Class<A> fromA, Class<B> fromB, Strategy2<Map<K, V>, RxCache<KA, A>, RxCache<KB, B>> strategy) {
        return getComposer.map(fromA, fromB, strategy);
    }

    public <KA, A, KB, B, KC, C> GetComposerApi<K, V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Strategy3<Map<K, V>, RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>> strategy) {
        return getComposer.map(fromA, fromB, fromC, strategy);
    }

    public <KA, A, KB, B, KC, C, KD, D> GetComposerApi<K, V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Strategy4<Map<K, V>, RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>, RxCache<KD, D>> strategy) {
        return getComposer.map(fromA, fromB, fromC, fromD, strategy);
    }

    public <KA, A, KB, B, KC, C, KD, D, KE, E> GetComposerApi<K, V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, Strategy5<Map<K, V>, RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>, RxCache<KD, D>, RxCache<KE, E>> strategy) {
        return getComposer.map(fromA, fromB, fromC, fromD, fromE, strategy);
    }

    // -----------------------------------------------------------
    // Act on key
    // -----------------------------------------------------------

    public <KA, A> GetComposerApi<K, V> mapKey(Class<A> fromA, KA keyA, Strategy2<Map<K, V>, KA, Optional<A>> strategy) {
        return getComposer.mapKey(fromA, keyA, strategy);
    }

    public <KA, A, KB, B> GetComposerApi<K, V> mapKeys(Class<A> fromA, Class<B> fromB, KA keyA, KB keyB, Strategy4<Map<K, V>, KA, Optional<A>, KB, Optional<B>> strategy) {
        return getComposer.mapKeys(fromA, fromB, keyA, keyB, strategy);
    }

    // -----------------------------------------------------------
    // Compose actor function
    // -----------------------------------------------------------

    @Override
    public RxStreamer done() {
        return build().parent();
    }

    @Override
    public RxStreamer subscribe() {
        return build().parent().subscribe();
    }

    @Override
    public StreamerGroup parallel() {
        return build().parallel();
    }

    @Override
    public StreamerGroup sequential() {
        return build().sequential();
    }

    // -----------------------------------------------------------
    // Override policy on the execution of the command
    // -----------------------------------------------------------

    @Override
    public GetComposerApi<K, V> withCachedObjectLifetime(Lifetime lifetime) {
        state.dataCachePolicyBuilder.withObjectLifetime(lifetime);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withCacheResourceLimits(ResourceLimits resourceLimits) {
        state.dataCachePolicyBuilder.withResourceLimits(resourceLimits);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withCommandPolicy(CommandPolicy commandPolicy) {
        state.commandPolicyBuilder.replace(commandPolicy);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withAttempt(Attempt attempt) {
        state.commandPolicyBuilder.withAttempt(attempt);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withTimeout(Timeout timeout) {
        state.commandPolicyBuilder.withTimeout(timeout);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withInterval(Interval interval) {
        state.commandPolicyBuilder.withInterval(interval);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withRetryInterval(Interval retryInterval) {
        state.commandPolicyBuilder.withRetryInterval(retryInterval);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
        state.commandPolicyBuilder.withCircuitBreakerPolicy(circuitBreakerPolicy);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withCircuitBreakerId(CircuitId circuitBreakerId) {
        state.circuitBreakerId = requireNonNull(circuitBreakerId);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy) {
        state.commandPolicyBuilder.withRateLimiterPolicy(rateLimiterPolicy);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withRateLimiterId(RateLimiterId rateLimiterId) {
        state.rateLimiterId = requireNonNull(rateLimiterId);
        return this;
    }

    @Override
    public GetComposerApi<K, V> withSuccessCriterion(Criterion successCriterion) {
        state.commandPolicyBuilder.withSuccessCriterion(successCriterion);
        return this;
    }

    // -----------------------------------------------------------
    // Create fallback actor one-off
    // -----------------------------------------------------------

    @Override
    public GetComposerApi<K, V> fallbackGet(Strategy0<Map<K, V>> actor) {
        return fallbackComposer.get(actor);
    }

    // -----------------------------------------------------------
    // Compose actor function
    // -----------------------------------------------------------

    @Override
    public FunctionComposer<K, V> withDetails() {
        return getComposer;
    }

    @Override
    public FunctionComposer<K, V> withFallbackDetails() {
        return fallbackComposer;
    }

    // -----------------------------------------------------------
    // build act
    // -----------------------------------------------------------

    @Override
    public StreamerGroup build() {
        Validate.assertTrue(!getComposer.isEmpty());
        return fallbackComposer.isEmpty()
                ?
                state.group.addActor(
                        state.cacheHandle,
                        state.circuitBreakerId,
                        state.rateLimiterId,
                        getComposer.getGetter(),
                        Optional.of(getComposer.getRxLambdaSubject()),
                        ActPolicy.from(state.actPolicy, CachePolicy.create(state.actPolicy.getCachePolicy().getCacheMasterPolicy(), state.dataCachePolicyBuilder.build())),
                        state.commandPolicyBuilder.build()
                )
                :
                state.group.addActor(
                        state.cacheHandle,
                        state.circuitBreakerId,
                        state.rateLimiterId,
                        getComposer.getGetter(),
                        fallbackComposer.getGetter(),
                        Optional.of(getComposer.getRxLambdaSubject()),
                        Optional.of(fallbackComposer.getRxLambdaSubject()),
                        ActPolicy.from(state.actPolicy, CachePolicy.create(state.actPolicy.getCachePolicy().getCacheMasterPolicy(), state.dataCachePolicyBuilder.build())),
                        state.commandPolicyBuilder.build()
                );
    }
}
