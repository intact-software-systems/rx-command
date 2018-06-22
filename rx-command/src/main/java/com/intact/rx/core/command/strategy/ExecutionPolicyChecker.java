package com.intact.rx.core.command.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.policy.*;

public final class ExecutionPolicyChecker {
    private static final Logger log = LoggerFactory.getLogger(ExecutionPolicyChecker.class);

    private static final long oneThousandMilliseconds = 1000;
    private static final long acceptableDeviationMilliseconds = 1000;

    private ExecutionPolicyChecker() {
    }

    /**
     * Check execution time compared to policy Lifetime.
     */
    @SuppressWarnings("unused")
    public static boolean isInLifetime(final ExecutionStatus status, final Lifetime lifetime) {
        return status.isExecuting() && !status.isCancelled() && status.getTime().getTimeSinceLastExecutionTimeMs() <= lifetime.inMillis();
    }

    public static boolean isNExecutionsLeft(final ExecutionStatus executionStatus, final Attempt attempt, long n) {
        long totalAttempted = executionStatus.getCount().getTotalAttempted() + n;
        long successCount = executionStatus.getCount().getNumSuccesses() + n;

        return totalAttempted >= attempt.getMaxNumAttempts() || successCount >= attempt.getMinNumSuccesses();
    }

    static boolean isNAttemptsLeft(final ExecutionStatus executionStatus, final Attempt attempt, long n) {
        return executionStatus.getCount().getTotalAttempted() + n >= attempt.getMaxNumAttempts();
    }

    /**
     * Check execution state compared to Attempt policy and deduce if command can be executed.
     */
    public static boolean isInAttempt(final ExecutionStatus executionStatus, final Attempt attempt) {
        // ---------------------------------------
        // If command is cancelled it is considered done
        // --------------------------------------
        if (executionStatus.isCancelled()) {
            return false;
        }

        boolean executeCommand = false;

        // ---------------------------------------
        // Check execution counts
        // ---------------------------------------
        switch (attempt.getKind()) {
            case FOREVER: {
                executeCommand = true;
                break;
            }
            case NUM_SUCCESSFUL_TIMES: {
                // ------------------------------------------------------------------------------------------------
                // if "number of successful executions" < "required number of successful executions" then doExecute
                // ------------------------------------------------------------------------------------------------
                if (executionStatus.getCount().getNumSuccesses() < attempt.getMinNumSuccesses()) {
                    executeCommand = true;
                }

                // ------------------------------------------------------------------------------------------------
                // if "total number of attempts" >= "maximum number of attempts" then doNotExecute
                // ------------------------------------------------------------------------------------------------
                if (executionStatus.getCount().getTotalAttempted() >= attempt.getMaxNumAttempts()) {
                    executeCommand = false;
                }
                break;
            }
            case UNTIL_SUCCESS: {
                // ------------------------------------------------------------------------------------------------
                // if "number of successful executions" == 0 then doExecute
                // ------------------------------------------------------------------------------------------------
                if (executionStatus.getCount().getNumSuccesses() == 0) {
                    executeCommand = true;
                }

                // ------------------------------------------------------------------------------------------------
                // if "total number of attempts" >= "maximum number of attempts" then doNotExecute
                // ------------------------------------------------------------------------------------------------
                if (executionStatus.getCount().getTotalAttempted() >= attempt.getMaxNumAttempts()) {
                    executeCommand = false;
                }

                break;
            }
            //noinspection UnnecessaryDefault
            default: {
                log.error("Unidentified attempt type {}!", attempt.getKind());
                break;
            }
        }

        return executeCommand;
    }

    /**
     * Check execution state in comparison to Interval policy.
     */
    private static boolean isInInterval(final ExecutionStatus executionStatus, final Interval interval) {
        if (executionStatus.isFailureOnLastExecutionAttempt()) {
            return false;
        }

        // ------------------------------------------------------------------------------------------------
        // if "never run before" and "time since command creation" > "initial delayed start"
        // ------------------------------------------------------------------------------------------------
        //noinspection IfStatementWithIdenticalBranches
        if (executionStatus.isNeverExecuted() && executionStatus.getTime().getTimeSinceLastExecutionTimeMs() >= interval.getInitialDelayMs()) {
            return true;
        }
        // ------------------------------------------------------------------------------------------------
        // (else if implied "run before") and if "time since last execution" >= "configured minimum interval period"
        // ------------------------------------------------------------------------------------------------
        else if (executionStatus.getTime().getTimeSinceLastExecutionTimeMs() >= interval.getPeriodMs()) {
            return true;
        }

        // ------------------------------------------------------------------------------------------------
        // if "time since last execution" < "configured minimum interval period"
        // ------------------------------------------------------------------------------------------------
        //else if(executionState.getExecutionTime().getTimeSinceLastExecutionTimeMs() < interval.getPeriodMs())
        //    return false;

        // Post invariant: executionStatus.getTime().getTimeSinceLastExecutionTimeMs() < interval.getPeriodMs();
        return false;
    }

    /**
     * Check execution state in comparison to retry Interval policy.
     */
    private static boolean isInRetryInterval(final ExecutionStatus executionStatus, final Interval retryInterval) {
        if (!executionStatus.isFailureOnLastExecutionAttempt()) {
            return false;
        }

        // ------------------------------------------------------------------------------------------------
        // if "never run before" and "time since command creation" > "initial delayed start"
        // ------------------------------------------------------------------------------------------------
        //noinspection IfStatementWithIdenticalBranches
        if (executionStatus.getCount().getNumFailures() == 1 && executionStatus.getTime().getTimeSinceLastFailedExecutionTimeMs() >= retryInterval.getInitialDelayMs()) {
            return true;
        }
        // ------------------------------------------------------------------------------------------------
        // (else if implied "run before") and if "time since last execution" >= "configured minimum interval period"
        // ------------------------------------------------------------------------------------------------
        else if (executionStatus.getTime().getTimeSinceLastFailedExecutionTimeMs() >= retryInterval.getPeriodMs()) {
            return true;
        }

        return false;
    }

    /**
     * Compute the next execution time based on execution state and Interval policy.
     */
    public static long computeTimeUntilNextExecutionTimeMs(final ExecutionStatus executionStatus, final Interval interval) {
        if (executionStatus.isFailureOnLastExecutionAttempt()) {
            return Long.MAX_VALUE;
        }

        if (executionStatus.isNeverExecuted()) {
            // Time until execute according to initialDelayMs
            return Math.max(0, interval.getInitialDelayMs() - executionStatus.getTime().getTimeSinceLastExecutionTimeMs());
        }

        // Time until execute according to periodMs
        long intervalMs = Math.max(0, interval.getPeriodMs() - executionStatus.getTime().getTimeSinceLastExecutionTimeMs());
        return executionStatus.isExecuting() && intervalMs <= 0
                ? Math.min(interval.getPeriodMs(), oneThousandMilliseconds)  // back off if still executing
                : intervalMs;
    }

    /**
     * Compute the next execution time based on execution state and Interval policy.
     */
    private static long computeTimeUntilNextRetryTimeMs(final ExecutionStatus executionStatus, final Interval interval) {
        if (!executionStatus.isFailureOnLastExecutionAttempt()) {
            return Long.MAX_VALUE;
        }

        if (executionStatus.getCount().getNumFailures() == 1) {
            // Time until retry if failed once
            long initialMs = Math.max(0, interval.getInitialDelayMs() - executionStatus.getTime().getTimeSinceLastFailedExecutionTimeMs());
            return executionStatus.isExecuting() && initialMs <= 0
                    ? Math.min(interval.getPeriodMs(), oneThousandMilliseconds)  // back off if still executing
                    : initialMs;
        }

        // Time until retry if failed more than once
        long intervalMs = Math.max(0, interval.getPeriodMs() - executionStatus.getTime().getTimeSinceLastFailedExecutionTimeMs());
        return executionStatus.isExecuting() && intervalMs <= 0
                ? Math.min(interval.getPeriodMs(), oneThousandMilliseconds)  // back off if still executing
                : intervalMs;
    }

    /**
     * Compute time in ms until timeout.
     */
    public static long computeTimeUntilTimeoutMs(final ExecutionStatus executionStatus, final Timeout timeout) {
        if (!executionStatus.isExecuting() || timeout.isForever()) {
            return Long.MAX_VALUE;
        }

        return Math.max(0, timeout.toMillis() - executionStatus.getTime().getTimeSinceLastExecutionTimeMs());
    }

    /**
     * Compute earliest time a command can be rescheduled.
     */
    public static long computeEarliestReadyInMs(final ExecutionStatus status,
                                                final Attempt attempt,
                                                final Interval interval,
                                                final Interval retryInterval,
                                                final Timeout timeout) {
        long nextExecutionTimeMs = Long.MAX_VALUE;
        long nextRetryTimeMs = Long.MAX_VALUE;

        if (isInAttempt(status, attempt)) {
            nextExecutionTimeMs = computeTimeUntilNextExecutionTimeMs(status, interval);
            nextRetryTimeMs = computeTimeUntilNextRetryTimeMs(status, retryInterval);
        }

        long timeUntilTimeoutMs = computeTimeUntilTimeoutMs(status, timeout);

        // -----------------------------------------
        // Compute earliest ready time based on policies and status
        // -----------------------------------------
        long earliestReadyTimeMs = Long.MAX_VALUE;
        earliestReadyTimeMs = Math.min(earliestReadyTimeMs, nextExecutionTimeMs);
        earliestReadyTimeMs = Math.min(earliestReadyTimeMs, nextRetryTimeMs);
        earliestReadyTimeMs = Math.min(earliestReadyTimeMs, timeUntilTimeoutMs);

//        if (earliestReadyTimeMs == Long.MAX_VALUE) {
//            log.debug("{}/{}: Earliest ready time FOREVER. isExecuting = {}, nextExecutionTimeMs = {}, nextRetryTimeMs = {}, timeUntilTimeoutMs = {}",
//                    status, attempt, status.isExecuting(), nextExecutionTimeMs, nextRetryTimeMs, timeUntilTimeoutMs);
//        }
//        else if(earliestReadyTimeMs == 0)
//            log.warn("Earliest ready time NOW. isExecuting = " + status.isExecuting() + ", nextExecutionTimeMs = " + nextExecutionTimeMs + ", nextRetryTimeMs = " + nextRetryTimeMs + ", timeUntilTimeoutMs = " + timeUntilTimeoutMs);
//        else
//            log.warn("Earliest ready time " + earliestReadyTimeMs + " ms. isExecuting = " + status.isExecuting() + ", nextExecutionTimeMs = " + nextExecutionTimeMs + ", nextRetryTimeMs = " + nextRetryTimeMs + ", timeUntilTimeoutMs = " + timeUntilTimeoutMs);

        if (status.isFalseStart() && earliestReadyTimeMs <= 0) {
            earliestReadyTimeMs = computeBackoffMs(status, oneThousandMilliseconds);
            log.warn("False start - backing off and making new attempt in {} ms", earliestReadyTimeMs);
        }

        return earliestReadyTimeMs;
    }

    /**
     * Compute earliest time a command can be rescheduled.
     */
    public static long computeEarliestReadyInMs(final ExecutionStatus status,
                                                final Interval interval,
                                                final Interval retryInterval,
                                                final Timeout timeout) {
        long nextExecutionTimeMs = computeTimeUntilNextExecutionTimeMs(status, interval);
        long nextRetryTimeMs = computeTimeUntilNextRetryTimeMs(status, retryInterval);
        long timeUntilTimeoutMs = computeTimeUntilTimeoutMs(status, timeout);

        // -----------------------------------------
        // Compute earliest ready time based on policies and status
        // -----------------------------------------
        long earliestReadyTimeMs = Long.MAX_VALUE;
        earliestReadyTimeMs = Math.min(earliestReadyTimeMs, nextExecutionTimeMs);
        earliestReadyTimeMs = Math.min(earliestReadyTimeMs, nextRetryTimeMs);
        earliestReadyTimeMs = Math.min(earliestReadyTimeMs, timeUntilTimeoutMs);

//        if (earliestReadyTimeMs == Long.MAX_VALUE) {
//            log.debug("{}/{}: Earliest ready time FOREVER. isExecuting = {}, nextExecutionTimeMs = {}, nextRetryTimeMs = {}, timeUntilTimeoutMs = {}",
//                    status, attempt, status.isExecuting(), nextExecutionTimeMs, nextRetryTimeMs, timeUntilTimeoutMs);
//        }
//        else if(earliestReadyTimeMs == 0)
//            log.warn("Earliest ready time NOW. isExecuting = " + status.isExecuting() + ", nextExecutionTimeMs = " + nextExecutionTimeMs + ", nextRetryTimeMs = " + nextRetryTimeMs + ", timeUntilTimeoutMs = " + timeUntilTimeoutMs);
//        else
//            log.warn("Earliest ready time " + earliestReadyTimeMs + " ms. isExecuting = " + status.isExecuting() + ", nextExecutionTimeMs = " + nextExecutionTimeMs + ", nextRetryTimeMs = " + nextRetryTimeMs + ", timeUntilTimeoutMs = " + timeUntilTimeoutMs);

        if (status.isFalseStart() && earliestReadyTimeMs <= 0) {
            earliestReadyTimeMs = computeBackoffMs(status, oneThousandMilliseconds);
            log.warn("False start - backing off and making new attempt in {} ms", earliestReadyTimeMs);
        }

        return earliestReadyTimeMs;
    }

    private static long computeBackoffMs(ExecutionStatus status, long exponentialBackoffStartMs) {
        return exponentialBackoffStartMs * status.getCount().getTotalFalseStart();
    }

    /**
     * Check if policy and status resolves to command ready to execute.
     */
    public static boolean isReadyToExecute(final ExecutionStatus executionStatus,
                                           final Attempt attempt,
                                           final Interval interval,
                                           final Interval retryInterval) {
        boolean inLifetime = isInAttempt(executionStatus, attempt);
        boolean inInterval = isInInterval(executionStatus, interval);
        boolean inRetryInterval = isInRetryInterval(executionStatus, retryInterval);

        // ---------------------------------------
        // Q: Is Timeout policy relevant here?
        // A: No, it is handled in controller
        // ---------------------------------------

        return inLifetime && inInterval || inLifetime && inRetryInterval;
    }

    public static boolean isReadyToExecute(final ExecutionStatus executionStatus,
                                           final Interval interval,
                                           final Interval retryInterval) {
        boolean inInterval = isInInterval(executionStatus, interval);
        boolean inRetryInterval = isInRetryInterval(executionStatus, retryInterval);

        // ---------------------------------------
        // Q: Is Timeout policy relevant here?
        // A: No, it is handled in controller
        // ---------------------------------------

        return inInterval || inRetryInterval;
    }

    /**
     * Check if the execution has timed out according to Timeout policy.
     */
    public static boolean isTimeout(final ExecutionStatus executionStatus, final Timeout timeout) {
        return executionStatus.isExecuting() && executionStatus.getTime().getTimeSinceLastExecutionTimeMs() >= timeout.toMillis();
    }

    /**
     * Check for policy violation
     */
    public static boolean isPolicyViolated(final ExecutionStatus executionStatus,
                                           final Attempt attempt,
                                           final Interval interval,
                                           final Interval retryInterval,
                                           final Timeout timeout) {
        if (isPolicyViolated(executionStatus, attempt)) {
            return true;
        }

        if (isInAttempt(executionStatus, attempt)) {
            if (isPolicyViolated(executionStatus, attempt, interval, retryInterval)) {
                return true;
            }

            if (isPolicyViolated(executionStatus, timeout)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check for policy violation
     */
    private static boolean isPolicyViolated(final ExecutionStatus executionStatus, final Attempt policy) {
        if (executionStatus.getCount().getTotalAttempted() > policy.getMaxNumAttempts()) {
            if (log.isWarnEnabled()) {
                log.warn("{}: Attempt policy violated (total num attempts > max num attempts): {} > {}",
                        executionStatus, executionStatus.getCount().getTotalAttempted(), policy.getMaxNumAttempts());
            }
            return true;
        } else if (executionStatus.getCount().getNumSuccesses() > policy.getMinNumSuccesses()) {
            if (log.isWarnEnabled()) {
                log.warn("{}: Attempt policy violated (number of successes > min num successes): {} > {}",
                        executionStatus, executionStatus.getCount().getNumSuccesses(), policy.getMinNumSuccesses());
            }
            return true;
        }

        return false;
    }

    /**
     * Check for policy violation
     */
    private static boolean isPolicyViolated(final ExecutionStatus executionStatus, Attempt attempt, final Interval interval, final Interval retryInterval) {
        return !executionStatus.isCancelled() && isRetryIntervalPolicyViolated(executionStatus, attempt, retryInterval) || isIntervalPolicyVioldated(executionStatus, attempt, interval);
    }

    private static boolean isIntervalPolicyVioldated(ExecutionStatus executionStatus, Attempt attempt, Interval interval) {
        if (executionStatus.isFailureOnLastExecutionAttempt()) {
            return false;
        }

        if (executionStatus.isNeverExecuted()) {
            if (executionStatus.getTime().getTimeSinceLastExecutionTimeMs() > interval.getInitialDelayMs() + acceptableDeviationMilliseconds) {
                if (log.isWarnEnabled()) {
                    log.warn("{}: Interval policy violated (Time since last execution time > initial delay): {} > {}",
                            executionStatus, executionStatus.getTime().getTimeSinceLastExecutionTimeMs(), interval.getInitialDelayMs());
                }
                return true;
            }

        } else if (attempt.getMinNumSuccesses() > 1) {
            long periodMs = interval.getPeriodMs() == Long.MAX_VALUE ? interval.getPeriodMs() : interval.getPeriodMs() + acceptableDeviationMilliseconds;

            if (executionStatus.getTime().getTimeSinceLastExecutionTimeMs() > periodMs) {
                if (log.isWarnEnabled()) {
                    log.warn("{}: Interval policy violated (Time since last execution time > period): {} > {}",
                            executionStatus, executionStatus.getTime().getTimeSinceLastExecutionTimeMs(), interval.getPeriodMs());
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isRetryIntervalPolicyViolated(ExecutionStatus executionStatus, Attempt attempt, Interval retryInterval) {
        if (!executionStatus.isFailureOnLastExecutionAttempt()) {
            return false;
        }

        if (executionStatus.getCount().getNumFailures() == 1) {
            if (executionStatus.getTime().getTimeSinceLastFailedExecutionTimeMs() > retryInterval.getInitialDelayMs() + acceptableDeviationMilliseconds) {
                if (log.isWarnEnabled()) {
                    log.warn("{}: Retry Interval policy violated (Time since last execution time > initial delay): {} > {}",
                            executionStatus, executionStatus.getTime().getTimeSinceLastExecutionTimeMs(), retryInterval.getInitialDelayMs());
                }
                return true;
            }

        } else if (attempt.getMaxNumAttempts() > attempt.getMinNumSuccesses()) {
            long periodMs = retryInterval.getPeriodMs() == Long.MAX_VALUE ? retryInterval.getPeriodMs() : retryInterval.getPeriodMs() + acceptableDeviationMilliseconds;

            if (executionStatus.getTime().getTimeSinceLastFailedExecutionTimeMs() > periodMs) {
                if (log.isWarnEnabled()) {
                    log.warn("{}: Retry Interval policy violated (Time since last execution time > period): {} > {}",
                            executionStatus, executionStatus.getTime().getTimeSinceLastExecutionTimeMs(), retryInterval.getPeriodMs());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Check for policy violation
     */
    private static boolean isPolicyViolated(final ExecutionStatus executionStatus, final Timeout policy) {
        if (executionStatus.isExecuting() && executionStatus.getTime().getTimeSinceLastExecutionTimeMs() >= policy.toMillis()) {
            if (log.isWarnEnabled()) {
                log.warn("{}: Timeout policy violated (Time since last current execution started > timeout period): {} > {}",
                        executionStatus, executionStatus.getTime().getTimeSinceLastExecutionTimeMs(), policy.toMillis());
            }
            return true;
        }

        return false;
    }

    /**
     * Check for policy violation
     */
    static boolean isCriterionMet(final ExecutionStatus status, final Criterion successCriterion) {
        if (successCriterion.isAll()) {
            if (status.getCount().getNumFailures() > 0) {
                return false;
            }
        } else if (successCriterion.isMinimum()) {
            if (status.getCount().getNumFailures() > successCriterion.getMinLimit().getLimit()) {
                return false;
            }
        }

        // Note! Unconditional criterion is always true
        return true;
    }
}
