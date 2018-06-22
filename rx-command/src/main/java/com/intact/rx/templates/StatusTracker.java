package com.intact.rx.templates;

import java.time.Duration;
import java.time.Instant;

@SuppressWarnings({"WeakerAccess", "SynchronizedMethod"})
public class StatusTracker {
    private long firstAccess = System.currentTimeMillis();
    private long mostRecentAccess = System.currentTimeMillis();
    private boolean isFirst = true;
    private long totalInstances;
    private long totalSum;

    // ------------------------------------------------------
    // Update counters and timestamps
    // ------------------------------------------------------

    public synchronized void next(long num) {
        if (isFirst) {
            first(num);
        } else {
            nextInternal(num);
        }
    }

    public synchronized void reset(long num) {
        resetInternal();
        first(num);
    }

    public synchronized void reset() {
        resetInternal();
    }

    // ------------------------------------------------------
    // Count accesses
    // ------------------------------------------------------

    public synchronized long totalSum() {
        return totalSum;
    }

    public synchronized long totalInstances() {
        return totalInstances;
    }

    public synchronized boolean isInstances() {
        return totalInstances != 0;
    }

    // ------------------------------------------------------
    // Access absolute timestamps
    // ------------------------------------------------------

    public synchronized Instant firstAccess() {
        return Instant.ofEpochMilli(firstAccess);
    }

    public synchronized Instant mostRecentAccess() {
        return Instant.ofEpochMilli(mostRecentAccess);
    }

    // ------------------------------------------------------
    // Compute time since a point in time
    // ------------------------------------------------------

    public synchronized Duration timeSinceFirstAccess() {
        return Duration.between(Instant.ofEpochMilli(firstAccess), Instant.now());
    }

    public synchronized Duration timeSinceMostRecentAccess() {
        return Duration.between(Instant.ofEpochMilli(mostRecentAccess), Instant.now());
    }

    public synchronized boolean isStatus() {
        return !isFirst;
    }


    private void first(long num) {
        ++totalInstances;

        totalSum += num;
        firstAccess = System.currentTimeMillis();
        isFirst = false;
    }

    private void nextInternal(long num) {
        ++totalInstances;

        totalSum += num;
        mostRecentAccess = System.currentTimeMillis();
    }

    private void resetInternal() {
        totalInstances = 0;
        totalSum = 0;
        firstAccess = System.currentTimeMillis();
        mostRecentAccess = System.currentTimeMillis();
        isFirst = true;
    }
}
