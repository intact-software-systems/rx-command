package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.ObjectTypeObserver;
import com.intact.rx.api.command.VoidStrategy1;

public class ObjectTypeLambdaSubject<V> implements ObjectTypeObserver<V> {
    private static final Logger log = LoggerFactory.getLogger(ObjectTypeLambdaSubject.class);

    private final Map<VoidStrategy1<V>, VoidStrategy1<V>> created = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<V>, VoidStrategy1<V>> removed = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<V>, VoidStrategy1<V>> modified = new ConcurrentHashMap<>();

    @Override
    public void onObjectCreated(V value) {
        callbackOn(value, created);
    }

    @Override
    public void onObjectRemoved(V value) {
        callbackOn(value, removed);
    }

    @Override
    public void onObjectModified(V value) {
        callbackOn(value, modified);
    }

    public ObjectTypeLambdaSubject<V> onObjectCreatedDo(VoidStrategy1<V> strategy) {
        created.put(strategy, strategy);
        return this;
    }

    public ObjectTypeLambdaSubject<V> onObjectRemovedDo(VoidStrategy1<V> strategy) {
        removed.put(strategy, strategy);
        return this;
    }

    public ObjectTypeLambdaSubject<V> onObjectModifiedDo(VoidStrategy1<V> strategy) {
        modified.put(strategy, strategy);
        return this;
    }

    public void disconnectAll() {
        created.clear();
        removed.clear();
        modified.clear();
    }

    private static <V> void callbackOn(V value, Map<VoidStrategy1<V>, VoidStrategy1<V>> observers) {
        for (VoidStrategy1<V> strategy : observers.values()) {
            try {
                strategy.perform(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", strategy, e);
            }
        }
    }
}
