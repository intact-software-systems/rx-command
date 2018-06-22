package com.intact.rx.api.rxcircuit;

import com.intact.rx.core.rxcircuit.rate.RateLimiterId;

public interface RateLimiterObserver {
    void onViolatedFrequency(RateLimiterId rateLimiterId);

    void onViolatedQuota(RateLimiterId rateLimiterId);
}
