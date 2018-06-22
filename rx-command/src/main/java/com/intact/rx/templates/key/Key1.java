package com.intact.rx.templates.key;

import java.util.Objects;

@SuppressWarnings({"PublicField", "WeakerAccess"})
public class Key1<A> {
    public final A first;

    public Key1(A first) {
        this.first = first;
    }

    @Override
    public String toString() {
        return "Key1{" +
                "first=" + first +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key1)) return false;

        Key1<?> key1 = (Key1<?>) o;

        return Objects.equals(first, key1.first);
   }

    @Override
    public int hashCode() {
        return first != null ? first.hashCode() : 0;
    }
}
