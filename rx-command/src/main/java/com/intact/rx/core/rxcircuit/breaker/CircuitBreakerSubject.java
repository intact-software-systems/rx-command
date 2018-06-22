package com.intact.rx.core.rxcircuit.breaker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcircuit.CircuitBreakerObserver;

@SuppressWarnings({"WeakerAccess", "PublicMethodNotExposedInInterface", "UnusedReturnValue"})
public class CircuitBreakerSubject implements CircuitBreakerObserver {
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerSubject.class);

    private final Map<VoidStrategy1<CircuitId>, VoidStrategy1<CircuitId>> openFunctions = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<CircuitId>, VoidStrategy1<CircuitId>> closeFunctions = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<CircuitId>, VoidStrategy1<CircuitId>> halfOpenFunctions = new ConcurrentHashMap<>();

    @Override
    public void onOpen(CircuitId handle) {
        openFunctions.forEach((key, s) -> {
            try {
                s.perform(handle);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        });
    }

    @Override
    public void onClose(CircuitId handle) {
        closeFunctions.forEach((key, s) -> {
            try {
                s.perform(handle);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        });
    }

    @Override
    public void onHalfOpen(CircuitId handle) {
        halfOpenFunctions.forEach((key, s) -> {
            try {
                s.perform(handle);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        });
    }

    public void disconnectAll() {
        openFunctions.clear();
        closeFunctions.clear();
        halfOpenFunctions.clear();
    }

    public CircuitBreakerSubject onOpenDo(VoidStrategy1<CircuitId> onOpen) {
        requireNonNull(onOpen);
        openFunctions.put(onOpen, onOpen);
        return this;
    }

    public CircuitBreakerSubject onCloseDo(VoidStrategy1<CircuitId> onClose) {
        requireNonNull(onClose);
        closeFunctions.put(onClose, onClose);
        return this;
    }

    public CircuitBreakerSubject onHalfOpenDo(VoidStrategy1<CircuitId> onHalfOpen) {
        requireNonNull(onHalfOpen);
        halfOpenFunctions.put(onHalfOpen, onHalfOpen);
        return this;
    }
}
