package com.intact.rx.api.subject;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.templates.api.Observable;

public class RxSubject<T> implements
        RxObserver<T>,
        Observable<RxObserver<T>> {
    private static final Logger log = LoggerFactory.getLogger(RxSubject.class);
    private final Map<RxObserver<T>, RxObserver<T>> observers = new ConcurrentHashMap<>();

    @Override
    public void onComplete() {
        try {
            observers.values().forEach(RxObserver::onComplete);
        } catch (RuntimeException e) {
            log.warn("Exception caught when performing callback", e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        for (RxObserver<T> observer : observers.values()) {
            try {
                observer.onError(throwable);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        }
    }

    @Override
    public void onNext(T value) {
        for (RxObserver<T> observer : observers.values()) {
            try {
                observer.onNext(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        observers.values().forEach(obs -> {
            try {
                obs.onSubscribe(subscription);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        });
    }

    @Override
    public boolean connect(RxObserver<T> observer) {
        requireNonNull(observer);
        observers.put(observer, observer);
        return true;
    }

    @Override
    public boolean disconnect(RxObserver<T> observer) {
        requireNonNull(observer);
        return observers.remove(observer) != null;
    }

    @Override
    public void disconnectAll() {
        observers.clear();
    }

    public Collection<RxObserver<T>> observers() {
        return observers.values();
    }
}
