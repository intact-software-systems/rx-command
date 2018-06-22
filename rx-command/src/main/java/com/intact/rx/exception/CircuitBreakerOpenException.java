package com.intact.rx.exception;

public final class CircuitBreakerOpenException extends RuntimeException {

    public CircuitBreakerOpenException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -2128704892439325950L;
}
