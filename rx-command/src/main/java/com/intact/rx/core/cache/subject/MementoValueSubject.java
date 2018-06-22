package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.MementoObserver;
import com.intact.rx.api.command.VoidStrategy1;

public class MementoValueSubject<V> implements MementoObserver<V> {
    private static final Logger log = LoggerFactory.getLogger(MementoValueSubject.class);

    private final Map<VoidStrategy1<V>, VoidStrategy1<V>> undo = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<V>, VoidStrategy1<V>> redo = new ConcurrentHashMap<>();
    private final Map<MementoObserver<V>, MementoObserver<V>> observers = new ConcurrentHashMap<>();

    @Override
    public void onUndo(V value) {
        callbackOn(value, undo);

        for (MementoObserver<V> observer : observers.values()) {
            try {
                observer.onUndo(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", observer, e);
            }
        }
    }

    @Override
    public void onRedo(V value) {
        callbackOn(value, redo);

        for (MementoObserver<V> observer : observers.values()) {
            try {
                observer.onRedo(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", observer, e);
            }
        }
    }

    public MementoValueSubject<V> onUndoDo(VoidStrategy1<V> strategy) {
        undo.put(strategy, strategy);
        return this;
    }

    public MementoValueSubject<V> onRedoDo(VoidStrategy1<V> strategy) {
        redo.put(strategy, strategy);
        return this;
    }

    public MementoValueSubject<V> connect(MementoObserver<V> observer) {
        observers.put(observer, observer);
        return this;
    }

    public MementoValueSubject<V> disconnect(MementoObserver<V> mementoObserver) {
        observers.remove(mementoObserver);
        return this;
    }

    public void disconnectAll() {
        undo.clear();
        redo.clear();
        observers.clear();
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
