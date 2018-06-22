package com.intact.rx.exception;

public class RateLimitViolatedException extends RuntimeException {
    public RateLimitViolatedException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 1986698870775655015L;
}
