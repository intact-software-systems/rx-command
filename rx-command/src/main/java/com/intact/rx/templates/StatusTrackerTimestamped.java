package com.intact.rx.templates;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

import static com.intact.rx.templates.Validate.assertTrue;

@SuppressWarnings({"WeakerAccess", "SynchronizedMethod"})
public class StatusTrackerTimestamped extends StatusTracker {
    private final long windowDurationMsecs;
    private final ConcurrentHashMap<Long, Long> timestampedCount;

    public StatusTrackerTimestamped(Duration windowDuration) {
        requireNonNull(windowDuration);
        this.windowDurationMsecs = windowDuration.toMillis();
        this.timestampedCount = new ConcurrentHashMap<>();

        assertTrue(windowDurationMsecs > 0);
    }

    public synchronized long sumInWindow() {
        return getSumInWindow(windowDurationMsecs);
    }

    public synchronized long sumInWindow(Duration duration) {
        return getSumInWindow(duration.toMillis());
    }

    public synchronized long numOfInstances() {
        return getNumberOfInstancesInWindow(windowDurationMsecs);
    }

    public synchronized long numOfInstancesIn(Duration duration) {
        return getNumberOfInstancesInWindow(duration.toMillis());
    }

    public synchronized Duration windowSpan() {
        return Duration.ofMillis(windowDurationMsecs);
    }

    // ------------------------------------------------------
    // Update counters and timestamps
    // ------------------------------------------------------

    @Override
    public synchronized void next(long num) {
        super.next(num);
        updateWindow(windowDurationMsecs, num);
    }

    @Override
    public synchronized void reset(long num) {
        super.reset(num);
        resetWindow();
        updateWindow(windowDurationMsecs, num);
    }

    @Override
    public synchronized void reset() {
        super.reset();
        resetWindow();
    }

    // ------------------------------------------------------
    // update count window
    // ------------------------------------------------------

    private long getSumInWindow(long duration) {
        long sum = 0;
        for (Map.Entry<Long, Long> entry : timestampedCount.entrySet()) {
            long timestamp = entry.getKey();
            long sinceStamped = System.currentTimeMillis() - timestamp;
            if (sinceStamped < duration) {
                sum += entry.getValue();
            } else {
                break;
            }
        }
        return sum;
    }

    private long getNumberOfInstancesInWindow(long duration) {
        long counter = 0;
        for (long timestamp : timestampedCount.keySet()) {
            long sinceStamped = System.currentTimeMillis() - timestamp;
            if (sinceStamped < duration) {
                ++counter;
            } else {
                break;
            }
        }
        return counter;
    }

    // ------------------------------------------------------
    // update count window
    // ------------------------------------------------------

    private void updateWindow(long windowDuration, long num) {
        timestampedCount.put(System.currentTimeMillis(), num);

        pruneWindow(windowDuration);
    }

    private void pruneWindow(long windowDuration) {
        Collection<Long> keys = new ArrayList<>();

        for (Map.Entry<Long, Long> entry : timestampedCount.entrySet()) {
            long timestamp = entry.getKey();

            long sinceStamped = System.currentTimeMillis() - timestamp;
            if (sinceStamped >= windowDuration) {
                keys.add(timestamp);
            } else {
                break;
            }
        }

        keys.forEach(timestampedCount::remove);
    }

    private void resetWindow() {
        timestampedCount.clear();
    }
}
