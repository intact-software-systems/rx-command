package com.intact.rx.core.rxcache.api;

import java.util.Map;

import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.rxcache.StreamerGroup;
import com.intact.rx.core.rxcache.factory.FunctionComposer;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.*;

public interface GetComposerApi<K, V> {

    GetComposerApi<K, V> withCachedObjectLifetime(Lifetime lifetime);

    GetComposerApi<K, V> withCacheResourceLimits(ResourceLimits resourceLimits);

    GetComposerApi<K, V> withCommandPolicy(CommandPolicy commandPolicy);

    GetComposerApi<K, V> withAttempt(Attempt attempt);

    GetComposerApi<K, V> withTimeout(Timeout timeout);

    GetComposerApi<K, V> withInterval(Interval interval);

    GetComposerApi<K, V> withRetryInterval(Interval retryInterval);

    GetComposerApi<K, V> withCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy);

    GetComposerApi<K, V> withCircuitBreakerId(CircuitId circuitId);

    GetComposerApi<K, V> withRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy);

    GetComposerApi<K, V> withRateLimiterId(RateLimiterId rateLimiterId);

    GetComposerApi<K, V> withSuccessCriterion(Criterion successCriterion);

    GetComposerApi<K, V> fallbackGet(Strategy0<Map<K, V>> actor);

    FunctionComposer<K, V> withDetails();

    FunctionComposer<K, V> withFallbackDetails();

    RxStreamer done();

    RxStreamer subscribe();

    StreamerGroup parallel();

    StreamerGroup sequential();

    StreamerGroup build();
}
