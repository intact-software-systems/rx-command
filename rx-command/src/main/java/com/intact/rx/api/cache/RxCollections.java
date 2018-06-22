package com.intact.rx.api.cache;

import java.util.Map;
import java.util.Set;

import com.intact.rx.core.cache.CacheMap;
import com.intact.rx.core.cache.CacheSet;

@SuppressWarnings("WeakerAccess")
public final class RxCollections {

    public static <K, V> Map<K, V> asMap(ReaderWriter<K, V> cache) {
        return new CacheMap<>(cache);
    }

    public static <T> Set<T> asSet(RxSet<T> set) {
        return new CacheSet<>(set);
    }

    private RxCollections() {
    }
}
