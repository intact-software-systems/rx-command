package com.intact.rx.monitor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.machine.context.RxThreadPoolState;
import com.intact.rx.core.machine.factory.RxThreadPoolFactory;

/**
 * Check state of rx thread pools
 */
final class MachineMonitorAlgorithms {

    private MachineMonitorAlgorithms() {
    }

    private static final Logger log = LoggerFactory.getLogger(MachineMonitorAlgorithms.class);

    static void monitorThreadPools() {
        RxThreadPoolFactory.poolcache().readAsList()
                .forEach(
                        threadPool -> monitorThreadPool(threadPool, threadPool.getId().getId())
                );
    }

    private static void monitorThreadPool(RxThreadPool threadPool, final String prefix) {
        if (threadPool.getFutures().size() > 100) {
            cleanupState(threadPool.getId().getId(), threadPool);
        }

        logState(threadPool, prefix);
    }

    private static void cleanupState(String prefix, RxThreadPool threadPool) {
        long scheduledInThePastCount = 0;
        long scheduledTooLongInTheFuture = 0;

        for (ScheduledFuture<?> future : threadPool.getFutures().keySet()) {
            if (future.getDelay(TimeUnit.MILLISECONDS) < 0) {
                scheduledInThePastCount++;
                threadPool.removeScheduledFuture(future);
            } else if (future.getDelay(TimeUnit.DAYS) > RxThreadPoolState.sixMonthsInTheFuture) {
                scheduledTooLongInTheFuture++;
                threadPool.removeScheduledFuture(future);
            }
        }

        if (scheduledInThePastCount > 0) {
            log.info("{}: Removed {} futures scheduled in the past", prefix, scheduledInThePastCount);
        } else if (scheduledTooLongInTheFuture > 0) {
            log.info("{}: Removed {} futures scheduled to run 6 months from now", prefix, scheduledTooLongInTheFuture);
        }
    }

    private static void logState(RxThreadPool threadPool, final String prefix) {
        // Note: If thread pool is used more than a percentage then log warning
        if (threadPool.getActiveCount() > (int) (threadPool.getPoolSize() * 0.8)) {
            log.warn("{}: Size: {}. Active: {}. Queue: {}", prefix, threadPool.getPoolSize(), threadPool.getActiveCount(), threadPool.getQueue().size());
        }

        long sz = threadPool.getFutures().size();
        if (sz > 100) {
            log.info("{}: Potential memory leak? Scheduled future map is filling up: {}", prefix, sz);
        }
    }
}
