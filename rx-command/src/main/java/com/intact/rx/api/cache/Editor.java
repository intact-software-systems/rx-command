package com.intact.rx.api.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface Editor<K, V> {

    Editor<K, V> autoRefreshAll();

    Editor<K, V> autoRefresh(Collection<? extends K> iterable);

    Editor<K, V> autoRefreshNone();

    RxCache<K, V> edit();

    RxCache<K, V> cache();

    boolean isModified(K key);

    boolean isModified();

    Map<K, V> readModified();

    /**
     * @param key commit value found by using key, if modified.
     * @return updated value
     */
    Optional<V> commit(K key);

    /**
     * Commit if modified.
     */
    Map<? extends K, ? extends V> commit();

    /**
     * Refresh from main cache and replace edited values if applicable
     */
    void refresh();
}
