package com.intact.rx.api.cache;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class ValueHandle<K> {
    private final CacheHandle cacheHandle;
    private final K key;

    private ValueHandle(CacheHandle cacheHandle, K key) {
        this.cacheHandle = requireNonNull(cacheHandle);
        this.key = requireNonNull(key);
    }

    public static <K> ValueHandle<K> create(CacheHandle cacheHandle, K key) {
        return new ValueHandle<>(cacheHandle, key);
    }

    public CacheHandle getCacheHandle() {
        return cacheHandle;
    }

    public K getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "ValueHandle{" +
                "cacheHandle=" + cacheHandle +
                ", key=" + key +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueHandle<?> that = (ValueHandle<?>) o;
        return Objects.equals(cacheHandle, that.cacheHandle) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cacheHandle, key);
    }
}
