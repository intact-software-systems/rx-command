package com.intact.rx.core.rxcache.controller;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.machine.RxThreadPoolSchedulerStripped;
import com.intact.rx.core.machine.api.Schedulable;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.core.rxcache.acts.ActGroupChain;
import com.intact.rx.core.rxcache.acts.ActGroupIterator;
import com.intact.rx.core.rxcache.acts.ActGroupPolicy;
import com.intact.rx.core.rxcache.api.Act;
import com.intact.rx.core.rxcache.api.ActsController;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerCache;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterCache;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.exception.CancelCommandException;
import com.intact.rx.exception.CircuitBreakerOpenException;
import com.intact.rx.exception.ExecutionEndedOnErrorException;
import com.intact.rx.exception.RateLimitViolatedException;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.Tuple2;
import com.intact.rx.templates.Validate;
import com.intact.rx.templates.api.Context;

import static com.intact.rx.core.command.strategy.ExecutionPolicyChecker.*;

@SuppressWarnings("SynchronizedMethod")
public class ActsCommandsController implements
        ActsController,
        Schedulable<Long>,
        RxKeyValueObserver<Act<?, ?>, Map<?, ?>>,
        Subscription {
    private static final long ACQUIRE_LOCK_TIMEOUT_IN_MS = 5000L;
    private static final Logger log = LoggerFactory.getLogger(ActsCommandsController.class);

    private final Context<ActsControllerPolicy, ActsControllerState> context;

    public ActsCommandsController(ActsControllerPolicy policy, RxThreadPool rxThreadPool, ActGroupChain chain, CircuitId circuitBreakerId, RateLimiterId rateLimiterId) {
        //noinspection ThisEscapedInObjectConstruction
        context = new ContextObject<>(policy, new ActsControllerState(new RxThreadPoolSchedulerStripped(this, rxThreadPool), chain, circuitBreakerId, rateLimiterId));
    }

    // -----------------------------------------------------------
    // Interface Schedulable
    // -----------------------------------------------------------

    @Override
    public void run() {
        if (!lockSchedulingMutex(ACQUIRE_LOCK_TIMEOUT_IN_MS)) {
            log.warn("Could not acquire scheduling lock within " + ACQUIRE_LOCK_TIMEOUT_IN_MS + " ms");
            return;
        }

        try {
            // Note: currently the controller is "fail-fast" on any error
            if (state().getExecutionStatus().isExecuting() && state().iterator().current() != null && state().iterator().current().isFailure()) {
                executionFailure();
                return;
            } else if (isTimeout(state().getExecutionStatus(), config().getTimeout())) {
                executionFailure();
                log.debug("Timing out ActsController");
                return;
            } else if (state().getExecutionStatus().isCancelled()) {
                return;
            } else if (!state().iterator().hasNext() && !state().getExecutionStatus().isExecuting()) {
                log.debug("No group to execute. Finished. {}", state().getExecutionStatus());
                return;
            }

            boolean isStarting = state().getExecutionStatus().isStarting();
            boolean isContinueToExecuteGroup = state().iterator().current() != null && !state().iterator().current().isDone();
            boolean readyToExecute = Optional.ofNullable(state().iterator().current()).map(g -> isReadyToExecute(g.getExecutionStatus(), config().getGroupInterval(), config().getRetryGroupInterval())).orElse(false);
            boolean isReadyToExecuteNextGroup = state().iterator().current() != null && state().iterator().current().isDone() && readyToExecute;

            if (isStarting || isContinueToExecuteGroup || isReadyToExecuteNextGroup) {

                if (state().iterator().currentGroupIterator() == null) {
                    executionStart();
                }

                Validate.assertTrue(!state().getExecutionStatus().isStarting());

                computeGroupIteratorIfAbsent()
                        .map(groupIterator -> executeGroup(groupIterator, groupIterator.getConfig()))
                        .orElseGet(() -> {
                            if (!state().iterator().hasNext() && state().iterator().current() != null && state().iterator().current().isDone()) {
                                executionFinished();
                            }
                            return false;
                        });
            }
        } finally {
            state().schedulingMutex().unlock();
        }
    }

    @Override
    public Long next() {
        // Assumption: this.run() is scheduled by callbacks when onComplete and onError is called.
        // if running, then next run is timeout, running = group is running
        ActGroup group = state().iterator().current();
        return group != null && group.isExecuting() && !config().getTimeout().isForever()
                ? computeTimeUntilTimeoutMs(group.getExecutionStatus(), config().getTimeout())
                : Long.MAX_VALUE;
    }

    @Override
    public boolean hasNext() {
        return state().getExecutionStatus().isExecuting();
    }

    // -----------------------------------------------------------
    // Interface ActsController
    // -----------------------------------------------------------

    @Override
    public <K, V> Tuple2<FutureStatus, Optional<V>> computeIfAbsent(CacheHandle cacheHandle, K key, long msecs) {
        //noinspection SynchronizeOnThis
        synchronized (this) {
            if (!lockSchedulingMutex(ACQUIRE_LOCK_TIMEOUT_IN_MS)) {
                //noinspection AccessToStaticFieldLockedOnInstance
                log.warn("Could not acquire subscribe lock within " + ACQUIRE_LOCK_TIMEOUT_IN_MS + " ms");
                return new Tuple2<>(FutureStatus.NotStarted, Optional.empty());
            }

            try {
                Optional<V> value = RxCacheAccess.<K, V>find(cacheHandle).flatMap(cache -> cache.read(key));
                if (value.isPresent()) {
                    return new Tuple2<>(FutureStatus.NotStarted, value);
                }
                subscribe();
            } finally {
                state().schedulingMutex().unlock();
            }
        }

        FutureStatus futureStatus = waitFor(msecs);
        return new Tuple2<>(futureStatus, RxCacheAccess.<K, V>find(cacheHandle).flatMap(cache -> cache.read(key)));
    }

    @Override
    public <K, V> Tuple2<FutureStatus, Map<K, V>> computeIfAbsent(CacheHandle cacheHandle, Iterable<K> keys, long msecs) {
        //noinspection SynchronizeOnThis
        synchronized (this) {
            if (!lockSchedulingMutex(ACQUIRE_LOCK_TIMEOUT_IN_MS)) {
                //noinspection AccessToStaticFieldLockedOnInstance
                log.warn("Could not acquire subscribe lock within " + ACQUIRE_LOCK_TIMEOUT_IN_MS + " ms");
                return new Tuple2<>(FutureStatus.NotStarted, Collections.emptyMap());
            }

            try {
                Map<K, V> values = RxCacheAccess.<K, V>find(cacheHandle).map(cache -> cache.read(keys)).orElse(Collections.emptyMap());
                if (values.size() == keys.spliterator().getExactSizeIfKnown()) {
                    return new Tuple2<>(FutureStatus.NotStarted, values);
                }
                subscribe();
            } finally {
                state().schedulingMutex().unlock();
            }
        }

        FutureStatus futureStatus = waitFor(msecs);
        return new Tuple2<>(futureStatus, RxCacheAccess.<K, V>find(cacheHandle).map(cache -> cache.read(keys)).orElse(Collections.emptyMap()));
    }

    @Override
    public <K, V> Tuple2<FutureStatus, Optional<V>> computeIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        //noinspection SynchronizeOnThis
        synchronized (this) {
            if (!lockSchedulingMutex(ACQUIRE_LOCK_TIMEOUT_IN_MS)) {
                //noinspection AccessToStaticFieldLockedOnInstance
                log.warn("Could not acquire subscribe lock within " + ACQUIRE_LOCK_TIMEOUT_IN_MS + " ms");
                return new Tuple2<>(FutureStatus.NotStarted, Optional.empty());
            }

            try {
                Optional<V> value = RxCacheAccess.<K, V>find(cacheHandle).flatMap(cache -> cache.read(key));
                if (!computeResolver.perform(value.orElse(null))) {
                    return new Tuple2<>(FutureStatus.NotStarted, value);
                }
                subscribe();
            } finally {
                state().schedulingMutex().unlock();
            }
        }

        FutureStatus futureStatus = waitFor(msecs);
        return new Tuple2<>(futureStatus, RxCacheAccess.<K, V>find(cacheHandle).flatMap(cache -> cache.read(key)));
    }

    @Override
    public <K, V> Tuple2<FutureStatus, Map<K, V>> computeIf(CacheHandle cacheHandle, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        //noinspection SynchronizeOnThis
        synchronized (this) {
            if (!lockSchedulingMutex(ACQUIRE_LOCK_TIMEOUT_IN_MS)) {
                //noinspection AccessToStaticFieldLockedOnInstance
                log.warn("Could not acquire subscribe lock within " + ACQUIRE_LOCK_TIMEOUT_IN_MS + " ms");
                return new Tuple2<>(FutureStatus.NotStarted, Collections.emptyMap());
            }

            try {
                Map<K, V> values = RxCacheAccess.<K, V>find(cacheHandle).map(cache -> cache.read(keys)).orElse(Collections.emptyMap());
                if (!computeResolver.perform(values)) {
                    return new Tuple2<>(FutureStatus.NotStarted, values);
                }
                subscribe();
            } finally {
                state().schedulingMutex().unlock();
            }
        }

        FutureStatus futureStatus = waitFor(msecs);
        return new Tuple2<>(futureStatus, RxCacheAccess.<K, V>find(cacheHandle).map(cache -> cache.read(keys)).orElse(Collections.emptyMap()));
    }

    @Override
    public synchronized boolean subscribe() {
        // Note: execution status should reflect "all executions" not single like today
        if (state().getExecutionStatus().isExecuting()) {
            return false;
        }

        if (!lockSchedulingMutex(ACQUIRE_LOCK_TIMEOUT_IN_MS)) {
            //noinspection AccessToStaticFieldLockedOnInstance
            log.warn("Could not acquire subscribe lock within " + ACQUIRE_LOCK_TIMEOUT_IN_MS + " ms");
            return false;
        }

        try {
            if (state().getExecutionStatus().isExecuting()) {
                return false;
            }

            if (!CircuitBreakerCache.circuit(state().getCircuitBreakerId(), config().getCircuitBreakerPolicy()).allowRequest()) {
                executionNotStartedCircuitOpen();
                return false;
            }

            if (!RateLimiterCache.rateLimiter(state().getRateLimiterId(), config().getRateLimiterPolicy()).allowRequest()) {
                executionNotStartedRateLimiterViolation();
                return false;
            }

            state().getExecutionStatus().reset();
            state().getExecutionStatus().starting();
            state().iterator().rewind();
            state().getResult().resetAndSubscribe();

            state().scheduler().onSubscribe(this);
            state().scheduler().onNext(0L);

        } finally {
            state().schedulingMutex().unlock();
        }

        return true;
    }

    @Override
    public synchronized boolean unsubscribe() {
        state().scheduler().onComplete();
        Optional.ofNullable(state().iterator().current())
                .ifPresent(this::unsubscribeAndDisconnect);
        return true;
    }

    @Override
    public boolean isSubscribed() {
        // Note: Check different status. It should be a SubscriptionStatus, not ExecutionStatus
        return state().getExecutionStatus().isExecuting();
    }

    @Override
    public void request(long n) {
        subscribe();
    }

    @Override
    public synchronized void cancel() {
        executionCancelled();
    }

    @Override
    public boolean isSuccess() {
        return state().getExecutionStatus().isSuccess();
    }

    @Override
    public boolean isCancelled() {
        return state().getExecutionStatus().isCancelled();
    }

    @Override
    public FutureStatus waitFor(long msecs) {
        return state().getResult().getResult(msecs).first;
    }

    @Override
    public FutureStatus waitForGroupN(int n, long msecs) {
        Validate.assertTrue(n > 0);
        ActGroup actGroup = state().chain().get(n - 1);
        return actGroup != null ? actGroup.waitFor(msecs) : FutureStatus.NotStarted;
    }

    @Override
    public List<Throwable> getExceptions() {
        List<Throwable> exceptions = new ArrayList<>();
        state().chain().forEach(
                group -> group.getList().forEach(
                        act -> exceptions.addAll(act.getCommands().getExceptions())
                )
        );
        return exceptions;
    }

    public boolean connect(RxObserver<ActGroup> observer) {
        return state().getRxSubject().connect(observer);
    }

    // -----------------------------------------------------------
    // Interface CommandControllerObserver
    // -----------------------------------------------------------

    @Override
    public void onComplete(Act<?, ?> act) {
        Validate.assertTrue(state().iterator().current() != null);

        boolean isFinished = state().iterator().current().next(act);

        switch (state().iterator().current().config().getComputation()) {
            case SEQUENTIAL:
                state().scheduler().scheduleNowIfNotTriggered();
                break;
            case PARALLEL:
                if (isFinished) {
                    state().scheduler().scheduleNowIfNotTriggered();
                }
                break;
        }
    }

    @Override
    public void onError(Act<?, ?> act, Throwable throwable) {
        Validate.assertTrue(state().iterator().current() != null);

        state().iterator().current().error(act, throwable);
        state().scheduler().scheduleNowIfNotTriggered();
    }

    @Override
    public void onNext(Act<?, ?> act, Map<?, ?> values) {
    }

    @Override
    public void onSubscribe(Act<?, ?> act, Subscription subscription) {
    }

    // -----------------------------------------------------------
    // private functions
    // -----------------------------------------------------------

    private boolean executeGroup(ActGroupIterator groupIterator, ActGroupPolicy policy) {
        Validate.assertTrue(!state().getExecutionStatus().isStarting());
        Validate.assertTrue(groupIterator != null);
        Validate.assertTrue(policy != null);

        switch (policy.getComputation()) {
            case PARALLEL:
                if (groupIterator.isAtStart()) {
                    return executeAll(groupIterator);
                }
                break;
            case SEQUENTIAL: {
                boolean isDone = state().iterator().currentGroupIterator().current().map(Act::isDone).orElse(true);

                if (isDone) {
                    return executeNext(groupIterator);
                } else {
                    log.info("Sequential execution, act is not complete, waiting for completion");
                }
            }
        }
        return false;
    }

    private boolean executeAll(ActGroupIterator group) {
        if (state().getExecutionStatus().isCancelled() || group == null || !group.hasNext()) {
            log.warn("Ignoring execute all for group {}", group);
            return false;
        }

        boolean isAnyExecuted = false;
        while (group.hasNext() && !state().getExecutionStatus().isCancelled()) {
            boolean executed = executeNext(group);
            if (!executed) {
                return false;
            }
            isAnyExecuted = true;
        }
        return isAnyExecuted;
    }

    private boolean executeNext(ActGroupIterator group) {
        if (state().getExecutionStatus().isCancelled() || group == null || !group.hasNext()) {
            log.warn("Ignoring execute next for group {}", group);
            return false;
        }

        Act<?, ?> act = group.next();
        return act != null && act.subscribe().isValid();
    }

    private Optional<ActGroupIterator> computeGroupIteratorIfAbsent() {
        if (state().iterator().current() != null && !state().iterator().current().isDone()) {
            return Optional.of(state().iterator().currentGroupIterator());
        }

        // Disconnect current group, if any
        Optional.ofNullable(state().iterator().current())
                .ifPresent(group -> {
                    if (group.isDone()) {
                        group.getList().forEach(act -> act.disconnect(this));
                        state().getRxSubject().onNext(group);
                    }
                });

        // Find next group to execute, if any
        ActGroup nextGroup = null;
        if (state().iterator().hasNext()) {
            do {
                nextGroup = state().iterator().next();
            } while (nextGroup != null && nextGroup.isEmpty() && state().iterator().hasNext());
        }

        if (nextGroup == null || nextGroup.isEmpty()) {
            return Optional.empty();
        }

        nextGroup.start();
        nextGroup.getList().forEach(act -> act.connect(this));

        return Optional.of(state().iterator().currentGroupIterator());
    }

    // -----------------------------------------------------------
    // Execution lifecycle methods:
    //  - start, finished, failure, cancelled, not started
    // -----------------------------------------------------------

    private void executionStart() {
        Validate.assertTrue(state().getExecutionStatus().isStarting());
        Validate.assertTrue(state().iterator().hasNext());
        Validate.assertTrue(state().iterator().isAtStart());

        state().getExecutionStatus().start();
        state().getRxSubject().onSubscribe(this);
        state().getResult().onSubscribe(this);
    }

    private void executionFinished() {
        Validate.assertTrue(!state().iterator().hasNext());
        Validate.assertTrue(!state().getExecutionStatus().isStarting());
        Validate.assertTrue(state().getExecutionStatus().isExecuting());

        Optional.ofNullable(state().iterator().current())
                .ifPresent(group -> Validate.assertTrue(group.isDone()));

        Optional.ofNullable(state().iterator().current())
                .ifPresent(group -> group.getList().forEach(act -> act.disconnect(this)));

        state().getExecutionStatus().success();
        CircuitBreakerCache.circuit(state().getCircuitBreakerId(), config().getCircuitBreakerPolicy()).success();

        {
            state().scheduler().onComplete();
            state().getRxSubject().onComplete();
            state().getResult().onComplete();
        }
    }

    private void executionFailure() {
        Optional.ofNullable(state().iterator().current())
                .ifPresent(this::unsubscribeAndDisconnect);

        state().getExecutionStatus().failure();
        CircuitBreakerCache.circuit(state().getCircuitBreakerId(), config().getCircuitBreakerPolicy()).failure();

        {
            ExecutionEndedOnErrorException throwable = new ExecutionEndedOnErrorException(createExceptionMsg("Execution of ActsController chain ended in error"));
            state().scheduler().onError(throwable);
            state().getRxSubject().onError(throwable);
            state().getResult().onError(throwable);
        }
    }

    private void executionCancelled() {
        Optional.ofNullable(state().iterator().current())
                .ifPresent(this::unsubscribeAndDisconnect);

        state().getExecutionStatus().cancel();

        {
            CancelCommandException throwable = new CancelCommandException(createExceptionMsg("Execution of ActsController was cancelled"));
            state().scheduler().onError(throwable);
            state().getRxSubject().onError(throwable);
            state().getResult().onError(throwable);
        }
    }

    private void executionNotStartedCircuitOpen() {
        state().getRxSubject().onSubscribe(this);
        state().getResult().onSubscribe(this);
        state().scheduler().onSubscribe(this);

        state().getExecutionStatus().failure();

        {
            CircuitBreakerOpenException throwable = new CircuitBreakerOpenException("Circuit breaker " + state().getCircuitBreakerId() + " is OPEN for ActsController@" + hashCode());
            state().scheduler().onError(throwable);
            state().getRxSubject().onError(throwable);
            state().getResult().onError(throwable);
        }
    }

    private void executionNotStartedRateLimiterViolation() {
        state().getRxSubject().onSubscribe(this);
        state().getResult().onSubscribe(this);
        state().scheduler().onSubscribe(this);

        state().getExecutionStatus().failure();

        {
            RateLimitViolatedException throwable = new RateLimitViolatedException("Rate limiter violation " + state().getRateLimiterId() + " is violated for ActsController@" + hashCode());
            state().scheduler().onError(throwable);
            state().getRxSubject().onError(throwable);
            state().getResult().onError(throwable);
        }
    }

    // -----------------------------------------------------------
    // Subscription handling, i.e., observers, etc.
    // -----------------------------------------------------------

    private void unsubscribeAndDisconnect(ActGroup group) {
        Validate.assertTrue(group != null);

        group.getList().forEach(act -> {
            act.unsubscribe();
            act.disconnect(this);
        });
    }

    private ActsControllerPolicy config() {
        return context.config();
    }

    private ActsControllerState state() {
        return context.state();
    }

    private boolean lockSchedulingMutex(long lockTimeoutInMs) {
        try {
            if (!state().schedulingMutex().tryLock(lockTimeoutInMs, TimeUnit.MILLISECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for scheduling lock", e);
            return false;
        }
        return true;
    }

    private String createExceptionMsg(String msg) {
        StringBuilder exceptionMessage = new StringBuilder();
        state().chain().forEach(
                group -> group.getList().forEach(
                        act ->
                                act.getCommands().forEach(command -> {
                                    if (!command.getExceptions().isEmpty()) {
                                        exceptionMessage.append(command);
                                    }
                                })
                )
        );

        return msg + ":" + exceptionMessage;
    }
}
