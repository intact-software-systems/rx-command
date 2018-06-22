package com.intact.rx.api.rxrepo;

import java.util.Optional;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.cache.RxValueAccess;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.templates.api.ObservableFluent;
import com.intact.rx.templates.api.RxObserverSupport;

public interface RxValueReader<V> extends RxObserverSupport<RxValueReader<V>, V>, ObservableFluent<RxValueReader<V>, RxObserver<V>> {

    /**
     * @return access to the cached value
     */
    RxValueAccess<V> accessCached();

    /**
     * @return status of request
     */
    RxRequestStatus getStatus();

    /**
     * @param msecs wait time before timeout
     * @return value V from cache or remote
     */
    Optional<V> computeIfAbsent(long msecs);

    /**
     * @param computeResolver checks if the value found in cache needs updating (false -> no compute, return cached, true -> compute and return fetched value)
     * @param msecs           wait time before timeout
     * @return value V from cache or remote
     */
    Optional<V> computeIf(Strategy1<Boolean, V> computeResolver, long msecs);

    /**
     * @param msecs wait time before timeout
     * @return value V from cache or remote, throws exception if request failed or value is null
     */
    V computeValueIfAbsent(long msecs);

    /**
     * @param computeResolver checks if the value found in cache needs updating (false -> no compute, return cached, true -> compute and return fetched value)
     * @param msecs           wait time before timeout
     * @return value V from cache or remote, throws exception if request failed or value is null
     */
    V computeValueIf(Strategy1<Boolean, V> computeResolver, long msecs);

    /**
     * @param msecs wait time before timeout
     * @return value V from remote
     */
    Optional<V> compute(long msecs);

    /**
     * @param msecs wait time before timeout
     * @return value V from remote, throws exception if request failed or value is null
     */
    V computeValue(long msecs);

    /**
     * Executes and returns immediately
     */
    RxValueReader<V> subscribe();

    /**
     * Waits maximum msecs for execution to finish.
     *
     * @param msecs wait time before timeout
     */
    RxValueReader<V> waitFor(long msecs);
}
