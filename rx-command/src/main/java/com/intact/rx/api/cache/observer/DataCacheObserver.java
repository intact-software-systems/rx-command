package com.intact.rx.api.cache.observer;

import com.intact.rx.api.cache.CacheHandle;

public interface DataCacheObserver {

    void onClearedCache(CacheHandle id);

    void onCreatedCache(CacheHandle id);

    void onModifiedCache(CacheHandle id);

    void onRemovedCache(CacheHandle id);
}
