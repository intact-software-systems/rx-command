package com.intact.rx.core.command.status;

/**
 * TODO: isSuccess doesn't make sense if policy indicates multiple executions.
 */
@SuppressWarnings({"SynchronizedMethod", "unused"})
public class ExecutionStatus {
    public enum Type {
        /**
         * If command has not been touched in any way.
         */
        NO,

        /**
         * If command is currently about to be executed.
         */
        STARTING,

        /**
         * If command is currently being executed.
         */
        STARTED,

        // == SUCCESS
        NEXT,

        // == FINAL/STOPPED
        COMPLETED,

        /**
         * If command is currently suspended
         */
        SUSPENDED,

        /**
         * If command has been suspended and then now running
         */
        RESUMED,

        /**
         * If command was cancelled during execution.
         */
        CANCELLED,

        TIMEDOUT,

        /**
         * If command was cancelled during execution.
         */
        // == ERROR
        FAILURE,

        /**
         * If command was successfully executed.
         */
        STOPPED
    }

    /**
     * Counts executions
     */
    private final ExecutionCount count;

    /**
     * Tracks execution times
     */
    private final ExecutionTime time;

    private Type state;

    // ------------------------------------------------------
    // Constructor
    // ------------------------------------------------------

    public ExecutionStatus() {
        count = new ExecutionCount();
        time = new ExecutionTime();
        state = Type.NO;
    }

    private ExecutionStatus(ExecutionCount count, ExecutionTime time, Type state) {
        this.count = count;
        this.time = time;
        this.state = state;
    }

    public synchronized ExecutionStatus copy() {
        return new ExecutionStatus(count.copy(), time.copy(), state);
    }

    // ------------------------------------------------------
    // Simple getters
    // ------------------------------------------------------

    public ExecutionCount getCount() {
        return count;
    }

    public ExecutionTime getTime() {
        return time;
    }

    // ------------------------------------------------------
    // Called when execution starts and stops
    // ------------------------------------------------------

    public synchronized void starting() {
        state = Type.STARTING;
    }

    public synchronized void start() {
        count.start();
        time.start();
        state = Type.STARTED;
    }

    public synchronized void reset() {
        count.reset();
        time.reset();
        state = Type.NO;
    }

    // Complete()
    public synchronized void success() {
        count.success();
        time.success();
        state = Type.COMPLETED;
    }

    // Error()
    public synchronized void failure() {
        count.failure();
        time.failure();
        state = Type.FAILURE;
    }

    public synchronized void cancel() {
        count.failure();
        time.failure();
        state = Type.CANCELLED;
    }

//    public synchronized void Timeout() {
//        count.Timeout();
//        time.Timeout();
//        state = Type.TIMEDOUT;
//    }

    public synchronized void fallback() {
        count.fallback();
        time.fallback();
    }

    public synchronized void falseStart() {
        count.falseStart();
        time.falseStart();
    }

    public synchronized void resetFalseStartCount() {
        count.resetFalseStart();
    }

    public synchronized boolean isExecuting() {
        return state == Type.STARTED || state == Type.STARTING;
    }

    public synchronized boolean isStarting() {
        return state == Type.STARTING;
    }

    public synchronized boolean isFalseStart() {
        // TODO: Review entire status handling
        return time.getTimeSinceLastFalseStart() <= 100;
    }

    // isComplete()
    public synchronized boolean isSuccess() {
        return state == Type.COMPLETED;
    }

    public synchronized boolean isCancelled() {
        return state == Type.CANCELLED;
    }

    public synchronized boolean isFailureOnLastExecutionAttempt() {
        return getTime().didLastExecutionFail();
    }

    public synchronized boolean isNeverExecuted() {
        return getCount().getTotalAttempted() <= 0;
    }

    public synchronized boolean isEverExecuted() {
        return getCount().getTotalAttempted() > 0;
    }

    @Override
    public synchronized String toString() {
        return "ExecutionStatus{" +
                "count=" + count +
                ", time=" + time +
                ", state=" + state +
                '}';
    }
}
