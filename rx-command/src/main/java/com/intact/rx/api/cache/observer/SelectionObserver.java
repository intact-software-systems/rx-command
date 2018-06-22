package com.intact.rx.api.cache.observer;

public interface SelectionObserver<T> {

    void onObjectIn(T value);

    void onObjectOut(T value);

    void onObjectModified(T value);

    void onDetach();
}
