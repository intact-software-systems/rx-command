package com.intact.rx.policy;

import java.util.Objects;

public final class MinLimit {
    private final long limit;

    public MinLimit(long limit) {
        this.limit = limit;
    }

    public long getLimit() {
        return limit;
    }

    public boolean isOver(final long num) {
        return num > limit;
    }

    public boolean isWithin(final long num) {
        return num >= limit;
    }

    public static MinLimit withLimit(final long limit) {
        return new MinLimit(limit);
    }

    @Override
    public String toString() {
        return "MinLimit{" +
                "limit=" + limit +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MinLimit)) return false;
        MinLimit minLimit = (MinLimit) o;
        return limit == minLimit.limit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit);
    }
}
