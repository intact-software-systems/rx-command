package com.intact.rx.api;

public interface RxKeyValueObserver<K, V> {

    void onComplete(K key);

    void onError(K key, Throwable throwable);

    void onNext(K key, V value);

    void onSubscribe(K key, Subscription subscription);
}
