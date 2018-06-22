package com.intact.rx.api.subject;

import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.command.VoidStrategy2;
import com.intact.rx.templates.api.Observable;

public class RxKeyValueSubjectCombi<K, V> implements
        RxKeyValueObserver<K, V>,
        Observable<RxKeyValueObserver<K, V>> {
    private final RxKeyValueSubject<K, V> observers = new RxKeyValueSubject<>();
    private final RxKeyValueLambdaSubject<K, V> lambdas = new RxKeyValueLambdaSubject<>();

    @Override
    public void onComplete(K key) {
        observers.onComplete(key);
        lambdas.onComplete(key);
    }

    @Override
    public void onError(K key, Throwable throwable) {
        observers.onError(key, throwable);
        lambdas.onError(key, throwable);
    }

    @Override
    public void onNext(K key, V value) {
        observers.onNext(key, value);
        lambdas.onNext(key, value);
    }

    @Override
    public void onSubscribe(K key, Subscription subscription) {
        observers.onSubscribe(key, subscription);
        lambdas.onSubscribe(key, subscription);
    }

    @Override
    public boolean connect(RxKeyValueObserver<K, V> observer) {
        return observers.connect(observer);
    }

    @Override
    public boolean disconnect(RxKeyValueObserver<K, V> observer) {
        return observers.disconnect(observer);
    }

    @Override
    public void disconnectAll() {
        observers.disconnectAll();
        lambdas.disconnectAll();
    }

    public RxKeyValueSubjectCombi<K, V> onCompleteDo(VoidStrategy1<K> completedFunction) {
        lambdas.onCompleteDo(completedFunction);
        return this;
    }

    public RxKeyValueSubjectCombi<K, V> onErrorDo(VoidStrategy2<K, Throwable> errorFunction) {
        lambdas.onErrorDo(errorFunction);
        return this;
    }

    public RxKeyValueSubjectCombi<K, V> onNextDo(VoidStrategy2<K, V> nextFunction) {
        lambdas.onNextDo(nextFunction);
        return this;
    }

    public RxKeyValueSubjectCombi<K, V> onSubscribeDo(VoidStrategy1<K> subscribeFunction) {
        lambdas.onSubscribeDo(subscribeFunction);
        return this;
    }
}
