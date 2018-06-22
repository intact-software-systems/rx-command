package com.intact.rx.core.rxcircuit.breaker;

import com.intact.rx.api.command.VoidStrategy1;

/*
From AKKA doc:
- During normal operation, a circuit breaker is in the Closed state:
    Exceptions or calls exceeding the configured callTimeout increment a failure counter
    Successes reset the failure count to zero
    When the failure counter reaches a maxFailures count, the breaker is tripped into Open state
- While in Open state:
    All calls fail-fast with a CircuitBreakerOpenException
    After the configured resetTimeout, the circuit breaker enters a Half-Open state
- In Half-Open state:
    The first call attempted is allowed through without failing fast
    All other calls fail-fast with an exception just as in Open state
    If the first call succeeds, the breaker is reset back to Closed state
    If the first call fails, the breaker is tripped again into the Open state for another full resetTimeout
- State transition listeners:
    Callbacks can be provided for every state entry via onOpen, onClose, and onHalfOpen
    These are executed in the ExecutionContext provided
*/
public interface CircuitBreaker {
    /**
     * Successes reset the failure count to zero
     */
    void success();

    /**
     * Exceptions or calls exceeding the configured callTimeout increment a failure counter, controlled externally.
     */
    void failure();

    /**
     * @return true if request is allowed according to current status and configured policy. Mutating function.
     */
    boolean allowRequest();

    /**
     * @return true if circuit is in open state
     */
    boolean isOpen();

    /**
     * @return true if circuit is in half open state
     */
    boolean isHalfOpen();

    /**
     * @return true if circuit is in close state
     */
    boolean isClosed();

    /**
     * @param onOpen callback
     * @return this
     */
    CircuitBreaker onOpenDo(VoidStrategy1<CircuitId> onOpen);

    /**
     * @param onClose callback
     * @return this
     */
    CircuitBreaker onCloseDo(VoidStrategy1<CircuitId> onClose);

    /**
     * @param onHalfOpen callback
     * @return this
     */
    CircuitBreaker onHalfOpenDo(VoidStrategy1<CircuitId> onHalfOpen);
}
