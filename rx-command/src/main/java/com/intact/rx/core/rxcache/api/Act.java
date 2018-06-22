package com.intact.rx.core.rxcache.api;

import java.util.Map;

import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.core.command.Commands;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.templates.api.RxObserverSupport;

public interface Act<K, V> extends RxObserverSupport<Act<K, V>, Map<K, V>> {
    /**
     * Start CommandController which executes its commands.
     */
    CommandResult<Map<K, V>> subscribe();

    /**
     * Refresh cache by executing attached commands.
     */
    CommandResult<Map<K, V>> refresh();

    /**
     * Stop CommandController and all attached commands..
     */
    boolean unsubscribe();

    boolean isSubscribed();

    Commands<Map<K, V>> getCommands();

    CommandResult<Map<K, V>> result();

    boolean isDone();

    ExecutionStatus getExecutionStatus();

    CacheHandle getCacheHandle();

    RxCache<K, V> cache();

    boolean connect(RxObserver<Map<K, V>> observer);

    boolean connect(RxKeyValueObserver<Act<?, ?>, Map<?, ?>> observer);

    boolean disconnect(RxObserver<Map<K, V>> observer);

    boolean disconnect(RxKeyValueObserver<Act<?, ?>, Map<?, ?>> observer);

    void disconnectAll();
}
