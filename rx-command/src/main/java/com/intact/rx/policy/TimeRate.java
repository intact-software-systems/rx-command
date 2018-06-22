package com.intact.rx.policy;

import java.time.Duration;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.templates.Validate;

@SuppressWarnings("WeakerAccess")
public final class TimeRate {
    public static final TimeRate unlimited = new TimeRate(Long.MAX_VALUE, Duration.ZERO);

    private final long total;
    private final Duration duration;

    public TimeRate(long total, Duration duration) {
        this.total = total;
        this.duration = requireNonNull(duration);

        Validate.assertTrue(total > 0);
    }

    public long getTotal() {
        return total;
    }

    public Duration getDuration() {
        return duration;
    }

    public boolean isUnlimited() {
        return total == Long.MAX_VALUE;
    }


    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeRate timeRate = (TimeRate) o;
        return total == timeRate.total &&
                Objects.equals(duration, timeRate.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, duration);
    }

    @Override
    public String toString() {
        return "TimeRate{" +
                "total=" + total +
                ", duration=" + duration +
                '}';
    }
}
