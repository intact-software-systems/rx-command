package com.intact.rx.exception;

public class ExecutionNotStartedException extends RuntimeException {

    public ExecutionNotStartedException(String s) {
        super(s);
    }

    ExecutionNotStartedException(String s, Throwable exception) {
        super(s, exception);
    }

    private static final long serialVersionUID = -4083837795830053158L;
}
