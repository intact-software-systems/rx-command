package com.intact.rx.exception;

public class ExecutionEndedWithNoResultException extends RuntimeException {

    public ExecutionEndedWithNoResultException(String s) {
        super(s);
    }

    ExecutionEndedWithNoResultException(String s, Throwable exception) {
        super(s, exception);
    }

    private static final long serialVersionUID = -4083837795830053158L;
}
