package com.intact.rx.core.machine;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.core.machine.api.RxScheduledThreadPool;
import com.intact.rx.core.machine.context.RxThreadPoolConfig;
import com.intact.rx.core.machine.context.RxThreadPoolState;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.Validate;
import com.intact.rx.templates.api.Context;

/**
 * The thread pool holds and triggers the workers that execute the commands attached to command controllers.
 */
public class RxThreadPool
        extends ScheduledThreadPoolExecutor implements
        RxScheduledThreadPool<Runnable>,
        RejectedExecutionHandler {
    private static final Logger log = LoggerFactory.getLogger(RxThreadPool.class);

    private final Context<RxThreadPoolConfig, RxThreadPoolState> context;

    private RxThreadPool(RxThreadPoolConfig threadPoolPolicy, RxThreadPoolId id) {
        super(requireNonNull(threadPoolPolicy).threadPoolSize().getLimit(), threadPoolPolicy.getThreadFactory());

        this.context = new ContextObject<>(threadPoolPolicy, new RxThreadPoolState(id));
    }

    // -----------------------------------------------------------
    // Factory functions
    // -----------------------------------------------------------

    public static RxThreadPool create(RxThreadPoolConfig threadPoolPolicy) {
        RxThreadPool threadPool = new RxThreadPool(threadPoolPolicy, threadPoolPolicy.getThreadPoolId());
        threadPool.setRejectedExecutionHandler(threadPool);
        return threadPool;
    }

    // ----------------------------------
    // Public functions
    // ----------------------------------

    public RxThreadPoolId getId() {
        return state().getId();
    }

    public Map<ScheduledFuture<?>, Runnable> getFutures() {
        return state().getFutures();
    }

    // -----------------------------------------------------------
    // Interface RxScheduleObserver<Runnable>
    // -----------------------------------------------------------

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long msecs) {
        // -------------------------------
        // filtering code -- Make nicer :)
        // -------------------------------
        if (msecs < 0) {
            log.warn("{}: Schedule time {} is in the past for: {}", state().getId(), msecs, runnable);
            return null;
        } else if (msecs == Long.MAX_VALUE || msecs >= 0x7fffffffffffffL) {
            log.warn("{}: Schedule time {} is forever in the future for: {}", state().getId(), msecs, runnable);
            return null;
        }
        // -- filtering code --


        // -- debug --
        if (msecs > 6000000) // 100 minutes
        {
            log.warn("{}: Schedule time {} is {} hours in the future: {}", state().getId(), msecs, Duration.ofMillis(msecs).toHours(), runnable);
        }
        // -- debug --

        return this.scheduleRunnableMsec(runnable, msecs);
    }

    @Override
    public Runnable removeScheduledFuture(ScheduledFuture<?> schedule) {
        return state().removeFuture(schedule);
    }

    @Override
    public ScheduledFuture<?> cleanupScheduleQueueAndSchedule(Runnable runnable, int msecs) {
        this.cleanupScheduleQueuePrivate(runnable, false);
        return this.scheduleRunnableMsec(runnable, msecs);
    }

    @Override
    public void cleanupScheduleQueueForRunnable(Runnable runnable) {
        this.cleanupScheduleQueuePrivate(runnable, false);
    }

    @Override
    public void errorRunnableCleanupQueueAndStopRunning(Runnable runnable, Throwable throwable) {
        this.cleanupScheduleQueuePrivate(runnable, true);
        //log.debug("{}: Runnable {} experienced error: ", state().getId(), runnable, throwable);
    }

    @Override
    public void onInterrupt(ScheduledFuture<?> future, Runnable runnable) {
        future.cancel(true);
        super.purge();

        state().removeFuture(future);
    }

    // -----------------------------------------------------------
    // Interface RejectedExecutionHandler
    // -----------------------------------------------------------

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
        log.error("{} Rejected execution of {} in threadPool {}", state().getId(), runnable, threadPoolExecutor);
    }

    // -----------------------------------------------------------
    // Overridden from ThreadPoolExecutor
    // -----------------------------------------------------------

    @Override
    protected void afterExecute(Runnable runnable, Throwable t) {
        if (runnable instanceof ScheduledFuture<?>) {
            state().removeFuture((ScheduledFuture<?>) runnable);
        }

        super.afterExecute(runnable, t);
    }

    // -----------------------------------------------------------
    // Private functions
    // -----------------------------------------------------------

    private void cleanupScheduleQueuePrivate(Runnable runnable, boolean mayInterrupt) {
        state().cancelFuturesForRunnable(runnable, mayInterrupt);
        super.purge();
    }

    private ScheduledFuture<?> scheduleRunnableMsec(Runnable runnable, long msecs) {
        requireNonNull(runnable);
        Validate.assertTrue(msecs >= 0);

        ScheduledFuture<?> schedule = super.schedule(runnable, msecs, TimeUnit.MILLISECONDS);
        state().addFuture(schedule, runnable);
        return schedule;
    }

    private RxThreadPoolState state() {
        return context.state();
    }
}
