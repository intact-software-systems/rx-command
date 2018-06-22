package com.intact.rx.core.rxcache.factory;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.command.*;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.core.cache.factory.DataCachePolicyBuilder;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.factory.CommandPolicyBuilder;
import com.intact.rx.core.rxcache.StreamerGroup;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.api.ValueComposerApi;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.*;
import com.intact.rx.templates.Validate;

/**
 * Assists in creating lambda functions with similar characteristics and attach main strategy to a StreamerGroup.
 *
 * @param <V> key type in cache
 */
@SuppressWarnings({"WeakerAccess", "unused", "PublicMethodNotExposedInInterface"})
public class ValueComposer<V> implements ValueComposerApi<V> {
    private final State state;
    private final FunctionValueComposer<V> getComposer;
    private final FunctionValueComposer<V> fallbackComposer;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public ValueComposer(StreamerGroup group, RxStreamer rxStreamer, CacheHandle cacheHandle, ActPolicy actPolicy, CommandPolicy commandPolicy) {
        this.state = new State(group, cacheHandle, actPolicy, commandPolicy);
        this.getComposer = new FunctionValueComposer<>(rxStreamer, cacheHandle, this);
        this.fallbackComposer = new FunctionValueComposer<>(rxStreamer, cacheHandle, this);
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

        State(StreamerGroup group, CacheHandle cacheHandle, ActPolicy actPolicy, CommandPolicy commandPolicy) {
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
    // Act on collections
    // -----------------------------------------------------------

    public ValueComposerApi<V> map(Strategy1<Collection<V>, Collection<V>> strategy) {
        return getComposer.map(strategy);
    }

    public <A> ValueComposerApi<V> map(Class<A> from, Strategy1<Collection<V>, Collection<A>> strategy) {
        return getComposer.map(from, strategy);
    }

    public <A, B> ValueComposerApi<V> map(Class<A> fromA, Class<B> fromB, Strategy2<Collection<V>, Collection<A>, Collection<B>> strategy) {
        return getComposer.map(fromA, fromB, strategy);
    }

    public <A, B, C> ValueComposerApi<V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Strategy3<Collection<V>, Collection<A>, Collection<B>, Collection<C>> strategy) {
        return getComposer.map(fromA, fromB, fromC, strategy);
    }

    public <A, B, C, D> ValueComposerApi<V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Strategy4<Collection<V>, Collection<A>, Collection<B>, Collection<C>, Collection<D>> strategy) {
        return getComposer.map(fromA, fromB, fromC, fromD, strategy);
    }

    public <A, B, C, D, E> ValueComposerApi<V> map(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, Strategy5<Collection<V>, Collection<A>, Collection<B>, Collection<C>, Collection<D>, Collection<E>> strategy) {
        return getComposer.map(fromA, fromB, fromC, fromD, fromE, strategy);
    }

    // -----------------------------------------------------------
    // Act on keys
    // -----------------------------------------------------------

    public <KA, A> ValueComposerApi<V> mapKeys(Class<A> from, Strategy1<Collection<V>, Set<KA>> strategy) {
        return getComposer.mapKeys(from, strategy);
    }

    public <KA, A, KB, B> ValueComposerApi<V> mapKeys(Class<A> fromA, Class<B> fromB, Strategy2<Collection<V>, Set<KA>, Set<KB>> strategy) {
        return getComposer.mapKeys(fromA, fromB, strategy);
    }

    public <KA, A, KB, B, KC, C> ValueComposerApi<V> mapKeys(Class<A> fromA, Class<B> fromB, Class<C> fromC, Strategy3<Collection<V>, Set<KA>, Set<KB>, Set<KC>> strategy) {
        return getComposer.mapKeys(fromA, fromB, fromC, strategy);
    }

    public <KA, A, KB, B, KC, C, KD, D> ValueComposerApi<V> mapKeys(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Strategy4<Collection<V>, Set<KA>, Set<KB>, Set<KC>, Set<KD>> strategy) {
        return getComposer.mapKeys(fromA, fromB, fromC, fromD, strategy);
    }

    public <KA, A, KB, B, KC, C, KD, D, KE, E> ValueComposerApi<V> mapKeys(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, Strategy5<Collection<V>, Set<KA>, Set<KB>, Set<KC>, Set<KD>, Set<KE>> strategy) {
        return getComposer.mapKeys(fromA, fromB, fromC, fromD, fromE, strategy);
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
    // Override policy on the execution of the act command
    // -----------------------------------------------------------

    @Override
    public ValueComposerApi<V> withCachedObjectLifetime(Lifetime lifetime) {
        state.dataCachePolicyBuilder.withObjectLifetime(lifetime);
        return this;
    }

    @Override
    public ValueComposerApi<V> withCacheResourceLimits(ResourceLimits resourceLimits) {
        state.dataCachePolicyBuilder.withResourceLimits(resourceLimits);
        return this;
    }

    @Override
    public ValueComposerApi<V> withCommandPolicy(CommandPolicy commandPolicy) {
        state.commandPolicyBuilder.replace(commandPolicy);
        return this;
    }

    @Override
    public ValueComposerApi<V> withAttempt(Attempt attempt) {
        state.commandPolicyBuilder.withAttempt(attempt);
        return this;
    }

    @Override
    public ValueComposerApi<V> withTimeout(Timeout timeout) {
        state.commandPolicyBuilder.withTimeout(timeout);
        return this;
    }

    @Override
    public ValueComposerApi<V> withInterval(Interval interval) {
        state.commandPolicyBuilder.withInterval(interval);
        return this;
    }

    @Override
    public ValueComposerApi<V> withRetryInterval(Interval retryInterval) {
        state.commandPolicyBuilder.withRetryInterval(retryInterval);
        return this;
    }

    @Override
    public ValueComposerApi<V> withCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
        state.commandPolicyBuilder.withCircuitBreakerPolicy(circuitBreakerPolicy);
        return this;
    }

    @Override
    public ValueComposerApi<V> withCircuitBreakerId(CircuitId circuitId) {
        state.circuitBreakerId = requireNonNull(circuitId);
        return this;
    }

    @Override
    public ValueComposerApi<V> withRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy) {
        state.commandPolicyBuilder.withRateLimiterPolicy(rateLimiterPolicy);
        return this;
    }

    @Override
    public ValueComposerApi<V> withRateLimiterId(RateLimiterId rateLimiterId) {
        state.rateLimiterId = requireNonNull(rateLimiterId);
        return this;
    }

    @Override
    public ValueComposerApi<V> withSuccessCriterion(Criterion successCriterion) {
        state.commandPolicyBuilder.withSuccessCriterion(successCriterion);
        return this;
    }

    // -----------------------------------------------------------
    // Create fallback actor one-off
    // -----------------------------------------------------------

    @Override
    public ValueComposerApi<V> fallbackGet(Strategy0<Collection<V>> actor) {
        return fallbackComposer.get(actor);
    }

    // -----------------------------------------------------------
    // Compose actor function
    // -----------------------------------------------------------

    @Override
    public FunctionValueComposer<V> withDetails() {
        return getComposer;
    }

    @Override
    public FunctionValueComposer<V> withFallbackDetails() {
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
