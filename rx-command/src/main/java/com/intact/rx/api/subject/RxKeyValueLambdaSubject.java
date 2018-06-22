package com.intact.rx.api.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.command.VoidStrategy2;

public class RxKeyValueLambdaSubject<K, V> implements RxKeyValueObserver<K, V> {
    private static final Logger log = LoggerFactory.getLogger(RxKeyValueLambdaSubject.class);

    private final Map<VoidStrategy1<K>, VoidStrategy1<K>> completeFunctions = new ConcurrentHashMap<>();
    private final Map<VoidStrategy2<K, Throwable>, VoidStrategy2<K, Throwable>> errorFunctions = new ConcurrentHashMap<>();
    private final Map<VoidStrategy2<K, V>, VoidStrategy2<K, V>> nextFunctions = new ConcurrentHashMap<>();
    private final Map<VoidStrategy1<K>, VoidStrategy1<K>> subscribeFunctions = new ConcurrentHashMap<>();

    @Override
    public void onComplete(K key) {
        try {
            completeFunctions.values().forEach(obs -> obs.perform(key));
        } catch (RuntimeException e) {
            log.warn("Exception caught when performing callback", e);
        }
    }

    @Override
    public void onError(K key, Throwable throwable) {
        for (VoidStrategy2<K, Throwable> obs : errorFunctions.values()) {
            try {
                obs.perform(key, throwable);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        }
    }

    @Override
    public void onNext(K key, V value) {
        for (VoidStrategy2<K, V> obs : nextFunctions.values()) {
            try {
                obs.perform(key, value);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        }
    }

    @Override
    public void onSubscribe(K key, Subscription subscription) {
        subscribeFunctions.values().forEach(obs -> {
            try {
                obs.perform(key);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback", e);
            }
        });
    }

    public void disconnectAll() {
        completeFunctions.clear();
        errorFunctions.clear();
        nextFunctions.clear();
        subscribeFunctions.clear();
    }

    public void connect(RxKeyValueLambdaSubject<K, V> subject) {
        this.errorFunctions.putAll(subject.errorFunctions);
        this.nextFunctions.putAll(subject.nextFunctions);
        this.completeFunctions.putAll(subject.completeFunctions);
        this.subscribeFunctions.putAll(subject.subscribeFunctions);
    }

    public RxKeyValueLambdaSubject<K, V> onCompleteDo(VoidStrategy1<K> completedFunction) {
        if (completedFunction != null) {
            completeFunctions.put(completedFunction, completedFunction);
        }
        return this;
    }

    public RxKeyValueLambdaSubject<K, V> onErrorDo(VoidStrategy2<K, Throwable> errorFunction) {
        if (errorFunction != null) {
            errorFunctions.put(errorFunction, errorFunction);
        }
        return this;
    }

    public RxKeyValueLambdaSubject<K, V> onNextDo(VoidStrategy2<K, V> nextFunction) {
        if (nextFunction != null) {
            nextFunctions.put(nextFunction, nextFunction);
        }
        return this;
    }

    public RxKeyValueLambdaSubject<K, V> onSubscribeDo(VoidStrategy1<K> subscribeFunction) {
        if (subscribeFunction != null) {
            subscribeFunctions.put(subscribeFunction, subscribeFunction);
        }
        return this;
    }
}
