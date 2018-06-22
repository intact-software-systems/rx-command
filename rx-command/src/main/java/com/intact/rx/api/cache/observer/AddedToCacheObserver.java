package com.intact.rx.api.cache.observer;

/**
 * An object instance stored in a DataCache may implement this interface and receive callbacks when
 * it is added to the rx cache.
 */
@FunctionalInterface
public interface AddedToCacheObserver {
    void onAddedToCache();
}
