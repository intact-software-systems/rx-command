package com.intact.rx.core.cache.subject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.CacheMasterObserver;
import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.templates.api.Observable;

public class CacheMasterSubject implements
        CacheMasterObserver,
        Observable<CacheMasterObserver> {
    private static final Logger log = LoggerFactory.getLogger(CacheMasterSubject.class);
    private final Map<CacheMasterObserver, CacheMasterObserver> observers = new ConcurrentHashMap<>();

    @Override
    public void onCreatedCacheMaster(CacheMaster cacheMaster) {
        for (CacheMasterObserver observer : observers.values()) {
            try {
                observer.onCreatedCacheMaster(cacheMaster);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {} ", cacheMaster.getMasterCacheId(), e);
            }
        }
    }

    @Override
    public void onRemovedCacheMaster(CacheMaster cacheMaster) {
        for (CacheMasterObserver observer : observers.values()) {
            try {
                observer.onRemovedCacheMaster(cacheMaster);
            } catch (RuntimeException e) {
                log.warn("Exception caught when performing callback on {} ", cacheMaster.getMasterCacheId(), e);
            }
        }
    }

    @Override
    public boolean connect(CacheMasterObserver observer) {
        requireNonNull(observer);
        observers.put(observer, observer);
        return true;
    }

    @Override
    public boolean disconnect(CacheMasterObserver observer) {
        requireNonNull(observer);
        return observers.remove(observer) != null;
    }

    @Override
    public void disconnectAll() {
        observers.clear();
    }
}
