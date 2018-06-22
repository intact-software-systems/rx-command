package com.intact.rx.templates.key;

import java.util.Objects;

@SuppressWarnings({"PublicField", "WeakerAccess"})
public class Key2<A, B> {
    public final A first;
    public final B second;

    public Key2(A first, B second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "Key2{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    @SuppressWarnings({"ControlFlowStatementWithoutBraces", "SimplifiableIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key2)) return false;

        Key2<?, ?> key2 = (Key2<?, ?>) o;

        if (!Objects.equals(first, key2.first)) return false;
        return Objects.equals(second, key2.second);
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}
