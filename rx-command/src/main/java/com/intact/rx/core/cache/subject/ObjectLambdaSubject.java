package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.ObjectObserver;
import com.intact.rx.api.command.VoidStrategy2;

public class ObjectLambdaSubject<K, V> implements
        ObjectObserver<K, V> {
    private static final Logger log = LoggerFactory.getLogger(ObjectLambdaSubject.class);

    private final Map<VoidStrategy2<K, V>, VoidStrategy2<K, V>> created = new ConcurrentHashMap<>();
    private final Map<VoidStrategy2<K, V>, VoidStrategy2<K, V>> removed = new ConcurrentHashMap<>();
    private final Map<VoidStrategy2<K, V>, VoidStrategy2<K, V>> modified = new ConcurrentHashMap<>();

    @Override
    public void onObjectCreated(K key, V value) {
        callbackOn(key, value, created);
    }

    @Override
    public void onObjectRemoved(K key, V value) {
        callbackOn(key, value, removed);
    }

    @Override
    public void onObjectModified(K key, V value) {
        callbackOn(key, value, modified);
    }

    public ObjectLambdaSubject<K, V> onObjectCreatedDo(VoidStrategy2<K, V> strategy) {
        created.put(strategy, strategy);
        return this;
    }

    public ObjectLambdaSubject<K, V> onObjectRemovedDo(VoidStrategy2<K, V> strategy) {
        removed.put(strategy, strategy);
        return this;
    }

    public ObjectLambdaSubject<K, V> onObjectModifiedDo(VoidStrategy2<K, V> strategy) {
        modified.put(strategy, strategy);
        return this;
    }

    public void disconnectAll() {
        created.clear();
        removed.clear();
        modified.clear();
    }

    private void callbackOn(K key, V value, Map<VoidStrategy2<K, V>, VoidStrategy2<K, V>> observers) {
        for (VoidStrategy2<K, V> strategy : observers.values()) {
            try {
                strategy.perform(key, value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", key, e);
            }
        }
    }
}
