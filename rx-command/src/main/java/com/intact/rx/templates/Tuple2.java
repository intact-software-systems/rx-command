package com.intact.rx.templates;

@SuppressWarnings({"PublicField", "WeakerAccess"})
public class Tuple2<A, B> {
    public final A first;
    public final B second;

    public Tuple2(A first, B second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "Tuple2{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
