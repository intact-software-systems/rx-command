package com.intact.rx.exception;

class WaitForExecutionTimedOutException extends RuntimeException {

    WaitForExecutionTimedOutException(String s, Throwable exception) {
        super(s, exception);
    }

    private static final long serialVersionUID = -4083837795830053158L;
}
