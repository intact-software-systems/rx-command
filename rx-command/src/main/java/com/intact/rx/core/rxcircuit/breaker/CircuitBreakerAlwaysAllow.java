package com.intact.rx.core.rxcircuit.breaker;

import com.intact.rx.api.command.VoidStrategy1;

@SuppressWarnings("WeakerAccess")
public class CircuitBreakerAlwaysAllow implements CircuitBreaker {
    public static final CircuitBreakerAlwaysAllow instance = new CircuitBreakerAlwaysAllow();

    @Override
    public void success() {
    }

    @Override
    public void failure() {
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isHalfOpen() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    @Override
    public boolean allowRequest() {
        return true;
    }

    @Override
    public CircuitBreaker onOpenDo(VoidStrategy1<CircuitId> onOpen) {
        return this;
    }

    @Override
    public CircuitBreaker onCloseDo(VoidStrategy1<CircuitId> onClose) {
        return this;
    }

    @Override
    public CircuitBreaker onHalfOpenDo(VoidStrategy1<CircuitId> onHalfOpen) {
        return this;
    }
}
