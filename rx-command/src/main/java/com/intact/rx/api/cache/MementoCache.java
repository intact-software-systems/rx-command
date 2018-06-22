package com.intact.rx.api.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MementoCache<K, V> {

    /**
     * @param key to identify value
     * @return previous value for key
     */
    Optional<V> undo(K key);

    /**
     * @param key to identify value
     * @return previous value for key
     */
    Optional<V> redo(K key);

    /**
     * @param key to identify value
     * @return undo stack for key
     */
    List<V> undoStack(K key);

    /**
     * @param key to identify value
     * @return redo stack for key
     */
    List<V> redoStack(K key);

    /**
     * Clear undo stack for key.
     *
     * @param key to identify value
     * @return this
     */
    MementoCache<K, V> clearUndo(K key);

    /**
     * Clear redo stack for key.
     *
     * @param key to identify value
     * @return this
     */
    MementoCache<K, V> clearRedo(K key);

    /**
     * Undo last write in cache.
     *
     * @return entry that was undone.
     */
    Optional<Map.Entry<K, V>> undo();

    /**
     * Redo last write in cache.
     *
     * @return entry that was redone.
     */
    Optional<Map.Entry<K, V>> redo();

    /**
     * @return undo stack for cache
     */
    List<Map.Entry<K, V>> undoStack();

    /**
     * @return redo stack for cache
     */
    List<Map.Entry<K, V>> redoStack();

    /**
     * clear undo stack for cache
     *
     * @return this
     */
    MementoCache<K, V> clearUndo();

    /**
     * clear redo stack for cache
     *
     * @return this
     */
    MementoCache<K, V> clearRedo();
}
