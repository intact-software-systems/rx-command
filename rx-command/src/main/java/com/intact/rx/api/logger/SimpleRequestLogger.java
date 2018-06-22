package com.intact.rx.api.logger;

import org.slf4j.Logger;

import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.api.rxcache.RxStreamer;

public class SimpleRequestLogger implements RxKeyValueObserver<ValueHandle<?>, RxStreamer> {
    private final Logger log;

    public SimpleRequestLogger(Logger log) {
        this.log = log;
    }

    @Override
    public void onComplete(ValueHandle<?> key) {
        log.info("Completed {}", key.getKey());
    }

    @Override
    public void onError(ValueHandle<?> key, Throwable throwable) {
        log.info("Error {}", key.getKey(), throwable);
    }

    @Override
    public void onNext(ValueHandle<?> key, RxStreamer value) {
        log.info("Next {}", key.getKey());
    }

    @Override
    public void onSubscribe(ValueHandle<?> key, Subscription subscription) {
        log.info("Subscribe {}", key.getKey());
    }
}
