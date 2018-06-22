package com.intact.rx.core.rxcache.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.core.command.strategy.ExecutionPolicyChecker;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.machine.RxThreadPoolScheduler;
import com.intact.rx.core.machine.api.Schedulable;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.core.rxcache.api.ActsController;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.Tuple2;
import com.intact.rx.templates.api.Context;

public class ActsScheduler implements ActsController, Schedulable<Long>, RxObserver<ActGroup> {
    private static final long ACQUIRE_LOCK_TIMEOUT_IN_MS = 5000L;
    private static final Logger log = LoggerFactory.getLogger(ActsScheduler.class);

    private final Context<ActsSchedulerPolicy, ActsSchedulerState> context;

    public ActsScheduler(ActsSchedulerPolicy actsSchedulerPolicy, ActsCommandsController actsCommandsController, RxThreadPool rxThreadPool) {
        this.context = new ContextObject<>(actsSchedulerPolicy, new ActsSchedulerState(new RxThreadPoolScheduler(this, rxThreadPool), actsCommandsController));
    }

    // -----------------------------------------------------------
    // Interface Schedulable
    // -----------------------------------------------------------

    @Override
    public void run() {

        if (!lockSchedulingMutex(ACQUIRE_LOCK_TIMEOUT_IN_MS)) {
            log.warn("Could not acquire scheduling lock within " + ACQUIRE_LOCK_TIMEOUT_IN_MS + " ms");
            state().getExecutionStatus().falseStart();
            return;
        }

        try {
            if (isReady()) {
                log.info("Subscribing");
                subscribe();
            }
        } finally {
            state().schedulingMutex().unlock();
        }
    }

    private boolean isReady() {
        return !state().getActsCommandsController().isSubscribed() ||
                !state().getExecutionStatus().isExecuting() &&
                        (state().getTriggered().get() ||
                                ExecutionPolicyChecker.isReadyToExecute(state().getExecutionStatus(), config().getAttempt(), config().getInterval(), config().getRetryInterval()));

    }

    @Override
    public boolean hasNext() {
        return ExecutionPolicyChecker.isInAttempt(state().getExecutionStatus(), config().getAttempt());
    }

    @Override
    public Long next() {
        return ExecutionPolicyChecker.computeEarliestReadyInMs(
                state().getExecutionStatus(),
                config().getAttempt(),
                config().getInterval(),
                config().getRetryInterval(),
                config().getTimeout()
        );
    }

    // -----------------------------------------------------------
    // Interface RxObserver<ActGroup>
    // -----------------------------------------------------------

    @Override
    public void onSubscribe(Subscription subscription) {
        // TODO: Set current subscription and use it to cancel subscription
        state().getExecutionStatus().start();
    }

    @Override
    public void onComplete() {
        state().getExecutionStatus().success();
    }

    @Override
    public void onError(Throwable throwable) {
        state().getExecutionStatus().failure();
    }

    @Override
    public void onNext(ActGroup value) {
    }

    // -----------------------------------------------------------
    // Interface ActsController
    // -----------------------------------------------------------

    @Override
    public <K, V> Tuple2<FutureStatus, Optional<V>> computeIfAbsent(CacheHandle cacheHandle, K key, long msecs) {
        return state().getActsCommandsController().computeIfAbsent(cacheHandle, key, msecs);
    }

    @Override
    public <K, V> Tuple2<FutureStatus, Map<K, V>> computeIfAbsent(CacheHandle cacheHandle, Iterable<K> keys, long msecs) {
        return state().getActsCommandsController().computeIfAbsent(cacheHandle, keys, msecs);
    }

    @Override
    public <K, V> Tuple2<FutureStatus, Optional<V>> computeIf(CacheHandle cacheHandle, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return state().getActsCommandsController().computeIf(cacheHandle, key, computeResolver, msecs);
    }

    @Override
    public <K, V> Tuple2<FutureStatus, Map<K, V>> computeIf(CacheHandle cacheHandle, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return state().getActsCommandsController().computeIf(cacheHandle, keys, computeResolver, msecs);
    }

    @Override
    public boolean subscribe() {
        return state().getActsCommandsController().subscribe();
    }

    @Override
    public boolean unsubscribe() {
        return state().getActsCommandsController().unsubscribe();
    }

    @Override
    public boolean isSubscribed() {
        return state().getActsCommandsController().isSubscribed();
    }

    @Override
    public void cancel() {
        state().getExecutionStatus().cancel();
        state().getActsCommandsController().cancel();
    }

    @Override
    public boolean isSuccess() {
        return state().getActsCommandsController().isSuccess();
    }

    @Override
    public boolean isCancelled() {
        return state().getActsCommandsController().isCancelled();
    }

    @Override
    public FutureStatus waitFor(long msecs) {
        return state().getActsCommandsController().waitFor(msecs);
    }

    @Override
    public FutureStatus waitForGroupN(int n, long msecs) {
        return state().getActsCommandsController().waitForGroupN(n, msecs);
    }

    @Override
    public List<Throwable> getExceptions() {
        return state().getActsCommandsController().getExceptions();
    }

    // -----------------------------------------------------------
    // private functions
    // -----------------------------------------------------------

    private ActsSchedulerPolicy config() {
        return context.config();
    }

    private ActsSchedulerState state() {
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
}
