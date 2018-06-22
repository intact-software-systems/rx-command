package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.observer.DataCacheObserver;
import com.intact.rx.templates.api.Observable;

public class DataCacheSubject implements
        DataCacheObserver,
        Observable<DataCacheObserver> {
    private static final Logger log = LoggerFactory.getLogger(DataCacheSubject.class);
    private final Map<DataCacheObserver, DataCacheObserver> observers = new ConcurrentHashMap<>();

    @Override
    public void onClearedCache(CacheHandle id) {
        for (DataCacheObserver observer : observers.values()) {
            try {
                observer.onClearedCache(id);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", id, e);
            }
        }
    }

    @Override
    public void onCreatedCache(CacheHandle id) {
        for (DataCacheObserver observer : observers.values()) {
            try {
                observer.onCreatedCache(id);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", id, e);
            }
        }
    }

    @Override
    public void onModifiedCache(CacheHandle id) {
        for (DataCacheObserver observer : observers.values()) {
            try {
                observer.onModifiedCache(id);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", id, e);
            }
        }
    }

    @Override
    public void onRemovedCache(CacheHandle id) {
        for (DataCacheObserver observer : observers.values()) {
            try {
                observer.onRemovedCache(id);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {}", id, e);
            }
        }
    }

    @Override
    public boolean connect(DataCacheObserver observer) {
        requireNonNull(observer);
        observers.put(observer, observer);
        return true;
    }

    @Override
    public boolean disconnect(DataCacheObserver observer) {
        requireNonNull(observer);
        return observers.remove(observer) != null;
    }

    @Override
    public void disconnectAll() {
        observers.clear();
    }
}
