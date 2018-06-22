package com.intact.rx.core.rxcache.api;

import java.util.Collection;

import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.rxcache.StreamerGroup;
import com.intact.rx.core.rxcache.factory.FunctionValueComposer;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.*;

public interface ValueComposerApi<V> {

    ValueComposerApi<V> withCachedObjectLifetime(Lifetime lifetime);

    ValueComposerApi<V> withCacheResourceLimits(ResourceLimits resourceLimits);

    ValueComposerApi<V> withCommandPolicy(CommandPolicy commandPolicy);

    ValueComposerApi<V> withAttempt(Attempt attempt);

    ValueComposerApi<V> withTimeout(Timeout timeout);

    ValueComposerApi<V> withInterval(Interval interval);

    ValueComposerApi<V> withRetryInterval(Interval retryInterval);

    ValueComposerApi<V> withCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy);

    ValueComposerApi<V> withCircuitBreakerId(CircuitId circuitId);

    ValueComposerApi<V> withRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy);

    ValueComposerApi<V> withRateLimiterId(RateLimiterId rateLimiterId);

    ValueComposerApi<V> withSuccessCriterion(Criterion successCriterion);

    ValueComposerApi<V> fallbackGet(Strategy0<Collection<V>> actor);

    FunctionValueComposer<V> withDetails();

    FunctionValueComposer<V> withFallbackDetails();

    RxStreamer done();

    RxStreamer subscribe();

    StreamerGroup parallel();

    StreamerGroup sequential();

    StreamerGroup build();
}
