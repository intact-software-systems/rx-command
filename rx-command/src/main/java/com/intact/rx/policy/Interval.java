package com.intact.rx.policy;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.templates.Utility;
import com.intact.rx.templates.Validate;

/**
 * An interval (represented by a duration time) between possibly repeating events.
 */
public final class Interval {
    private static final Interval everyThreeSecs = nowThenOfMillis(3000);
    private static final Interval everyTenSecs = nowThenOfMillis(10000);
    private static final Interval noInterval = new Interval(Duration.ZERO, Duration.ZERO);

    private final Duration initialDelay;
    private final Duration period;

    public Interval(Duration initialDelay, Duration period) {
        this.initialDelay = requireNonNull(initialDelay);
        this.period = requireNonNull(period);

        Validate.assertTrue(!initialDelay.isNegative());
        Validate.assertTrue(!period.isNegative());
    }

    public long getPeriodMs() {
        return period.toMillis();
    }

    public long getInitialDelayMs() {
        return initialDelay.toMillis();
    }

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static Interval no() {
        return noInterval;
    }

    public static Interval nowThenThreeSeconds() {
        return everyThreeSecs;
    }

    public static Interval nowThenTenSeconds() {
        return everyTenSecs;
    }

    public static Interval nowThenOfMillis(long periodMs) {
        return new Interval(Duration.ZERO, Duration.ofMillis(periodMs));
    }

    public static Interval nowThenOfSeconds(long periodSecs) {
        return new Interval(Duration.ZERO, Duration.ofSeconds(periodSecs));
    }

    public static Interval nowThenOfMinutes(long periodMinute) {
        return new Interval(Duration.ZERO, Duration.ofMinutes(periodMinute));
    }

    public static Interval ofMillis(long periodMs) {
        return new Interval(Duration.ofMillis(periodMs), Duration.ofMillis(periodMs));
    }

    public static Interval ofSeconds(long periodSecs) {
        return new Interval(Duration.ofSeconds(periodSecs), Duration.ofSeconds(periodSecs));
    }

    public static Interval ofMinutes(long periodMinute) {
        return new Interval(Duration.ofMinutes(periodMinute), Duration.ofMinutes(periodMinute));
    }

    public static Interval delayOfMillis(long delayMs, long periodMs) {
        return new Interval(Duration.ofMillis(delayMs), Duration.ofMillis(periodMs));
    }

    public static Interval delayOfSeconds(long delaySecs, long periodSecs) {
        return new Interval(Duration.ofSeconds(delaySecs), Duration.ofSeconds(periodSecs));
    }

    public static Interval delayOfMinutes(long delaySecs, long periodMinutes) {
        return new Interval(Duration.ofSeconds(delaySecs), Duration.ofMinutes(periodMinutes));
    }

    public static Interval atTimeOfDay(LocalTime timeOfDay) {
        return new Interval(Utility.timeUntilNext(timeOfDay), Duration.ofDays(1));
    }

    // --------------------------------------------
    // Overridden from Object
    // --------------------------------------------

    @Override
    public String toString() {
        return "Interval{" +
                "initialDelay=" + initialDelay +
                ", period=" + period +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interval interval = (Interval) o;
        return Objects.equals(initialDelay, interval.initialDelay) &&
                Objects.equals(period, interval.period);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initialDelay, period);
    }
}
