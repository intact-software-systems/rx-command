package com.intact.rx.templates.api;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface Memento<T> extends Supplier<T> {
    Memento<T> copy();

    Optional<T> read();

    boolean compareAndSet(T expect, T update);

    T getAndSet(T newValue);

    List<T> undoStack();

    List<T> redoStack();

    Optional<T> undo();

    Optional<T> redo();

    Memento<T> clearRedo();

    Memento<T> clearUndo();

    Memento<T> clearAll();

    Memento<T> set(T newValue);

    boolean isUndoStackEmpty();

    boolean isRedoStackEmpty();

    boolean isAllEmpty();
}
