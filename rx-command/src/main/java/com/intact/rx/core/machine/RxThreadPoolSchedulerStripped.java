package com.intact.rx.core.machine;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.Subscription;
import com.intact.rx.core.machine.api.Schedulable;
import com.intact.rx.core.machine.api.Scheduler;

/**
 * This is an externally controlled scheduler
 */
public class RxThreadPoolSchedulerStripped implements Scheduler {
    private static final Logger log = LoggerFactory.getLogger(RxThreadPoolSchedulerStripped.class);

    private final WeakReference<Schedulable<Long>> schedulable;
    private final RxThreadPool pool;

    private final AtomicBoolean trigger = new AtomicBoolean(false);

    public RxThreadPoolSchedulerStripped(Schedulable<Long> schedulable, RxThreadPool pool) {
        this.schedulable = new WeakReference<>(schedulable);
        this.pool = pool;
    }

    @Override
    public void run() {
        Schedulable<Long> schedulablRef = schedulable.get();
        if (schedulablRef != null) {
            schedule(schedulablRef);
        } else {
            onComplete();
        }
    }

    @Override
    public void onNext(Long msecs) {
        pool.schedule(this, msecs);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
    }

    @Override
    public void onComplete() {
        pool.cleanupScheduleQueueForRunnable(this);
    }

    @Override
    public void onError(Throwable throwable) {
        pool.errorRunnableCleanupQueueAndStopRunning(this, throwable);
    }

    // Trigger scheduler to run now if not already running
    public boolean scheduleNowIfNotTriggered() {
        boolean updatedValueToTrigger = trigger.compareAndSet(false, true);
        if (updatedValueToTrigger) {
            pool.schedule(this, 0);
        }
        return updatedValueToTrigger;
    }

    // -----------------------------------------------------------
    // private implementation
    // -----------------------------------------------------------

    private void schedule(Schedulable<Long> schedulable) {
        trigger.set(false);

        try {
            schedulable.run();

            if (schedulable.hasNext()) {
                long waitTimeMs = schedulable.next();
                if (waitTimeMs != Long.MAX_VALUE) {
                    onNext(waitTimeMs);
                }
            }
        } catch (Throwable throwable) {
            onError(throwable);
        }
    }
}
