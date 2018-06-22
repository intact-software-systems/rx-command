package com.intact.rx.api.logger;

import org.slf4j.Logger;

import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheData;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.core.command.status.ExecutionStatus;

public class RepositoryRequestsLogger implements RxKeyValueObserver<ValueHandle<?>, RxStreamer> {
    private static final String ON_COMPLETE = "onComplete";
    private static final String ON_COMPLETE_TIME = ON_COMPLETE + ".time";
    private static final String ON_ERROR = "onError";
    private static final String ON_ERROR_TIME = ON_ERROR + ".time";

    private final String metricName;
    private final Logger log;
    private final RxCache<ValueHandle<?>, ExecutionStatus> cache;

    public RepositoryRequestsLogger(String metricName, Logger log) {
        this.metricName = metricName;
        this.log = log;
        this.cache = RxCacheData
                .in(metricName)
                .withCachePolicy(CachePolicy.fiveMinutesUnlimited())
                .cache(ExecutionStatus.class);
    }

    @Override
    public void onComplete(ValueHandle<?> key) {
        ExecutionStatus status = cache.computeIfAbsent(key, valueHandle -> new ExecutionStatus());
        status.success();
        log.info("Completed {}, {}.{}, {} msecs", key.getKey(), metricName, ON_COMPLETE_TIME, status.getTime().getTimeSinceLastExecutionTimeMs());
    }

    @Override
    public void onError(ValueHandle<?> key, Throwable throwable) {
        ExecutionStatus status = cache.computeIfAbsent(key, valueHandle -> new ExecutionStatus());
        status.failure();
        log.info("Error {}, {}.{}, {} msecs", key.getKey(), metricName, ON_ERROR_TIME, status.getTime().getTimeSinceLastExecutionTimeMs(), throwable);
    }

    @Override
    public void onNext(ValueHandle<?> key, RxStreamer value) {
    }

    @Override
    public void onSubscribe(ValueHandle<?> key, Subscription subscription) {
        cache.computeIfAbsent(key, valueHandle -> new ExecutionStatus()).start();
    }
}
