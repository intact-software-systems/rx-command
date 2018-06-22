package com.intact.rx.core.rxcache.acts;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.command.Action0;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.machine.context.RxThreadPoolConfig;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.api.Act;
import com.intact.rx.core.rxcache.factory.RxCacheFactory;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.exception.ExecutionEndedOnErrorException;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.api.Context;

@SuppressWarnings({"SynchronizedMethod", "AccessToStaticFieldLockedOnInstance"})
public class ActGroup {
    private static final Logger log = LoggerFactory.getLogger(ActGroup.class);

    private final Context<ActGroupPolicy, ActGroupState> context;

    private ActGroup(ActGroupPolicy policy) {
        this.context = new ContextObject<>(policy, new ActGroupState());
    }

    // -----------------------------------------------------------
    // Factory functions
    // -----------------------------------------------------------

    public static ActGroup sequential() {
        return new ActGroup(ActGroupPolicy.sequential);
    }

    public static ActGroup parallel() {
        return new ActGroup(ActGroupPolicy.parallel);
    }

    // -----------------------------------------------------------
    // Access statuses and state
    // -----------------------------------------------------------

    public List<Act<?, ?>> getList() {
        return Collections.unmodifiableList(state().getList());
    }

    public boolean isEmpty() {
        return state().getList().isEmpty();
    }

    public boolean isExecuting() {
        return state().getExecutionStatus().isExecuting();
    }

    public synchronized boolean isDone() {
        return state().getExecutionStatus().isSuccess() || state().getCurrentFinishedExecution().size() == state().getList().size();
    }

    public boolean isFailure() {
        return state().getExecutionStatus().isFailureOnLastExecutionAttempt();
    }

    public boolean isSuccess() {
        return state().getExecutionStatus().isSuccess();
    }

    public boolean isSubscribed() {
        return !state().getExecutionStatus().isNeverExecuted();
    }

    public FutureStatus waitFor(long msecs) {
        return state().futureResult().getResult(msecs).first;
    }

    public ActGroupPolicy config() {
        return context.config();
    }

//    public long earliestReadyInMs() {
//        // TODO: Use configurable policy
//        return ExecutionPolicyChecker.computeEarliestReadyInMs(state().getExecutionStatus(), Attempt.once(), Interval.nowThenOfMinutes(1), Interval.nowThenThreeSeconds(), Timeout.ofSixtySeconds());
//    }

    public ExecutionStatus getExecutionStatus() {
        return state().getExecutionStatus();
    }

    // -----------------------------------------------------------
    // reactor functions - add act to current group execution status (similar to TCP acknowledgments, "Execution control protocol")
    // -----------------------------------------------------------

    public synchronized void start() {
        state().getCurrentFinishedExecution().clear();
        state().getExecutionStatus().start();
        state().futureResult().resetAndSubscribe();
    }

    public synchronized boolean next(Act<?, ?> act) {
        //log.debug("Time spent to complete {}: {}", act.getCacheHandle(), act.getExecutionStatus().getTime().getTimeSinceLastExecutionTimeMs());

        state().getCurrentFinishedExecution().add(act);

        if (state().getCurrentFinishedExecution().size() == state().getList().size()) {
            //log.debug("Time elapsed to group finish: {}", state().getExecutionStatus().getTime().getTimeSinceLastExecutionTimeMs());
            state().getExecutionStatus().success();
            state().futureResult().onComplete();
        }
        return state().getCurrentFinishedExecution().size() == state().getList().size();
    }

    public synchronized void error(Act<?, ?> act, Throwable throwable) {
        //log.debug("Time spent to error {}: {}", act.getCacheHandle().getDataCacheId().getId(), act.getExecutionStatus().getTime().getTimeSinceLastExecutionTimeMs());

        state().getCurrentFinishedExecution().add(act);
        state().getExecutionStatus().failure();
        state().futureResult().onError(new ExecutionEndedOnErrorException("Execution of ActGroup ended in error: " + this, throwable));
    }

    // -----------------------------------------------------------
    // Add actions
    // -----------------------------------------------------------

    @SafeVarargs
    public final synchronized <K, V> Act<K, V> act(CacheHandle cacheHandle, CircuitId circuitBreakerId, RateLimiterId rateLimiterId, RxThreadPoolConfig commandThreadPoolPolicy, ActPolicy actPolicy, CommandPolicy commandPolicy, Action0<Map<K, V>>... actions) {
        if (state().getExecutionStatus().isExecuting()) {
            return null;
        }

        final Act<K, V> access = RxCacheFactory.createAct(cacheHandle, circuitBreakerId, rateLimiterId, commandThreadPoolPolicy, actPolicy, commandPolicy, actions);

        state().chain(access);
        return access;
    }

    // -----------------------------------------------------------
    // private
    // -----------------------------------------------------------

    private ActGroupState state() {
        return context.state();
    }

    // -----------------------------------------------------------
    // Overridden from Object
    // -----------------------------------------------------------

    @Override
    public String toString() {
        return "ActGroup{" +
                "chain=" + context.state().getList() +
                "policy=" + context.config().getComputation() +
                '}';
    }
}
