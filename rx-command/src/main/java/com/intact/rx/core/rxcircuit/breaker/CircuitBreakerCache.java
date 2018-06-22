package com.intact.rx.core.rxcircuit.breaker;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.rxcircuit.CircuitBreakerObserver;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.context.ObjectRootPolicy;
import com.intact.rx.policy.Extension;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.ResourceLimits;

/**
 * Add a monitor that proactively checks the circuits?
 */
public class CircuitBreakerCache implements CircuitBreakerObserver {
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerCache.class);

    private static final CircuitBreakerSubject circuitBreakerSubject = new CircuitBreakerSubject();
    private static final CircuitBreakerCache instance = new CircuitBreakerCache();
    private static final CachePolicy circuitBreakerCachePolicy =
            CachePolicy.create(
                    CacheMasterPolicy.validForever(),
                    DataCachePolicy.leastRecentlyUsedAnd(
                            ResourceLimits.unlimited(),
                            Lifetime.forever(),
                            ObjectRootPolicy.create(Lifetime.ofHours(1), Extension.renewOnRead())
                    )
            );


    public static CircuitBreakerSubject observeAll() {
        return circuitBreakerSubject;
    }

    public static CircuitBreaker circuit(CircuitId circuitId, CircuitBreakerPolicy circuitBreakerPolicy) {
        if (circuitId.isNone() || circuitBreakerPolicy.isUnlimited()) {
            return CircuitBreakerAlwaysAllow.instance;
        }

        return cache(circuitId.getCacheHandle())
                .computeIfAbsent(
                        circuitId,
                        id -> {
                            CircuitBreaker circuitBreaker = new RxCircuitBreaker(circuitId, circuitBreakerPolicy);
                            circuitBreaker
                                    .onOpenDo(instance::onOpen)
                                    .onCloseDo(instance::onClose)
                                    .onHalfOpenDo(instance::onHalfOpen);
                            return circuitBreaker;
                        }
                );
    }

    public static Optional<CircuitBreaker> find(CircuitId circuitId) {
        return cache(circuitId.getCacheHandle()).read(circuitId);
    }

    private static RxCache<CircuitId, CircuitBreaker> cache(CacheHandle handle) {
        return RxCacheAccess.cache(handle, circuitBreakerCachePolicy);
    }

    // --------------------------------------------
    // Interface CircuitBreakerObserver
    // --------------------------------------------

    @Override
    public void onOpen(CircuitId handle) {
        log.info("Circuit OPEN: {}", handle);
        circuitBreakerSubject.onOpen(handle);
    }

    @Override
    public void onClose(CircuitId handle) {
        log.info("Circuit CLOSE: {}", handle);
        circuitBreakerSubject.onClose(handle);
    }

    @Override
    public void onHalfOpen(CircuitId handle) {
        log.info("Circuit HALF-OPEN: {}", handle);
        circuitBreakerSubject.onHalfOpen(handle);
    }
}
