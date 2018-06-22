package com.intact.rx.policy;

import java.time.Duration;
import java.util.Objects;

/**
 * Frequency: (i.e., the number of occurrences of a repeating event per unit time) Policy on jobs
 * <p>
 * One event for each period.
 */
public class Frequency {
    private static final Duration infinitePeriod = Duration.ofMillis(Long.MAX_VALUE);
    public static final Frequency unlimited = new Frequency(infinitePeriod);

    private final Duration period;

    public Frequency(Duration period) {
        this.period = period;
    }

    public Duration getPeriod() {
        return period;
    }

    public boolean isUnlimited() {
        return period.toMillis() == Long.MAX_VALUE;
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Frequency frequency = (Frequency) o;
        return Objects.equals(period, frequency.period);
    }

    @Override
    public int hashCode() {
        return Objects.hash(period);
    }

    @Override
    public String toString() {
        return "Frequency{" +
                "period=" + period +
                '}';
    }
}
