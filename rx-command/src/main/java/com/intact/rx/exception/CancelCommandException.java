package com.intact.rx.exception;

public class CancelCommandException extends RuntimeException {
    public CancelCommandException(String s) {
        super(s);
    }

    private static final long serialVersionUID = -5762705541578454597L;
}
