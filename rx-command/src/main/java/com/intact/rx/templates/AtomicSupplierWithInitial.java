package com.intact.rx.templates;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class AtomicSupplierWithInitial<T> {
    private final T initial;
    private final Supplier<T> supplier;
    private final AtomicReference<T> reference;

    public AtomicSupplierWithInitial(T initial, Supplier<T> supplier) {
        this.initial = requireNonNull(initial);
        this.supplier = requireNonNull(supplier);
        this.reference = new AtomicReference<>(initial);
    }

    public boolean isInitial() {
        //noinspection ObjectEquality
        return reference.get() == initial;
    }

    public T computeIfInitial() {
        //noinspection ObjectEquality
        if (reference.get() == initial) {
            reference.set(supplier.get());
        }
        return reference.get();
    }

    public T immutableGet() {
        return reference.get();
    }

    public T reset() {
        return reference.getAndSet(initial);
    }
}
