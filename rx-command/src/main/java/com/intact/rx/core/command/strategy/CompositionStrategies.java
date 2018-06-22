package com.intact.rx.core.command.strategy;

import java.util.Optional;

import com.intact.rx.api.command.Action0;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.action.FallbackStrategyAction;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreaker;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerCache;
import com.intact.rx.core.rxcircuit.rate.RateLimiterCache;
import com.intact.rx.exception.*;

@SuppressWarnings("WeakerAccess")
public final class CompositionStrategies {

    public static <T> boolean compose(CommandPolicy policy, Command<T> command) {
        final CircuitBreaker circuitBreaker = CircuitBreakerCache.circuit(command.getCircuitBreakerId(), policy.getCircuitBreakerPolicy());
        final boolean isCircuitClosed = circuitBreaker.allowRequest();

        final boolean isRateLimiterPolicyMet = RateLimiterCache.rateLimiter(command.getRateLimiterId(), policy.getRateLimiterPolicy()).allowRequest();

        final boolean allowRequest = isCircuitClosed && isRateLimiterPolicyMet;

        boolean criterionMet = false;
        if (allowRequest) {
            try {
                command.onSubscribe(command);
                criterionMet = callActions(policy, command, new ExecutionStatus());
            } catch (Throwable throwable) {
                command.onError(throwable);
                return false;
            }
        }

        if (!command.isCancelled() && !criterionMet && ExecutionPolicyChecker.isNAttemptsLeft(command.getExecutionStatus(), policy.getAttempt(), 1) || !allowRequest) {
            criterionMet = fallback(command, command.getActions(), policy.isErrorOnNull());
        }

        if (criterionMet) {
            circuitBreaker.success();
            command.onComplete();
        } else if (!isCircuitClosed) {
            command.onError(new CircuitBreakerOpenException("Command failed fast, circuit OPEN[" + command.getCircuitBreakerId() + "] [" + command + "]"));
        } else if (!isRateLimiterPolicyMet) {
            command.onError(new RateLimitViolatedException("Command failed fast, rate limiter VIOLATED[" + command.getRateLimiterId() + "] [" + command + "]"));
        } else { //  == !criterionMet && allowRequest
            circuitBreaker.failure();
            command.onError(new PolicyViolationException("Success criterion not met while executing actions: [" + command + "] "));
        }

        return true;
    }

    public static <T> boolean fallback(Command<T> command, Iterable<Action0<T>> actions, boolean errorOnNull) {
        ExecutionStatus status = new ExecutionStatus();
        try {
            for (Action0<T> action : actions) {
                if (command.isCancelled()) {
                    break;
                }

                if (action instanceof FallbackStrategyAction) {
                    FallbackStrategyAction<T> fallbackAction = (FallbackStrategyAction<T>) action;
                    Optional<T> result = callAction(command, fallbackAction.getFallback(), status, errorOnNull);

                    if (status.isSuccess()) {
                        command.onFallback(action);
                        result.ifPresent(command::onNext);
                    }
                }
            }
        } catch (Throwable throwable) {
            command.onError(throwable);
            return false;
        }

        return status.isEverExecuted();
    }

    public static <T> boolean callActions(CommandPolicy policy, Command<T> command, ExecutionStatus status) {
        boolean criterionMet = true;
        for (Action0<T> action : command.getActions()) {

            if (command.isCancelled()) {
                criterionMet = ExecutionPolicyChecker.isCriterionMet(status, policy.getSuccessCriterion());
                status.cancel();
                break;
            } else if (!criterionMet) {
                break;
            }

            Optional<T> result = callAction(command, action, status, policy.isErrorOnNull());

            criterionMet = ExecutionPolicyChecker.isCriterionMet(status, policy.getSuccessCriterion());
            if (criterionMet) {
                result.ifPresent(command::onNext);
            }
        }
        return criterionMet;
    }

    public static <T> Optional<T> callAction(Command<T> command, Action0<T> action, ExecutionStatus status, boolean errorOnNull) throws FatalException, CancelCommandException {
        Optional<T> value = Optional.empty();
        Throwable actionException = null;

        // -----------------------------------------
        // Update status and call action
        // -----------------------------------------

        try {
            command.onSubscribe(action, command);

            status.start();

            action.before();

            value = action.execute();
        } catch (Throwable e) {
            actionException = e;
        } finally {
            action.after();

            if (errorOnNull && !value.isPresent() && !action.isVoid() && actionException == null) {
                actionException = new PolicyViolationException("Command action returned null and policy ERROR_ON_EXCEPTION_AND_NULL is enabled");
            }
        }

        // -----------------------------------------
        // Callbacks and status update after action execution
        // -----------------------------------------

        if (actionException != null) {
            // -----------------------------------------
            // Action threw exception
            // -----------------------------------------

            command.onError(action, actionException);

            if (actionException instanceof CancelCommandException) {
                status.cancel();
                throw (CancelCommandException) actionException;
            } else if (actionException instanceof FatalException) {
                status.cancel();
                throw (FatalException) actionException;
            } else {
                status.failure();
            }
        } else {
            // -----------------------------------------
            // Action completed without throwing exception
            // -----------------------------------------
            value.ifPresent(val -> command.onNext(action, val));
            command.onComplete(action);
            status.success();
        }

        return value;
    }

    private CompositionStrategies() {
    }
}
