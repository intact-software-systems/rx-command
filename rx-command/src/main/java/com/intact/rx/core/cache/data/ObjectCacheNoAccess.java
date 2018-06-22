package com.intact.rx.core.cache.data;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.policy.LoanReturnPolicy;
import com.intact.rx.templates.MementoReferenceNoOp;
import com.intact.rx.templates.Pair;
import com.intact.rx.templates.Tuple3;
import com.intact.rx.templates.api.Memento;

class ObjectCacheNoAccess<K, V> extends ObjectCache<K, V> {

    ObjectCacheNoAccess(DataCacheId dataCacheId, DataCachePolicy policy) {
        super(dataCacheId, policy);
    }

    @Override
    public Pair<V, ObjectRoot<K, V>> write(K key, V value) {
        return Pair.empty();
    }

    @Override
    public Memento<ObjectRoot<K, V>> getMemento() {
        //noinspection unchecked
        return MementoReferenceNoOp.instance;
    }

    @Override
    public Optional<Pair<V, ObjectRoot<K, V>>> undo(K key) {
        return Optional.empty();
    }

    @Override
    public Optional<Pair<V, ObjectRoot<K, V>>> redo(K key) {
        return Optional.empty();
    }

    @Override
    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> undo() {
        return Optional.empty();
    }

    @Override
    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> redo() {
        return Optional.empty();
    }

    @Override
    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> computeIfAbsentAndLoan(K key, Function<? super K, ? extends V> factory, LoanPolicy loanPolicy) {
        return Optional.of(new Tuple3<>(AccessStatus.AccessState.WRITE, null, ObjectRoot.create(factory.apply(key), key, config().getRootPolicy())));
    }

    @Override
    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return Optional.empty();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Optional<ObjectRoot<K, V>> loan(K key, LoanPolicy loanPolicy) {
        return Optional.empty();
    }

    @Override
    public boolean isLoaned(K key) {
        return false;
    }

    @Override
    public Map<K, ObjectRoot<K, V>> take(Iterable<? extends K> keys) {
        return Collections.emptyMap();
    }

    @Override
    public Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> factory) {
        return new Tuple3<>(AccessStatus.AccessState.WRITE, null, ObjectRoot.create(factory.apply(key), key, config().getRootPolicy()));
    }

    @Override
    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> factory) {
        return Optional.empty();
    }

    @Override
    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> compute(K key, BiFunction<? super K, Optional<? super V>, Optional<? extends V>> remappingFunction) {
        return Optional.empty();
    }

    @Override
    public Optional<Pair<V, ObjectRoot<K, V>>> compareAndWrite(K key, Function<V, V> expect, Supplier<V> update) {
        return Optional.empty();
    }

    @Override
    public Optional<ObjectRoot<K, V>> take(K key) {
        return Optional.empty();
    }

    @Override
    public Map<K, ObjectRoot<K, V>> takeAll() {
        return Collections.emptyMap();
    }

    @Override
    public boolean containsKey(K key) {
        return false;
    }

    @Override
    public Optional<ObjectRoot<K, V>> read(K key) {
        return Optional.empty();
    }

    @Override
    public Optional<ObjectRoot<K, V>> returnLoan(K key, LoanReturnPolicy loanReturn) {
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
    public List<V> readAsList() {
        return Collections.emptyList();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Map<K, ObjectRoot<K, V>> clear() {
        return Collections.emptyMap();
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
    public Set<K> keySet() {
        return Collections.emptySet();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.emptySet();
    }

    @Override
    public List<ObjectRoot<K, V>> getRoots() {
        return Collections.emptyList();
    }
}
