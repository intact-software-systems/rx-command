package com.intact.rx.core.cache.nullobjects;

import java.util.Optional;
import java.util.UUID;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.ValueEditor;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.templates.MementoReferenceNoOp;
import com.intact.rx.templates.api.Memento;

public class ValueEditorNoOp<V> implements ValueEditor<V> {
    @SuppressWarnings("rawtypes")
    public static final ValueEditorNoOp instance = new ValueEditorNoOp();

    @Override
    public Object getKey() {
        return UUID.randomUUID();
    }

    @Override
    public ValueHandle<Object> getValueHandle() {
        return ValueHandle.create(CacheHandle.uuid(), "noop");
    }

    @Override
    public Optional<V> readEdited() {
        return Optional.empty();
    }

    @Override
    public Optional<V> readCached() {
        return Optional.empty();
    }

    @Override
    public Optional<V> write(V newVale) {
        return Optional.empty();
    }

    @Override
    public Memento<V> getMemento() {
        //noinspection unchecked
        return MementoReferenceNoOp.instance;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean commit() {
        return false;
    }
}
