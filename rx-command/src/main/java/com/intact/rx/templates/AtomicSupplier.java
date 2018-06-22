package com.intact.rx.templates;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class AtomicSupplier<T> {
    private final AtomicReference<T> reference;
    private final Supplier<T> supplier;

    public AtomicSupplier(Supplier<T> supplier) {
        this.supplier = requireNonNull(supplier);
        this.reference = new AtomicReference<>(null);
    }

    public boolean isSet() {
        return reference.get() != null;
    }

    public T get() {
        if (reference.get() == null) {
            reference.set(supplier.get());
        }
        return reference.get();
    }

    public T clear() {
        return reference.getAndSet(null);
    }

    @Override
    public String toString() {
        return "AtomicSupplier{" +
                "reference=" + reference +
                ", supplier=" + supplier +
                '}';
    }
}
