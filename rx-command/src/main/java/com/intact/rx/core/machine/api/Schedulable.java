package com.intact.rx.core.machine.api;

public interface Schedulable<T> extends Runnable {
    boolean hasNext();

    T next();
}
