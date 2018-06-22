package com.intact.rx.policy;

import java.util.Objects;

import com.intact.rx.templates.Validate;

public final class MementoPolicy {
    public static final MementoPolicy none = new MementoPolicy(0, 0);

    private final int undoDepth;
    private final int redoDepth;

    public MementoPolicy(int undoDepth, int redoDepth) {
        Validate.assertTrue(undoDepth >= 0);
        Validate.assertTrue(redoDepth >= 0);

        this.undoDepth = undoDepth;
        this.redoDepth = redoDepth;
    }

    public boolean isAnyDepth() {
        return undoDepth > 0 || redoDepth > 0;
    }

    public int getUndoDepth() {
        return undoDepth;
    }

    public int getRedoDepth() {
        return redoDepth;
    }

    @Override
    public String toString() {
        return "MementoPolicy{" +
                "undoDepth=" + undoDepth +
                ", redoDepth=" + redoDepth +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MementoPolicy)) return false;
        MementoPolicy that = (MementoPolicy) o;
        return undoDepth == that.undoDepth &&
                redoDepth == that.redoDepth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(undoDepth, redoDepth);
    }
}
