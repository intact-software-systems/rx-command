package com.intact.rx.api.cache.observer;

public interface MementoObserver<T> {
    void onUndo(T value);

    void onRedo(T value);
}
