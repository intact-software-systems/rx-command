package com.intact.rx.core.rxcircuit.breaker;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.observer.RemovedFromCacheObserver;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.templates.StatusTrackerTimestamped;

@SuppressWarnings("WeakerAccess")
public class RxCircuitBreaker implements CircuitBreaker, RemovedFromCacheObserver {
    private enum State {
        OPEN, CLOSE, HALF_OPEN
    }

    private final CircuitId circuitId;
    private final CircuitBreakerPolicy circuitBreakerPolicy;
    private final CircuitBreakerSubject circuitBreakerSubject;
    private final AtomicReference<State> currentStatus;
    private final StatusTrackerTimestamped slidingWindowFailures;
    private final AtomicLong timeStampOpen;

    public RxCircuitBreaker(CircuitId circuitId, CircuitBreakerPolicy circuitBreakerPolicy) {
        this.circuitId = requireNonNull(circuitId);
        this.circuitBreakerPolicy = requireNonNull(circuitBreakerPolicy);
        this.circuitBreakerSubject = new CircuitBreakerSubject();
        this.currentStatus = new AtomicReference<>(State.CLOSE);
        this.slidingWindowFailures = new StatusTrackerTimestamped(circuitBreakerPolicy.getErrorWindow());
        this.timeStampOpen = new AtomicLong(Long.MAX_VALUE);
    }

    // --------------------------------------------
    // Interface CircuitBreaker
    // --------------------------------------------

    @Override
    public void success() {
        slidingWindowFailures.reset();
        timeStampOpen.set(Long.MAX_VALUE);

        State previous = currentStatus.getAndSet(State.CLOSE);
        if (previous != State.CLOSE) {
            // this thread set new state, perform callback
            circuitBreakerSubject.onClose(circuitId);
        }
    }

    @Override
    public void failure() {
        slidingWindowFailures.next(1);

        if (slidingWindowFailures.sumInWindow() > circuitBreakerPolicy.getMaxFailures() || // if sum of failures in window > maximum allowed failures in window, then trip circuit OPEN
                currentStatus.get() == State.HALF_OPEN) // the breaker is tripped again into the OPEN state for another full resetTimeout
        {
            State previous = currentStatus.getAndSet(State.OPEN);
            if (previous != State.OPEN) {
                // this thread set new state, perform callback
                circuitBreakerSubject.onOpen(circuitId);
                timeStampOpen.set(System.currentTimeMillis());
            }
        }
    }

    @Override
    public boolean allowRequest() {
        if (currentStatus.get() == State.OPEN) {
            long timeSinceOpened = Math.max(0, System.currentTimeMillis() - timeStampOpen.get());
            if (timeSinceOpened > circuitBreakerPolicy.getResetTimeout().toMillis()) {
                State previous = currentStatus.getAndSet(State.HALF_OPEN);
                if (previous == State.OPEN) {
                    // This thread set circuit to half open and is allowed through
                    circuitBreakerSubject.onHalfOpen(circuitId);
                    return true;
                }
            }
        }

        return currentStatus.get() == State.CLOSE;
    }

    @Override
    public boolean isOpen() {
        return currentStatus.get() == State.OPEN;
    }

    @Override
    public boolean isHalfOpen() {
        return currentStatus.get() == State.HALF_OPEN;
    }

    @Override
    public boolean isClosed() {
        return currentStatus.get() == State.CLOSE;
    }

    @Override
    public CircuitBreaker onOpenDo(VoidStrategy1<CircuitId> onOpen) {
        circuitBreakerSubject.onOpenDo(onOpen);
        return this;
    }

    @Override
    public CircuitBreaker onCloseDo(VoidStrategy1<CircuitId> onClose) {
        circuitBreakerSubject.onCloseDo(onClose);
        return this;
    }

    @Override
    public CircuitBreaker onHalfOpenDo(VoidStrategy1<CircuitId> onHalfOpen) {
        circuitBreakerSubject.onHalfOpenDo(onHalfOpen);
        return this;
    }

    // --------------------------------------------
    // Interface RemovedFromCacheObserver
    // --------------------------------------------

    @Override
    public void onRemovedFromCache() {
        circuitBreakerSubject.disconnectAll();
    }

    @Override
    public String toString() {
        return "RxCircuitBreaker{" +
                "circuitBreakerPolicy=" + circuitBreakerPolicy +
                ", circuitBreakerId=" + circuitId +
                ", circuitBreakerSubject=" + circuitBreakerSubject +
                ", currentStatus=" + currentStatus +
                ", slidingWindowFailures=" + slidingWindowFailures +
                ", timeStampOpen=" + timeStampOpen +
                '}';
    }
}
