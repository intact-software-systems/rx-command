package com.intact.rx.api.subject;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.templates.api.Observable;
import com.intact.rx.templates.api.RxObserverSupport;

public class RxSubjectCombi<T> implements
        RxObserver<T>,
        RxObserverSupport<RxSubjectCombi<T>, T>,
        Observable<RxObserver<T>> {
    private final RxSubject<T> observers = new RxSubject<>();
    private final RxLambdaSubject<T> lambdas = new RxLambdaSubject<>();

    @Override
    public void onComplete() {
        observers.onComplete();
        lambdas.onComplete();
    }

    @Override
    public void onError(Throwable throwable) {
        observers.onError(throwable);
        lambdas.onError(throwable);
    }

    @Override
    public void onNext(T value) {
        observers.onNext(value);
        lambdas.onNext(value);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        observers.onSubscribe(subscription);
        lambdas.onSubscribe(subscription);
    }

    @Override
    public boolean connect(RxObserver<T> observer) {
        return observers.connect(observer);
    }

    @Override
    public boolean disconnect(RxObserver<T> observer) {
        return observers.disconnect(observer);
    }

    @Override
    public void disconnectAll() {
        observers.disconnectAll();
        lambdas.disconnectAll();
    }

    @Override
    public RxSubjectCombi<T> onCompleteDo(VoidStrategy0 completedFunction) {
        lambdas.onCompleteDo(completedFunction);
        return this;
    }

    @Override
    public RxSubjectCombi<T> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        lambdas.onErrorDo(errorFunction);
        return this;
    }

    @Override
    public RxSubjectCombi<T> onNextDo(VoidStrategy1<T> nextFunction) {
        lambdas.onNextDo(nextFunction);
        return this;
    }

    @Override
    public RxSubjectCombi<T> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        lambdas.onSubscribeDo(subscribeFunction);
        return this;
    }
}
