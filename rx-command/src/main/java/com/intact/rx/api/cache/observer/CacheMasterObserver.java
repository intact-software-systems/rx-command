package com.intact.rx.api.cache.observer;

import com.intact.rx.core.cache.data.CacheMaster;

public interface CacheMasterObserver {
    void onCreatedCacheMaster(CacheMaster id);

    void onRemovedCacheMaster(CacheMaster id);
}
