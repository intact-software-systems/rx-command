package com.intact.rx.core.cache;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.ValueEditor;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.templates.MementoReference;
import com.intact.rx.templates.api.Memento;

public class CachedValueEditor<K, V> implements ValueEditor<V> {
    private static final int UNDO_DEPTH = 5;
    private static final int REDO_DEPTH = 5;

    private final K key;
    private final RxCache<K, V> cache;
    private final AtomicReference<Memento<V>> reference;

    public CachedValueEditor(K key, RxCache<K, V> cache) {
        this.key = key;
        this.cache = cache;
        this.reference = new AtomicReference<>(null);
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public ValueHandle<Object> getValueHandle() {
        return ValueHandle.create(cache.getCacheHandle(), key);
    }

    @Override
    public Optional<V> readEdited() {
        return reference().read();
    }

    @Override
    public Optional<V> readCached() {
        return cache.read(key);
    }

    @Override
    public Optional<V> write(V newVale) {
        return Optional.ofNullable(reference().getAndSet(newVale));
    }

    @Override
    public Memento<V> getMemento() {
        return reference();
    }

    @Override
    public boolean isModified() {
        return cache.read(key).map(v -> !Objects.equals(v, reference().get())).orElse(true);
    }

    @Override
    public boolean commit() {
        if (reference().get() != null && isModified()) {
            cache.write(key, reference().get());
            return true;
        }
        return false;
    }

    /**
     * "initialize on first access" style
     */
    private Memento<V> reference() {
        if (reference.get() == null) {
            reference.set(new MementoReference<>(UNDO_DEPTH, REDO_DEPTH));
            cache.read(key).map(newValue -> reference.get().set(newValue));
        }
        return reference.get();
    }
}
