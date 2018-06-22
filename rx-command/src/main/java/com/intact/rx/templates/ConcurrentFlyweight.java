package com.intact.rx.templates;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Flyweight T using WeakHashMap.
 * <p>
 * Object creation cost is not a concern, the goal is to reduce overall memory consumption.
 *
 * @param <T>
 */
@SuppressWarnings("WeakerAccess")
public class ConcurrentFlyweight<T> {
    private final WeakHashMap<T, WeakReference<T>> flyweight = new WeakHashMap<>();

    public T compute(Supplier<T> supplier) {
        requireNonNull(supplier);

        T flyweightValue = requireNonNull(supplier.get());
        synchronized (flyweight) {
            return flyweight.computeIfAbsent(flyweightValue, WeakReference::new).get();
        }
    }

    public <K> T computeIfAbsent(K key, Supplier<T> supplier) {
        requireNonNull(key);
        requireNonNull(supplier);

        T value;
        synchronized (flyweight) {
            //noinspection SuspiciousMethodCalls
            value = Optional.ofNullable(flyweight.get(key)).map(Reference::get).orElse(null);
        }
        return value != null
                ? value
                : compute(supplier);
    }

    public Map<T, WeakReference<T>> readAll() {
        return Collections.unmodifiableMap(flyweight);
    }
}
