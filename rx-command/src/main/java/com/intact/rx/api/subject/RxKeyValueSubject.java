package com.intact.rx.api.subject;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.templates.api.Observable;

@SuppressWarnings("WeakerAccess")
public class RxKeyValueSubject<K, V> implements RxKeyValueObserver<K, V>, Observable<RxKeyValueObserver<K, V>> {
    private final Map<RxKeyValueObserver<K, V>, RxKeyValueObserver<K, V>> observers = new ConcurrentHashMap<>();

    @Override
    public void onComplete(K key) {
        observers.values().forEach(obs -> obs.onComplete(key));
    }

    @Override
    public void onError(K key, Throwable throwable) {
        observers.values().forEach(obs -> obs.onError(key, throwable));
    }

    @Override
    public void onNext(K key, V value) {
        observers.values().forEach(obs -> obs.onNext(key, value));
    }

    @Override
    public void onSubscribe(K key, Subscription subscription) {
        observers.values().forEach(obs -> obs.onSubscribe(key, subscription));
    }

    @Override
    public boolean connect(RxKeyValueObserver<K, V> observer) {
        requireNonNull(observer);
        observers.put(observer, observer);
        return true;
    }

    @Override
    public boolean disconnect(RxKeyValueObserver<K, V> observer) {
        requireNonNull(observer);
        return observers.remove(observer) != null;
    }

    @Override
    public void disconnectAll() {
        observers.clear();
    }

    public Collection<RxKeyValueObserver<K, V>> observers() {
        return observers.values();
    }

    public RxKeyValueSubject<K, V> makeCopy() {
        RxKeyValueSubject<K, V> subject = new RxKeyValueSubject<>();
        subject.observers.putAll(this.observers);
        return subject;
    }
}
