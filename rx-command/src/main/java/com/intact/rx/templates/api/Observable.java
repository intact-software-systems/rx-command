package com.intact.rx.templates.api;

public interface Observable<T> {
    boolean connect(T observer);

    boolean disconnect(T observer);

    void disconnectAll();
}
