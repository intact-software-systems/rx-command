package com.intact.rx.api.cache;

@FunctionalInterface
public interface RxFilter<K, V> {
    boolean filter(K key, V newValue, boolean expired);
}
