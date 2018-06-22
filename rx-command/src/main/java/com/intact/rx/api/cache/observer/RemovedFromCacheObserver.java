package com.intact.rx.api.cache.observer;

/**
 * An object instance stored in a DataCache may implement this interface and receive callbacks when
 * it is removed from the rx cache.
 */
@FunctionalInterface
public interface RemovedFromCacheObserver {
    void onRemovedFromCache();
}
