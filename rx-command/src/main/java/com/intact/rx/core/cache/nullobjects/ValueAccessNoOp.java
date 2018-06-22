package com.intact.rx.core.cache.nullobjects;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxValueAccess;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.api.command.VoidStrategy1;

public class ValueAccessNoOp<V> implements RxValueAccess<V> {
    @SuppressWarnings("rawtypes")
    public static final ValueAccessNoOp instance = new ValueAccessNoOp();

    @Override
    public Object key() {
        return UUID.randomUUID();
    }

    @Override
    public boolean isContained() {
        return false;
    }

    @Override
    public Optional<V> read() {
        return Optional.empty();
    }

    @Override
    public Optional<V> take() {
        return Optional.empty();
    }

    @Override
    public Optional<V> write(V value) {
        return Optional.empty();
    }

    @Override
    public V computeIfAbsent(Supplier<V> factory) {
        return factory.get();
    }

    @Override
    public ValueHandle<Object> getValueHandle() {
        return ValueHandle.create(CacheHandle.uuid(), "noop");
    }

    @Override
    public boolean isExpired() {
        return true;
    }

    @Override
    public RxValueAccess<V> onObjectCreatedDo(VoidStrategy1<V> strategy) {
        return this;
    }

    @Override
    public RxValueAccess<V> onObjectModifiedDo(VoidStrategy1<V> strategy) {
        return this;
    }

    @Override
    public RxValueAccess<V> onObjectRemovedDo(VoidStrategy1<V> strategy) {
        return this;
    }

    @Override
    public RxValueAccess<V> onUndoDo(VoidStrategy1<V> strategy) {
        return this;
    }

    @Override
    public RxValueAccess<V> onRedoDo(VoidStrategy1<V> strategy) {
        return this;
    }

    @Override
    public void disconnectAll() {
    }

    @Override
    public void detach() {
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isAttached() {
        return false;
    }

    @Override
    public List<V> undoStack() {
        return Collections.emptyList();
    }

    @Override
    public List<V> redoStack() {
        return Collections.emptyList();
    }

    @Override
    public Optional<V> undo() {
        return Optional.empty();
    }

    @Override
    public Optional<V> redo() {
        return Optional.empty();
    }

    @Override
    public RxValueAccess<V> clearRedo() {
        return this;
    }

    @Override
    public RxValueAccess<V> clearUndo() {
        return this;
    }
}
