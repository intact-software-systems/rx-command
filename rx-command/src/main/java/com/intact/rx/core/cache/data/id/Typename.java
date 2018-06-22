package com.intact.rx.core.cache.data.id;

import java.util.Objects;
import java.util.Optional;

public final class Typename {
    private final String typeName;
    private final Class<?> clazz;

    private Typename(String typeName) {
        this.typeName = typeName;
        this.clazz = null;
    }

    private Typename(String typeName, Class<?> clazz) {
        this.typeName = typeName;
        this.clazz = clazz;
    }

    public String getName() {
        return typeName;
    }

    public Optional<Class<?>> getClazz() {
        return Optional.ofNullable(clazz);
    }

    public <V> Optional<Class<V>> getTypedClazz() {
        //noinspection unchecked
        return Optional.ofNullable((Class<V>) clazz);
    }

    public static Typename create(Class<?> clazz) {
        return new Typename(clazz.getTypeName(), clazz);
    }

    public static Typename create(String typeName) {
        return new Typename(typeName);
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Typename)) return false;
        Typename typename = (Typename) o;
        return Objects.equals(typeName, typename.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + typeName + "]";
    }
}
