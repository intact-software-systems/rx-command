package com.intact.rx.core.rxcache.api;

import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.rxcache.StreamerGroup;
import com.intact.rx.core.rxcache.factory.FunctionRunComposer;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.Attempt;
import com.intact.rx.policy.Criterion;
import com.intact.rx.policy.Interval;
import com.intact.rx.policy.Timeout;

public interface RunComposerApi {

    RunComposerApi withCommandPolicy(CommandPolicy commandPolicy);

    RunComposerApi withAttempt(Attempt attempt);

    RunComposerApi withTimeout(Timeout timeout);

    RunComposerApi withInterval(Interval interval);

    RunComposerApi withRetryInterval(Interval retryInterval);

    RunComposerApi withCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy);

    RunComposerApi withCircuitBreakerId(CircuitId circuitId);

    RunComposerApi withRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy);

    RunComposerApi withRateLimiterId(RateLimiterId rateLimiterId);

    RunComposerApi withSuccessCriterion(Criterion successCriterion);

    RunComposerApi fallbackRun(VoidStrategy0 actor);

    FunctionRunComposer withDetails();

    FunctionRunComposer withFallbackDetails();

    RxStreamer done();

    RxStreamer subscribe();

    StreamerGroup parallel();

    StreamerGroup sequential();

    StreamerGroup build();
}
