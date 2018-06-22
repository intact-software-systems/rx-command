package com.intact.rx.api.cache.observer;

public interface ObjectTypeObserver<V> {

    void onObjectCreated(V value);

    void onObjectRemoved(V value);

    void onObjectModified(V value);
}
