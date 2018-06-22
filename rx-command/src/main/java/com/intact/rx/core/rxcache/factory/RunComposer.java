package com.intact.rx.core.rxcache.factory;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.*;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.factory.CommandPolicyBuilder;
import com.intact.rx.core.rxcache.StreamerGroup;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.api.RunComposerApi;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.Attempt;
import com.intact.rx.policy.Criterion;
import com.intact.rx.policy.Interval;
import com.intact.rx.policy.Timeout;
import com.intact.rx.templates.Validate;

/**
 * Assists in creating lambda functions with similar characteristics and attach main strategy to a StreamerGroup.
 */
@SuppressWarnings({"unused", "WeakerAccess", "PublicMethodNotExposedInInterface"})
public class RunComposer implements RunComposerApi {
    private final State state;
    private final FunctionRunComposer runComposer;
    private final FunctionRunComposer fallbackComposer;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public RunComposer(StreamerGroup group, RxStreamer rxStreamer, ActPolicy actPolicy, CommandPolicy commandPolicy) {
        this.state = new State(group, actPolicy, commandPolicy);
        this.runComposer = new FunctionRunComposer(rxStreamer, this);
        this.fallbackComposer = new FunctionRunComposer(rxStreamer, this);
    }

    @SuppressWarnings("PackageVisibleField")
    private static class State {
        final StreamerGroup group;
        final ActPolicy actPolicy;
        final CommandPolicyBuilder commandPolicyBuilder;
        final AtomicReference<CircuitId> circuitBreakerId;
        final AtomicReference<RateLimiterId> rateLimiterId;

        State(StreamerGroup group, ActPolicy actPolicy, CommandPolicy commandPolicy) {
            this.group = group;
            this.actPolicy = actPolicy;
            this.commandPolicyBuilder = CommandPolicyBuilder.from(commandPolicy);
            this.circuitBreakerId = new AtomicReference<>(CircuitId.none());
            this.rateLimiterId = new AtomicReference<>(RateLimiterId.none());
        }
    }

    // -----------------------------------------------------------
    // Create actor one-off
    // -----------------------------------------------------------

    public RunComposerApi run(VoidStrategy0 actor) {
        return runComposer.run(actor);
    }

    public <T> RunComposerApi runEachValueParallel(Class<T> from, VoidStrategy1<T> strategy) {
        return runComposer.runEachValueParallel(from, strategy);
    }

    public <T> RunComposerApi runEachValueSequential(Class<T> from, VoidStrategy1<T> strategy) {
        return runComposer.runEachValueSequential(from, strategy);
    }

    // -----------------------------------------------------------
    // Act on collections
    // -----------------------------------------------------------

    public <A> RunComposerApi runOnValues(Class<A> from, VoidStrategy1<Collection<A>> strategy) {
        return runComposer.runOnValues(from, strategy);
    }

    public <A, B> RunComposerApi runOnValues(Class<A> fromA, Class<B> fromB, VoidStrategy2<Collection<A>, Collection<B>> strategy) {
        return runComposer.runOnValues(fromA, fromB, strategy);
    }

    public <A, B, C> RunComposerApi runOnValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, VoidStrategy3<Collection<A>, Collection<B>, Collection<C>> strategy) {
        return runComposer.runOnValues(fromA, fromB, fromC, strategy);
    }

    public <A, B, C, D> RunComposerApi runOnValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, VoidStrategy4<Collection<A>, Collection<B>, Collection<C>, Collection<D>> strategy) {
        return runComposer.runOnValues(fromA, fromB, fromC, fromD, strategy);
    }

    public <A, B, C, D, E> RunComposerApi runOnValues(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, VoidStrategy5<Collection<A>, Collection<B>, Collection<C>, Collection<D>, Collection<E>> strategy) {
        return runComposer.runOnValues(fromA, fromB, fromC, fromD, fromE, strategy);
    }

    // -----------------------------------------------------------
    // Act on caches
    // -----------------------------------------------------------

    public <K1, T> RunComposerApi runOn(Class<T> from, VoidStrategy1<RxCache<K1, T>> strategy) {
        return runComposer.runOn(from, strategy);
    }

    public <KA, A, KB, B> RunComposerApi runOn(Class<A> fromA, Class<B> fromB, VoidStrategy2<RxCache<KA, A>, RxCache<KB, B>> strategy) {
        return runComposer.runOn(fromA, fromB, strategy);
    }

    public <KA, A, KB, B, KC, C> RunComposerApi runOn(Class<A> fromA, Class<B> fromB, Class<C> fromC, VoidStrategy3<RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>> strategy) {
        return runComposer.runOn(fromA, fromB, fromC, strategy);
    }

    public <KA, A, KB, B, KC, C, KD, D> RunComposerApi runOn(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, VoidStrategy4<RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>, RxCache<KD, D>> strategy) {
        return runComposer.runOn(fromA, fromB, fromC, fromD, strategy);
    }

    public <KA, A, KB, B, KC, C, KD, D, KE, E> RunComposerApi runOn(Class<A> fromA, Class<B> fromB, Class<C> fromC, Class<D> fromD, Class<E> fromE, VoidStrategy5<RxCache<KA, A>, RxCache<KB, B>, RxCache<KC, C>, RxCache<KD, D>, RxCache<KE, E>> strategy) {
        return runComposer.runOn(fromA, fromB, fromC, fromD, fromE, strategy);
    }

    // -----------------------------------------------------------
    // Act on key
    // -----------------------------------------------------------

    public <KA, A> RunComposerApi runOnKey(Class<A> from, KA keyForA, VoidStrategy2<KA, Optional<A>> strategy) {
        return runComposer.runOnKey(from, keyForA, strategy);
    }

    public <KA, A, KB, B> RunComposerApi runOnKeys(Class<A> fromA, Class<B> fromB, KA keyA, KB keyB, VoidStrategy4<KA, Optional<A>, KB, Optional<B>> strategy) {
        return runComposer.runOnKeys(fromA, fromB, keyA, keyB, strategy);
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
    public RunComposerApi withCommandPolicy(CommandPolicy commandPolicy) {
        state.commandPolicyBuilder.replace(commandPolicy);
        return this;
    }

    @Override
    public RunComposerApi withAttempt(Attempt attempt) {
        state.commandPolicyBuilder.withAttempt(attempt);
        return this;
    }

    @Override
    public RunComposerApi withTimeout(Timeout timeout) {
        state.commandPolicyBuilder.withTimeout(timeout);
        return this;
    }

    @Override
    public RunComposerApi withInterval(Interval interval) {
        state.commandPolicyBuilder.withInterval(interval);
        return this;
    }

    @Override
    public RunComposerApi withRetryInterval(Interval retryInterval) {
        state.commandPolicyBuilder.withRetryInterval(retryInterval);
        return this;
    }

    @Override
    public RunComposerApi withCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
        state.commandPolicyBuilder.withCircuitBreakerPolicy(circuitBreakerPolicy);
        return this;
    }

    @Override
    public RunComposerApi withCircuitBreakerId(CircuitId circuitBreakerId) {
        state.circuitBreakerId.set(circuitBreakerId);
        return this;
    }

    @Override
    public RunComposerApi withRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy) {
        state.commandPolicyBuilder.withRateLimiterPolicy(rateLimiterPolicy);
        return this;
    }

    @Override
    public RunComposerApi withRateLimiterId(RateLimiterId rateLimiterId) {
        state.rateLimiterId.set(requireNonNull(rateLimiterId));
        return this;
    }

    @Override
    public RunComposerApi withSuccessCriterion(Criterion successCriterion) {
        state.commandPolicyBuilder.withSuccessCriterion(successCriterion);
        return this;
    }

    // -----------------------------------------------------------
    // Compose actor function
    // -----------------------------------------------------------

    @Override
    public RunComposerApi fallbackRun(VoidStrategy0 actor) {
        return fallbackComposer.run(actor);
    }

    @Override
    public FunctionRunComposer withDetails() {
        return runComposer;
    }

    @Override
    public FunctionRunComposer withFallbackDetails() {
        return fallbackComposer;
    }

    // -----------------------------------------------------------
    // build act
    // -----------------------------------------------------------

    @Override
    public StreamerGroup build() {
        Validate.assertTrue(!runComposer.isEmpty());
        return fallbackComposer.isEmpty()
                ?
                state.group.addActor(
                        CacheHandle.voidHandle(),
                        state.circuitBreakerId.get(),
                        state.rateLimiterId.get(),
                        StreamerGroup.createVoidCommand(runComposer.getRun()),
                        Optional.of(runComposer.getRxLambdaSubject()),
                        state.actPolicy,
                        state.commandPolicyBuilder.build()
                )
                :
                state.group.addActor(
                        CacheHandle.voidHandle(),
                        state.circuitBreakerId.get(),
                        state.rateLimiterId.get(),
                        StreamerGroup.createVoidCommand(runComposer.getRun()),
                        StreamerGroup.createVoidCommand(fallbackComposer.getRun()),
                        Optional.of(runComposer.getRxLambdaSubject()),
                        Optional.of(fallbackComposer.getRxLambdaSubject()),
                        state.actPolicy,
                        state.commandPolicyBuilder.build()
                );
    }
}
