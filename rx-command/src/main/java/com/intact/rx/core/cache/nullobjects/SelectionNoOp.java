package com.intact.rx.core.cache.nullobjects;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxSelection;
import com.intact.rx.api.cache.observer.SelectionObserver;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;

public class SelectionNoOp<K, V> implements RxSelection<K, V> {

    @SuppressWarnings("rawtypes")
    public static final RxSelection instance = new SelectionNoOp<>();

    @Override
    public Optional<V> write(K key, V value) {
        return Optional.empty();
    }

    @Override
    public Optional<V> writeAndGet(K key, Supplier<V> factory) {
        return Optional.empty();
    }

    @Override
    public Map<? extends K, ? extends V> writeAll(Map<? extends K, ? extends V> values) {
        return Collections.emptyMap();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> factory) {
        return factory.apply(key);
    }

    @Override
    public Optional<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Optional.empty();
    }

    @Override
    public Optional<V> compute(K key, BiFunction<? super K, Optional<? super V>, ? extends V> remappingFunction) {
        return Optional.empty();
    }

    @Override
    public boolean compareAndWrite(K key, Supplier<V> expect, Supplier<V> update) {
        return false;
    }

    @Override
    public Optional<V> replace(K key, Supplier<V> update) {
        return Optional.empty();
    }

    @Override
    public Optional<V> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return Optional.empty();
    }

    @Override
    public Optional<V> take(K key) {
        return Optional.empty();
    }

    @Override
    public Map<K, V> take(Iterable<? extends K> keys) {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, V> takeAll() {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, V> takeExpired() {
        return Collections.emptyMap();
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean detach() {
        return false;
    }

    @Override
    public boolean isAttached() {
        return false;
    }

    @Override
    public boolean connect(SelectionObserver<V> observer) {
        return false;
    }

    @Override
    public boolean disconnect(SelectionObserver<V> observer) {
        return false;
    }

    @Override
    public void disconnectAll() {
    }

    @Override
    public RxSelection<K, V> onObjectInDo(VoidStrategy1<V> strategy) {
        return this;
    }

    @Override
    public RxSelection<K, V> onObjectOutDo(VoidStrategy1<V> strategy) {
        return this;
    }

    @Override
    public RxSelection<K, V> onObjectModifiedDo(VoidStrategy1<V> strategy) {
        return this;
    }

    @Override
    public RxSelection<K, V> onDetachDo(VoidStrategy0 strategy) {
        return this;
    }

    @Override
    public Object getId() {
        return UUID.randomUUID();
    }

    @Override
    public boolean containsKey(K key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Optional<V> read(K key) {
        return Optional.empty();
    }

    @Override
    public Map<K, V> read(Iterable<? extends K> keys) {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, V> readAll() {
        return Collections.emptyMap();
    }

    @Override
    public Map<K, V> readExpired() {
        return Collections.emptyMap();
    }

    @Override
    public Set<K> keySet() {
        return Collections.emptySet();
    }

    @Override
    public Set<K> keySetExpired() {
        return Collections.emptySet();
    }

    @Override
    public List<V> readAsList() {
        return Collections.emptyList();
    }

    @Override
    public CacheHandle getCacheId() {
        return CacheHandle.uuid();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isExpired(K key) {
        return true;
    }

    @Override
    public boolean isExpired() {
        return true;
    }

    @Override
    public void close() {

    }
}
