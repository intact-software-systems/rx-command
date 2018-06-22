package com.intact.rx.policy;

import java.time.Duration;
import java.util.Objects;

/**
 * Timeout represents a duration, i.e., 10 seconds, 1000 seconds, etc.
 */
public final class Timeout {
    private static final Timeout defaultTimeout = new Timeout(Duration.ofMillis(60000L));
    private static final Timeout foreverTimeout = new Timeout(Duration.ofMillis(Long.MAX_VALUE));

    private final Duration timeout;

    public Timeout(Duration timeoutMs) {
        this.timeout = Objects.requireNonNull(timeoutMs);
    }

    public long toMillis() {
        return timeout.toMillis();
    }

    public Duration duration() {
        return timeout;
    }

    public boolean isForever() {
        return timeout.toMillis() == foreverTimeout.toMillis();
    }

    public static Timeout no() {
        return foreverTimeout;
    }

    public static Timeout ofSixtySeconds() {
        return defaultTimeout;
    }

    public static Timeout ofMillis(long msecs) {
        return new Timeout(Duration.ofMillis(msecs));
    }

    public static Timeout ofSeconds(long secs) {
        return new Timeout(Duration.ofSeconds(secs));
    }

    public static Timeout ofMinutes(long minutes) {
        return new Timeout(Duration.ofMinutes(minutes));
    }

    public static Timeout ofHours(long hours) {
        return new Timeout(Duration.ofHours(hours));
    }

    public static Timeout from(Duration duration) {
        return new Timeout(duration);
    }

    @Override
    public String toString() {
        return "Timeout{" +
                "timeout=" + timeout +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Timeout timeout1 = (Timeout) o;
        return Objects.equals(timeout, timeout1.timeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeout);
    }
}
