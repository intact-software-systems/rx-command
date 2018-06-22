package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.ObjectObserver;
import com.intact.rx.templates.api.Observable;

public class ObjectSubject<K, V> implements
        ObjectObserver<K, V>,
        Observable<ObjectObserver<K, V>> {
    private static final Logger log = LoggerFactory.getLogger(ObjectSubject.class);
    private final Map<ObjectObserver<K, V>, ObjectObserver<K, V>> observers = new ConcurrentHashMap<>();

    @Override
    public void onObjectCreated(K key, V value) {
        for (ObjectObserver<K, V> observer : observers.values()) {
            try {
                observer.onObjectCreated(key, value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", key, e);
            }
        }
    }

    @Override
    public void onObjectRemoved(K key, V value) {
        for (ObjectObserver<K, V> observer : observers.values()) {
            try {
                observer.onObjectRemoved(key, value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", key, e);
            }

        }
    }

    @Override
    public void onObjectModified(K key, V value) {
        for (ObjectObserver<K, V> observer : observers.values()) {
            try {
                observer.onObjectModified(key, value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", key, e);
            }
        }
    }

    @Override
    public boolean connect(ObjectObserver<K, V> observer) {
        requireNonNull(observer);
        observers.put(observer, observer);
        return true;
    }

    @Override
    public boolean disconnect(ObjectObserver<K, V> observer) {
        requireNonNull(observer);
        return observers.remove(observer) != null;
    }

    @Override
    public void disconnectAll() {
        observers.clear();
    }
}
