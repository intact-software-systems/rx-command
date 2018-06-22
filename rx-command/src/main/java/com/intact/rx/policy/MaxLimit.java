package com.intact.rx.policy;

import java.util.Objects;

public final class MaxLimit {
    public static final MaxLimit unlimited = new MaxLimit(Integer.MAX_VALUE);
    public static final MaxLimit one = new MaxLimit(1);

    private final int limit;

    private MaxLimit(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isBelow(final int num) {
        return num < limit;
    }

    public boolean isWithin(final int num) {
        return num <= limit;
    }

    public boolean isUnlimited() {
        return limit == Integer.MAX_VALUE;
    }

    public static MaxLimit withLimit(final int limit) {
        return new MaxLimit(limit);
    }

    public static MaxLimit unlimited() {
        return unlimited;
    }

    public static MaxLimit one() {
        return one;
    }

    @Override
    public String toString() {
        return "MaxLimit{" +
                "limit=" + limit +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaxLimit maxLimit = (MaxLimit) o;
        return limit == maxLimit.limit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit);
    }
}
