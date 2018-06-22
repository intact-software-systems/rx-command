package com.intact.rx.policy;

import java.time.Duration;
import java.util.Objects;

public final class Lifetime {
    private static final Lifetime foreverSpan = new Lifetime(Duration.ofMillis(Long.MAX_VALUE));
    private static final Lifetime zeroSpan = new Lifetime(Duration.ZERO);

    private final Duration expirationPoint;

    public Lifetime(Duration expirationPoint) {
        this.expirationPoint = Objects.requireNonNull(expirationPoint);
    }

    public long inMillis() {
        return expirationPoint.toMillis();
    }

    public Duration duration() {
        return expirationPoint;
    }

    public static Lifetime forever() {
        return foreverSpan;
    }

    public static Lifetime zero() {
        return zeroSpan;
    }

    public static Lifetime until(Duration expirationPoint) {
        return new Lifetime(expirationPoint);
    }

    public static Lifetime ofSeconds(long seconds) {
        return new Lifetime(Duration.ofSeconds(seconds));
    }

    public static Lifetime ofMinutes(long minutes) {
        return new Lifetime(Duration.ofMinutes(minutes));
    }

    public static Lifetime ofHours(long hours) {
        return new Lifetime(Duration.ofHours(hours));
    }

    public static Lifetime ofDays(long days) {
        return new Lifetime(Duration.ofDays(days));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + expirationPoint + "]";
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lifetime)) return false;
        Lifetime lifetime = (Lifetime) o;
        return Objects.equals(expirationPoint, lifetime.expirationPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expirationPoint);
    }
}
