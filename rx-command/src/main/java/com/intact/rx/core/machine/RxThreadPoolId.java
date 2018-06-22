package com.intact.rx.core.machine;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class RxThreadPoolId {
    private final String id;

    private RxThreadPoolId(String id) {
        this.id = requireNonNull(id);
    }

    public String getId() {
        return id;
    }

    public static RxThreadPoolId create(String id) {
        return new RxThreadPoolId(id);
    }

    @Override
    public String toString() {
        return "RxThreadPoolId{" +
                "id='" + id + '\'' +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RxThreadPoolId that = (RxThreadPoolId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
