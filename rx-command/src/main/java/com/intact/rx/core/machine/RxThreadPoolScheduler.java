package com.intact.rx.core.machine;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.Subscription;
import com.intact.rx.core.machine.api.Schedulable;
import com.intact.rx.core.machine.api.Scheduler;
import com.intact.rx.templates.Validate;

/**
 * This is an externally controlled scheduler
 */
public class RxThreadPoolScheduler implements Scheduler {
    private static final Logger log = LoggerFactory.getLogger(RxThreadPoolScheduler.class);

    private final WeakReference<Schedulable<Long>> schedulable;
    private final RxThreadPool pool;

    // state
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean trigger = new AtomicBoolean(false);
    private final AtomicLong scheduleQueueSize = new AtomicLong(0);
    private final AtomicBoolean schedulerActive = new AtomicBoolean(false);

    public RxThreadPoolScheduler(Schedulable<Long> schedulable, RxThreadPool pool) {
        this.schedulable = new WeakReference<>(schedulable);
        this.pool = pool;
    }

    @Override
    public void run() {
        Schedulable<Long> schedulableRef = schedulable.get();
        if (schedulableRef != null) {
            schedule(schedulableRef);
        } else {
            log.debug("Schedulable went out of scope!");
            onComplete();
        }
    }

    @Override
    public void onNext(Long msecs) {
        Validate.assertTrue(schedulerActive.get());

        if (scheduleQueueSize.get() > 3) {
            log.warn("Ignoring scheduling Queue size {}", scheduleQueueSize);
            return;
        }

        scheduleQueueSize.incrementAndGet();
        pool.schedule(this, msecs);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        Validate.assertTrue(!schedulerActive.get());
        Validate.assertTrue(!running.get());
        Validate.assertTrue(scheduleQueueSize.get() == 0);

        schedulerActive.set(true);
        scheduleQueueSize.set(0);
    }

    @Override
    public void onComplete() {
        Validate.assertTrue(schedulerActive.get());

        boolean successfullySet = schedulerActive.compareAndSet(true, false);
        if (successfullySet) {
            pool.cleanupScheduleQueueForRunnable(this);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Validate.assertTrue(schedulerActive.get());

        boolean successfullySet = schedulerActive.compareAndSet(true, false);
        if (successfullySet) {
            pool.errorRunnableCleanupQueueAndStopRunning(this, throwable);
        }
    }

    // Trigger scheduler to run now if not already running
    public boolean scheduleNowIfNotTriggered() {
        if (!schedulerActive.get()) {
            log.warn("Ignoring scheduling, inactive scheduler");
            return false;
        }

        if (scheduleQueueSize.get() > 3) {
            log.warn("Ignoring scheduling Queue size {}", scheduleQueueSize);
            return false;
        }

        boolean updatedValueToTrigger = trigger.compareAndSet(false, true);
        if (updatedValueToTrigger) {
            scheduleQueueSize.incrementAndGet();
            pool.schedule(this, 0);
        }
        return updatedValueToTrigger;
    }

    // -----------------------------------------------------------
    // private implementation
    // -----------------------------------------------------------

    private void schedule(Schedulable<Long> schedulable) {
        trigger.set(false);

        if (!schedulerActive.get()) {
            log.debug("Ignoring run! Inactive scheduler");
            return;
        }

        scheduleQueueSize.getAndUpdate((long operand) -> Math.max(operand - 1, 0));

        try {
            if (!running.compareAndSet(false, true)) {
                return;
            }

            schedulable.run();

            if (schedulable.hasNext()) {
                long waitTimeMs = schedulable.next();
                if (waitTimeMs != Long.MAX_VALUE) {
                    onNext(waitTimeMs);
                }
            }
        } catch (Throwable throwable) {
            onError(throwable);
        } finally {
            running.set(false);
        }
    }
}
