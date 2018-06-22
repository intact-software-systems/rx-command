package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.SelectionObserver;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.templates.api.Observable;

@SuppressWarnings("UnusedReturnValue")
public class SelectionSubject<T> implements SelectionObserver<T>, Observable<SelectionObserver<T>> {
    private static final Logger log = LoggerFactory.getLogger(SelectionSubject.class);

    private final Map<SelectionObserver<T>, SelectionObserver<T>> observers = new ConcurrentHashMap<>();
    private final SelectionLambdaSubject<T> subject = new SelectionLambdaSubject<>();

    @Override
    public void onObjectIn(T value) {
        subject.onObjectIn(value);

        for (SelectionObserver<T> observer : observers.values()) {
            try {
                observer.onObjectIn(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", value, e);
            }
        }
    }

    @Override
    public void onObjectOut(T value) {
        subject.onObjectOut(value);

        for (SelectionObserver<T> observer : observers.values()) {
            try {
                observer.onObjectOut(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", value, e);
            }
        }
    }

    @Override
    public void onObjectModified(T value) {
        subject.onObjectModified(value);

        for (SelectionObserver<T> observer : observers.values()) {
            try {
                observer.onObjectModified(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", value, e);
            }
        }
    }

    @Override
    public void onDetach() {
        subject.onDetach();

        try {
            //noinspection RedundantTypeArguments
            observers.values().forEach(SelectionObserver<T>::onDetach);
        } catch (RuntimeException e) {
            log.warn("Exception caught when performing callback onDetach ", e);
        }
    }

    @Override
    public boolean connect(SelectionObserver<T> observer) {
        requireNonNull(observer);
        observers.put(observer, observer);
        return true;
    }

    @Override
    public boolean disconnect(SelectionObserver<T> observer) {
        requireNonNull(observer);
        return observers.remove(observer) != null;
    }

    @Override
    public void disconnectAll() {
        observers.clear();
        subject.disconnectAll();
    }

    public SelectionSubject<T> onObjectInDo(VoidStrategy1<T> strategy) {
        subject.onObjectInDo(strategy);
        return this;
    }

    public SelectionSubject<T> onObjectOutDo(VoidStrategy1<T> strategy) {
        subject.onObjectOutDo(strategy);
        return this;
    }

    public SelectionSubject<T> onObjectModifiedDo(VoidStrategy1<T> strategy) {
        subject.onObjectModifiedDo(strategy);
        return this;
    }

    public SelectionSubject<T> onDetachDo(VoidStrategy0 strategy) {
        subject.onDetachDo(strategy);
        return this;
    }
}
