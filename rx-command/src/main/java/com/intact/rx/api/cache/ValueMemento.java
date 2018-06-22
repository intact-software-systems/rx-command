package com.intact.rx.api.cache;

import java.util.List;
import java.util.Optional;

public interface ValueMemento<V> {
    /**
     * @return undo stack for value
     */
    List<V> undoStack();

    /**
     * @return redo stack for value
     */
    List<V> redoStack();

    /**
     * Undo last write on value.
     *
     * @return entry that was undone.
     */
    Optional<V> undo();

    /**
     * Redo last write on value
     *
     * @return value that was redone.
     */
    Optional<V> redo();

    /**
     * clear redo stack for value
     *
     * @return this
     */
    ValueMemento<V> clearRedo();

    /**
     * clear undo stack for value
     *
     * @return this
     */
    ValueMemento<V> clearUndo();
}
