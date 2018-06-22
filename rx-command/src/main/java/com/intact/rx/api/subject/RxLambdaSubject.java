package com.intact.rx.api.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.templates.api.RxObserverSupport;

public class RxLambdaSubject<T> implements
        RxObserver<T>,
        RxObserverSupport<RxLambdaSubject<T>, T> {
    private static final Logger log = LoggerFactory.getLogger(RxLambdaSubject.class);

    private final Map<VoidStrategy0, VoidStrategy0> completedFunctions = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<Throwable>, VoidStrategy1<Throwable>> errorFunctions = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<T>, VoidStrategy1<T>> nextFunctions = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<Subscription>, VoidStrategy1<Subscription>> subscribeFunctions = new ConcurrentHashMap<>();

    @Override
    public void onComplete() {
        try {
            completedFunctions.values().forEach(VoidStrategy0::perform);
        } catch (RuntimeException e) {
            log.warn("Exception caught when performing callback", e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        for (VoidStrategy1<Throwable> observer : errorFunctions.values()) {
            try {
                observer.perform(throwable);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        }
    }

    @Override
    public void onNext(T value) {
        for (VoidStrategy1<T> observer : nextFunctions.values()) {
            try {
                observer.perform(value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscribeFunctions.values().forEach(obs -> {
            try {
                obs.perform(subscription);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        });
    }

    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public void disconnectAll() {
        completedFunctions.clear();
        errorFunctions.clear();
        nextFunctions.clear();
        subscribeFunctions.clear();
    }

    @Override
    public RxLambdaSubject<T> onCompleteDo(VoidStrategy0 completedFunction) {
        if (completedFunction != null) {
            completedFunctions.put(completedFunction, completedFunction);
        }
        return this;
    }

    @Override
    public RxLambdaSubject<T> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        if (errorFunction != null) {
            errorFunctions.put(errorFunction, errorFunction);
        }
        return this;
    }

    @Override
    public RxLambdaSubject<T> onNextDo(VoidStrategy1<T> nextFunction) {
        if (nextFunction != null) {
            nextFunctions.put(nextFunction, nextFunction);
        }
        return this;
    }

    @Override
    public RxLambdaSubject<T> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        if (subscribeFunction != null) {
            subscribeFunctions.put(subscribeFunction, subscribeFunction);
        }
        return this;
    }
}
