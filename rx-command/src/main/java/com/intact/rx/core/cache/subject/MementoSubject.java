package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.MementoObserver;
import com.intact.rx.api.command.VoidStrategy1;

public class MementoSubject<K, V> implements MementoObserver<Map.Entry<K, V>> {
    private static final Logger log = LoggerFactory.getLogger(MementoSubject.class);

    private final Map<VoidStrategy1<Map.Entry<K, V>>, VoidStrategy1<Map.Entry<K, V>>> undo = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<Map.Entry<K, V>>, VoidStrategy1<Map.Entry<K, V>>> redo = new ConcurrentHashMap<>();

    @Override
    public void onUndo(Map.Entry<K, V> value) {
        callbackOn(value, undo);
    }

    @Override
    public void onRedo(Map.Entry<K, V> value) {
        callbackOn(value, redo);
    }

    public MementoSubject<K, V> onUndoDo(VoidStrategy1<Map.Entry<K, V>> strategy) {
        undo.put(strategy, strategy);
        return this;
    }

    public MementoSubject<K, V> onRedoDo(VoidStrategy1<Map.Entry<K, V>> strategy) {
        redo.put(strategy, strategy);
        return this;
    }

    public void disconnectAll() {
        undo.clear();
        redo.clear();
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
