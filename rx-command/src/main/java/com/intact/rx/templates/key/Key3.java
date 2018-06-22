package com.intact.rx.templates.key;

import java.util.Objects;

@SuppressWarnings({"PublicField", "WeakerAccess"})
public class Key3<A, B, C> {
    public final A first;
    public final B second;
    public final C third;

    public Key3(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public String toString() {
        return "Key3{" +
                "first=" + first +
                ", second=" + second +
                ", third=" + third +
                '}';
    }

    @SuppressWarnings({"ControlFlowStatementWithoutBraces", "SimplifiableIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key3)) return false;

        Key3<?, ?, ?> key3 = (Key3<?, ?, ?>) o;

        if (!Objects.equals(first, key3.first)) return false;
        if (!Objects.equals(second, key3.second)) return false;
        return Objects.equals(third, key3.third);
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        result = 31 * result + (third != null ? third.hashCode() : 0);
        return result;
    }
}
