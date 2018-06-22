package com.intact.rx.core.command.status;

import com.intact.rx.templates.Utility;

@SuppressWarnings({"SynchronizedMethod", "unused", "WeakerAccess"})
public class ExecutionTime {
    /**
     * Last successful execution time point
     */
    private long lastExecutionTime;

    /**
     * Last failed execution time point
     */
    private long lastFailedExecutionTime;

    /**
     * Current execution start time point
     */
    private long currentExecutionTime;

    /**
     * Last timepoint for a false start
     */
    private long lastFalseStart;

    /**
     * Last timepoint for fallback
     */
    private long lastFallbackTime;

    public ExecutionTime() {
        lastExecutionTime = 0;
        lastFailedExecutionTime = 0;
        currentExecutionTime = System.currentTimeMillis();
        lastFalseStart = 0;
        lastFallbackTime = 0;
    }

    private ExecutionTime(ExecutionTime time) {
        this.lastExecutionTime = time.lastExecutionTime;
        this.lastFailedExecutionTime = time.lastFailedExecutionTime;
        this.currentExecutionTime = time.currentExecutionTime;
        this.lastFalseStart = time.lastFalseStart;
        this.lastFallbackTime = time.lastFallbackTime;
    }

    public synchronized ExecutionTime copy() {
        return new ExecutionTime(this);
    }

    public synchronized long getLastSuccessfulExecutionTimeMs() {
        return lastExecutionTime;
    }

    public synchronized long getLastFailedExecutionTimeMs() {
        return lastFailedExecutionTime;
    }

    public synchronized long getTimeSinceLastFailedExecutionTimeMs() {
        return System.currentTimeMillis() - lastFailedExecutionTime;
    }

    public synchronized boolean didLastExecutionFail() {
        return lastFailedExecutionTime > lastExecutionTime;
    }

    public synchronized long getCurrentExecutionTimeMs() {
        return currentExecutionTime;
    }

    public synchronized long getTimeSinceLastExecutionTimeMs() {
        return System.currentTimeMillis() - currentExecutionTime;
    }

    public synchronized long getTimeSinceLastFalseStart() {
        return System.currentTimeMillis() - lastFalseStart;
    }

    public synchronized long getTimeSinceFallback() {
        return System.currentTimeMillis() - lastFallbackTime;
    }

    public synchronized void reset() {
        lastExecutionTime = 0;
        lastFailedExecutionTime = 0;
        currentExecutionTime = System.currentTimeMillis();
        lastFalseStart = 0;
        lastFallbackTime = 0;
    }

    public synchronized void start() {
        currentExecutionTime = System.currentTimeMillis();
    }

    public synchronized void success() {
        lastExecutionTime = currentExecutionTime;
    }

    public synchronized void failure() {
        lastFailedExecutionTime = currentExecutionTime;
    }

    public synchronized void falseStart() {
        lastFalseStart = System.currentTimeMillis();
    }

    public synchronized void fallback() {
        lastFallbackTime = System.currentTimeMillis();
    }

    @Override
    public synchronized String toString() {
        return "ExecutionTime{" +
                "lastExecutionTime=" + Utility.msecDateTimeFormatter(lastExecutionTime) +
                ", lastFailedExecutionTime=" + Utility.msecDateTimeFormatter(lastFailedExecutionTime) +
                ", currentExecutionTime=" + Utility.msecDateTimeFormatter(currentExecutionTime) +
                ", lastFalseStart=" + Utility.msecDateTimeFormatter(lastFalseStart) +
                ", lastFallbackTime=" + Utility.msecDateTimeFormatter(lastFallbackTime) +
                '}';
    }
}