package com.intact.rx.core.command.status;

/**
 * Keeps track of execution counts
 */
@SuppressWarnings({"SynchronizedMethod", "unused"})
public class ExecutionCount {
    /**
     * Includes successful executions
     */
    private long executionCount;

    /**
     * Includes successful + failed executions
     */
    private long totalExecutionCount;

    /**
     * Total number of attempt on execution
     */
    private long totalAttemptedCount;

    /**
     * Total number of false starts
     */
    private long totalFalseStart;

    /**
     * Total number of fallbacks
     */
    private long totalFallbackCount;

    public ExecutionCount() {
        this.executionCount = 0;
        this.totalExecutionCount = 0;
        this.totalAttemptedCount = 0;
        this.totalFalseStart = 0;
        this.totalFallbackCount = 0;
    }

    private ExecutionCount(ExecutionCount count) {
        this.executionCount = count.executionCount;
        this.totalExecutionCount = count.totalExecutionCount;
        this.totalAttemptedCount = count.totalAttemptedCount;
        this.totalFalseStart = count.totalFalseStart;
        this.totalFallbackCount = count.totalFallbackCount;
    }

    public synchronized ExecutionCount copy() {
        return new ExecutionCount(this);
    }

    public synchronized long getNumSuccesses() {
        return executionCount;
    }

    public synchronized long getNumFailures() {
        return totalExecutionCount - executionCount;
    }

    public synchronized long getTotalFinishedAttempts() {
        return totalExecutionCount;
    }

    public synchronized long getTotalAttempted() {
        return totalAttemptedCount;
    }

    public synchronized long getTotalFalseStart() {
        return totalFalseStart;
    }

    public synchronized long getNumFallback() {
        return totalFallbackCount;
    }

    public synchronized void reset() {
        executionCount = 0;
        totalExecutionCount = 0;
        totalAttemptedCount = 0;
        totalFalseStart = 0;
        totalFallbackCount = 0;
    }

    public synchronized void start() {
        // Note: execution count incremented after done
        totalAttemptedCount++;
        totalFalseStart = 0;
    }

    public synchronized void success() {
        ++totalExecutionCount;
        ++executionCount;
        totalFalseStart = 0;
    }

    public synchronized void failure() {
        // Note: No need to explicitly count failures, it can be deduced from success and total.
        ++totalExecutionCount;
    }

    public synchronized void falseStart() {
        ++totalFalseStart;
    }

    public synchronized void resetFalseStart() {
        totalFalseStart = 0;
    }

    public synchronized void fallback() {
        ++totalFallbackCount;
    }

    @Override
    public synchronized String toString() {
        return "ExecutionCount{" +
                "executionCount=" + executionCount +
                ", totalFinishedCount=" + totalExecutionCount +
                ", totalAttemptedCount=" + totalAttemptedCount +
                ", totalFalseStart=" + totalFalseStart +
                ", totalFallbackCount=" + totalFallbackCount +
                '}';
    }
}
