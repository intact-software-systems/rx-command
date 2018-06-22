package com.intact.rx.templates;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.intact.rx.templates.api.Memento;

public class MementoReferenceNoOp<T> implements Memento<T> {
    @SuppressWarnings("rawtypes")
    public static final MementoReferenceNoOp instance = new MementoReferenceNoOp<>();

    @Override
    public Memento<T> copy() {
        //noinspection unchecked
        return instance;
    }

    @Override
    public Optional<T> read() {
        return Optional.empty();
    }

    @Override
    public boolean compareAndSet(T expect, T update) {
        return false;
    }

    @Override
    public T getAndSet(T newValue) {
        return null;
    }

    @Override
    public List<T> undoStack() {
        return Collections.emptyList();
    }

    @Override
    public List<T> redoStack() {
        return Collections.emptyList();
    }

    @Override
    public Optional<T> undo() {
        return Optional.empty();
    }

    @Override
    public Optional<T> redo() {
        return Optional.empty();
    }

    @Override
    public Memento<T> clearRedo() {
        return this;
    }

    @Override
    public Memento<T> clearUndo() {
        return this;
    }

    @Override
    public Memento<T> clearAll() {
        return this;
    }

    @Override
    public Memento<T> set(T newValue) {
        return this;
    }

    @Override
    public boolean isUndoStackEmpty() {
        return true;
    }

    @Override
    public boolean isRedoStackEmpty() {
        return true;
    }

    @Override
    public boolean isAllEmpty() {
        return true;
    }

    @Override
    public T get() {
        return null;
    }
}
