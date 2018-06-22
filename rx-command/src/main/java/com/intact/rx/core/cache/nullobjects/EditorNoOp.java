package com.intact.rx.core.cache.nullobjects;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.cache.Editor;
import com.intact.rx.api.cache.RxCache;

public class EditorNoOp<K, V> implements Editor<K, V> {
    @SuppressWarnings("rawtypes")
    public static final EditorNoOp instance = new EditorNoOp();

    @Override
    public Editor<K, V> autoRefreshAll() {
        return this;
    }

    @Override
    public Editor<K, V> autoRefresh(Collection<? extends K> iterable) {
        return this;
    }

    @Override
    public Editor<K, V> autoRefreshNone() {
        return this;
    }

    @Override
    public RxCache<K, V> edit() {
        //noinspection unchecked
        return RxCacheNoOp.instance;
    }

    @Override
    public RxCache<K, V> cache() {
        //noinspection unchecked
        return RxCacheNoOp.instance;
    }

    @Override
    public boolean isModified(K key) {
        return false;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public Map<K, V> readModified() {
        return Collections.emptyMap();
    }

    @Override
    public Optional<V> commit(K key) {
        return Optional.empty();
    }

    @Override
    public Map<? extends K, ? extends V> commit() {
        return Collections.emptyMap();
    }

    @Override
    public void refresh() {

    }
}
