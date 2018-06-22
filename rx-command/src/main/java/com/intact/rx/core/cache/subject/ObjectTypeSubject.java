package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.ObjectTypeObserver;
import com.intact.rx.templates.api.Observable;

public class ObjectTypeSubject<V> implements
        ObjectTypeObserver<V>,
        Observable<ObjectTypeObserver<V>> {
    private static final Logger log = LoggerFactory.getLogger(ObjectTypeSubject.class);
    private final Map<ObjectTypeObserver<V>, ObjectTypeObserver<V>> observers = new ConcurrentHashMap<>();

    @Override
    public void onObjectCreated(V value) {
        for (ObjectTypeObserver<V> observer : observers.values()) {
            try {
                observer.onObjectCreated(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", value, e);
            }
        }
    }

    @Override
    public void onObjectRemoved(V value) {
        for (ObjectTypeObserver<V> observer : observers.values()) {
            try {
                observer.onObjectRemoved(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", value, e);
            }
        }
    }

    @Override
    public void onObjectModified(V value) {
        for (ObjectTypeObserver<V> observer : observers.values()) {
            try {
                observer.onObjectModified(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", value, e);
            }
        }
    }

    @Override
    public boolean connect(ObjectTypeObserver<V> observer) {
        requireNonNull(observer);
        observers.put(observer, observer);
        return true;
    }

    @Override
    public boolean disconnect(ObjectTypeObserver<V> observer) {
        requireNonNull(observer);
        return observers.remove(observer) != null;
    }

    @Override
    public void disconnectAll() {
        observers.clear();
    }
}
