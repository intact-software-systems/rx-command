package com.intact.rx.templates;

import java.util.Objects;

public final class Validate {

    private Validate() {
    }

    public static void requireNoNullElements(Object[] actions) {
        Objects.requireNonNull(actions);
        for (Object action : actions) {
            Objects.requireNonNull(action);
        }
    }

    public static void assertTrue(boolean valid) {
        if (!valid) {
            throw new IllegalStateException("Boolean must be true, but was false!");
        }
    }

    public static void assertTrue(boolean valid, String msg) {
        if (!valid) {
            throw new IllegalStateException("Boolean must be true, but was false!: " + msg);
        }
    }
}
