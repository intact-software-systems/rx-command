package com.intact.rx.testdata.cache;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.observer.DataCacheObserver;

public class SimpleCacheObserver implements DataCacheObserver {

    private CacheHandle id;

    @Override
    public void onClearedCache(CacheHandle id) {
        this.id = id;
    }

    @Override
    public void onCreatedCache(CacheHandle id) {
        this.id = id;
    }

    @Override
    public void onModifiedCache(CacheHandle id) {
        this.id = id;
    }

    @Override
    public void onRemovedCache(CacheHandle id) {
        this.id = id;
    }

    public CacheHandle getId() {
        return id;
    }
}
