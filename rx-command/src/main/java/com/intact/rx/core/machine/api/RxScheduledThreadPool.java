package com.intact.rx.core.machine.api;

import java.util.concurrent.ScheduledFuture;

public interface RxScheduledThreadPool<T> {

    ScheduledFuture<?> schedule(T value, long msecs);

    Runnable removeScheduledFuture(ScheduledFuture<?> schedule);

    ScheduledFuture<?> cleanupScheduleQueueAndSchedule(T value, int msecs);

    void cleanupScheduleQueueForRunnable(T value);

    void errorRunnableCleanupQueueAndStopRunning(T value, Throwable throwable);

    void onInterrupt(ScheduledFuture<?> future, T runnable);
}
