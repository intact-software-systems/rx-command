package com.intact.rx.core.cache;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxValueAccess;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.api.cache.observer.MementoObserver;
import com.intact.rx.api.cache.observer.ObjectObserver;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.core.cache.subject.MementoValueSubject;
import com.intact.rx.core.cache.subject.ObjectTypeLambdaSubject;

public class CachedValueAccess<K, V> implements RxValueAccess<V>, ObjectObserver<K, V>, MementoObserver<Map.Entry<K, V>> {
    private final K key;
    private final RxCache<K, V> cache;
    private final AtomicBoolean attached;
    private final ObjectTypeLambdaSubject<V> subject;
    private final MementoValueSubject<V> mementoSubject;

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public CachedValueAccess(K key, RxCache<K, V> cache) {
        this.key = requireNonNull(key);
        this.cache = requireNonNull(cache);
        this.attached = new AtomicBoolean(true);
        this.subject = new ObjectTypeLambdaSubject<>();
        this.mementoSubject = new MementoValueSubject<>();

        this.cache.addObjectObserver(this);
        this.cache.addMementoObserver(this);
    }

    @Override
    public K key() {
        return key;
    }

    @Override
    public boolean isContained() {
        return cache.containsKey(key);
    }

    @Override
    public Optional<V> read() {
        return cache.read(key);
    }

    @Override
    public Optional<V> take() {
        return cache.take(key);
    }

    @Override
    public Optional<V> write(V value) {
        return cache.write(key, value);
    }

    @Override
    public V computeIfAbsent(Supplier<V> factory) {
        return cache.computeIfAbsent(key, k -> factory.get());
    }

    @Override
    public ValueHandle<Object> getValueHandle() {
        return ValueHandle.create(cache.getCacheHandle(), key);
    }

    @Override
    public boolean isExpired() {
        return cache.isExpired();
    }

    @Override
    public RxValueAccess<V> onObjectCreatedDo(VoidStrategy1<V> strategy) {
        subject.onObjectCreatedDo(strategy);
        return this;
    }

    @Override
    public RxValueAccess<V> onObjectModifiedDo(VoidStrategy1<V> strategy) {
        subject.onObjectModifiedDo(strategy);
        return this;
    }

    @Override
    public RxValueAccess<V> onObjectRemovedDo(VoidStrategy1<V> strategy) {
        subject.onObjectRemovedDo(strategy);
        return this;
    }

    @Override
    public RxValueAccess<V> onUndoDo(VoidStrategy1<V> strategy) {
        mementoSubject.onUndoDo(strategy);
        return this;
    }

    @Override
    public RxValueAccess<V> onRedoDo(VoidStrategy1<V> strategy) {
        mementoSubject.onRedoDo(strategy);
        return this;
    }

    @Override
    public void disconnectAll() {
        subject.disconnectAll();
    }

    @Override
    public void detach() {
        cache.removeObjectObserver(this);
        cache.removeMementoObserver(this);
        attached.set(false);
    }

    @Override
    public void close() {
        detach();
    }

    @Override
    public boolean isAttached() {
        return attached.get();
    }

    // -----------------------------------------------------
    // Interface Memento<V>
    // -----------------------------------------------------

    @Override
    public List<V> undoStack() {
        return cache.undoStack(key);
    }

    @Override
    public List<V> redoStack() {
        return cache.redoStack(key);
    }

    @Override
    public Optional<V> undo() {
        return cache.undo(key);
    }

    @Override
    public Optional<V> redo() {
        return cache.redo(key);
    }

    @Override
    public RxValueAccess<V> clearRedo() {
        cache.clearRedo(key);
        return this;
    }

    @Override
    public RxValueAccess<V> clearUndo() {
        cache.clearUndo(key);
        return this;
    }

    // -----------------------------------------------------
    // Interface ObjectObserver<K, V>
    // -----------------------------------------------------

    @Override
    public void onObjectCreated(K key, V value) {
        if (Objects.equals(key, this.key)) {
            subject.onObjectCreated(value);
        }
    }

    @Override
    public void onObjectRemoved(K key, V value) {
        if (Objects.equals(key, this.key)) {
            subject.onObjectRemoved(value);
        }
        detach();
    }

    @Override
    public void onObjectModified(K key, V value) {
        if (Objects.equals(key, this.key)) {
            subject.onObjectModified(value);
        }
    }

    // -----------------------------------------------------
    // Interface ObjectObserver<K, V>
    // -----------------------------------------------------

    @Override
    public void onUndo(Map.Entry<K, V> value) {
        if (Objects.equals(value.getKey(), this.key)) {
            mementoSubject.onUndo(value.getValue());
        }
    }

    @Override
    public void onRedo(Map.Entry<K, V> value) {
        if (Objects.equals(value.getKey(), this.key)) {
            mementoSubject.onRedo(value.getValue());
        }
    }

    @Override
    public String toString() {
        return "CachedValue{" +
                "cache=" + cache +
                ", key=" + key +
                '}';
    }
}
