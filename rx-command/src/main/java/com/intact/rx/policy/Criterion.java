package com.intact.rx.policy;

import java.util.Objects;

public final class Criterion {

    public enum Kind {
        ALL,
        MINIMUM,
        UNCONDITIONAL
    }

    private final MinLimit limit;

    private final Reaction reaction;

    private final Kind kind;

    private static final Criterion all = new Criterion(new MinLimit(Long.MAX_VALUE), Reaction.HALT, Kind.ALL);

    private static final Criterion unconditional = new Criterion(new MinLimit(0), Reaction.CONTINUE, Kind.UNCONDITIONAL);

    public Criterion(MinLimit limit, Reaction reaction, Kind kind) {
        this.limit = limit;
        this.reaction = reaction;
        this.kind = kind;
    }

    public MinLimit getMinLimit() {
        return limit;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public Kind getKind() {
        return kind;
    }

    public static Criterion all() {
        return all;
    }

    public static Criterion minimumAnd(MinLimit minLimit, Reaction reaction) {
        return new Criterion(minLimit, reaction, Kind.MINIMUM);
    }

    public static Criterion unconditional() {
        return unconditional;
    }

    public boolean isAll() {
        return Objects.equals(Kind.ALL, kind);

    }

    public boolean isMinimum() {
        return Objects.equals(Kind.MINIMUM, kind);
    }

    @Override
    public String toString() {
        return "Criterion{" +
                "limit=" + limit +
                ", reaction=" + reaction +
                ", kind=" + kind +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Criterion criterion = (Criterion) o;
        return Objects.equals(limit, criterion.limit) &&
                reaction == criterion.reaction &&
                kind == criterion.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, reaction, kind);
    }
}
