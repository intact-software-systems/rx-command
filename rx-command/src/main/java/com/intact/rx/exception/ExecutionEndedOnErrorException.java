package com.intact.rx.exception;

public class ExecutionEndedOnErrorException extends RuntimeException {

    public ExecutionEndedOnErrorException(String s) {
        super(s);
    }

    public ExecutionEndedOnErrorException(String s, Throwable exception) {
        super(s, exception);
    }

    private static final long serialVersionUID = -4083837795830053158L;
}
