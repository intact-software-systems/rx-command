package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.SelectionObserver;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;

@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public class SelectionLambdaSubject<T> implements SelectionObserver<T> {
    private static final Logger log = LoggerFactory.getLogger(SelectionLambdaSubject.class);

    private final Map<VoidStrategy1<T>, VoidStrategy1<T>> in = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<T>, VoidStrategy1<T>> out = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<T>, VoidStrategy1<T>> modified = new ConcurrentHashMap<>();
    private final Map<VoidStrategy0, VoidStrategy0> detach = new ConcurrentHashMap<>();

    @Override
    public void onObjectIn(T value) {
        for (VoidStrategy1<T> strategy : in.values()) {
            try {
                strategy.perform(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", value, e);
            }
        }
    }

    @Override
    public void onObjectOut(T value) {
        for (VoidStrategy1<T> strategy : out.values()) {
            try {
                strategy.perform(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", value, e);
            }
        }
    }

    @Override
    public void onObjectModified(T value) {
        for (VoidStrategy1<T> strategy : modified.values()) {
            try {
                strategy.perform(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", value, e);
            }
        }
    }

    @Override
    public void onDetach() {
        try {
            detach.values().forEach(VoidStrategy0::perform);
        } catch (RuntimeException e) {
            log.warn("Exception caught when performing callback onDetach ", e);
        }
    }

    public SelectionLambdaSubject<T> onObjectInDo(VoidStrategy1<T> strategy) {
        in.put(strategy, strategy);
        return this;
    }

    public SelectionLambdaSubject<T> onObjectOutDo(VoidStrategy1<T> strategy) {
        out.put(strategy, strategy);
        return this;
    }

    public SelectionLambdaSubject<T> onObjectModifiedDo(VoidStrategy1<T> strategy) {
        modified.put(strategy, strategy);
        return this;
    }

    public SelectionLambdaSubject<T> onDetachDo(VoidStrategy0 strategy) {
        detach.put(strategy, strategy);
        return this;
    }

    public void disconnectAll() {
        in.clear();
        out.clear();
        modified.clear();
        detach.clear();
    }
}
