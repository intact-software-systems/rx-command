package com.intact.rx.core.rxcircuit.rate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcircuit.RateLimiterObserver;

@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "PublicMethodNotExposedInInterface"})
public class RateLimiterSubject implements RateLimiterObserver {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterSubject.class);

    private final Map<VoidStrategy1<RateLimiterId>, VoidStrategy1<RateLimiterId>> onViolatedFrequencyFunctions = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<RateLimiterId>, VoidStrategy1<RateLimiterId>> onViolatedQuotaFunctions = new ConcurrentHashMap<>();

    @Override
    public void onViolatedFrequency(RateLimiterId rateLimiterId) {
        onViolatedFrequencyFunctions
                .forEach((key, s) -> {
                            try {
                                s.perform(rateLimiterId);
                            } catch (RuntimeException e) {
                                log.warn("Exception caught when performing callback", e);
                            }
                        }
                );
    }

    @Override
    public void onViolatedQuota(RateLimiterId rateLimiterId) {
        onViolatedQuotaFunctions
                .forEach((key, s) -> {
                            try {
                                s.perform(rateLimiterId);
                            } catch (RuntimeException e) {
                                log.warn("Exception caught when performing callback", e);
                            }
                        }
                );
    }

    public void disconnectAll() {
        onViolatedFrequencyFunctions.clear();
        onViolatedQuotaFunctions.clear();
    }

    public RateLimiterSubject onViolatedFrequencyDo(VoidStrategy1<RateLimiterId> onViolatedFrequency) {
        requireNonNull(onViolatedFrequency);
        onViolatedFrequencyFunctions.put(onViolatedFrequency, onViolatedFrequency);
        return this;
    }

    public RateLimiterSubject onViolatedQuotaDo(VoidStrategy1<RateLimiterId> onViolatedQuota) {
        requireNonNull(onViolatedQuota);
        onViolatedQuotaFunctions.put(onViolatedQuota, onViolatedQuota);
        return this;
    }
}
