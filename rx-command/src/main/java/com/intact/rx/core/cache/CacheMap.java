package com.intact.rx.core.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.ReaderWriter;

public class CacheMap<K, V> implements Map<K, V> {
    private final ReaderWriter<K, V> cache;

    public CacheMap(ReaderWriter<K, V> cache) {
        this.cache = requireNonNull(cache);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey((K) key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cache.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return cache.read((K) key).orElse(null);
    }

    @Override
    public V put(K key, V value) {
        return cache.write(key, value).orElse(null);
    }

    @Override
    public V remove(Object key) {
        return cache.take((K) key).orElse(null);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        cache.writeAll(m);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public Set<K> keySet() {
        return cache.keySet();
    }

    @Override
    public Collection<V> values() {
        return cache.readAsList();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return cache.readAll().entrySet();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return cache.read((K) key).orElse(defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        for (Entry<K, V> entry : entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        for (Entry<K, V> entry : entrySet()) {
            K key = entry.getKey();
            cache.write(key, function.apply(key, entry.getValue()));
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return cache.computeIfAbsent(key, k -> value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return (cache.containsKey((K) key) && cache.containsValue(value)) && cache.take((K) key).isPresent();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return cache.compareAndWrite(key, () -> oldValue, () -> newValue);
    }

    @Override
    public V replace(K key, V value) {
        return cache.replace(key, () -> value).orElse(null);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return cache.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return cache.computeIfPresent(key, remappingFunction).orElse(null);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return cache.compute(key, (k, currentValue) -> remappingFunction.apply(k, (V) currentValue.orElse(null))).orElse(null);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return cache.merge(key, value, remappingFunction::apply).orElse(null);
    }
}
