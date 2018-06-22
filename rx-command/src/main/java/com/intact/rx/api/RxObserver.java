package com.intact.rx.api;

public interface RxObserver<T> {
    void onSubscribe(Subscription subscription);

    void onComplete();

    void onError(Throwable throwable);

    void onNext(T value);
}
