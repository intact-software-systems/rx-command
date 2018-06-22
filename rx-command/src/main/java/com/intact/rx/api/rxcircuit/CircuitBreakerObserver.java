package com.intact.rx.api.rxcircuit;

import com.intact.rx.core.rxcircuit.breaker.CircuitId;

public interface CircuitBreakerObserver {
    void onOpen(CircuitId handle);

    void onClose(CircuitId handle);

    void onHalfOpen(CircuitId handle);
}
