package com.intact.rx.core.machine.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.core.machine.RxThreadPoolId;

public class RxThreadPoolState {
    private static final Logger log = LoggerFactory.getLogger(RxThreadPoolState.class);
    public static final long sixMonthsInTheFuture = 6 * 30;

    private final RxThreadPoolId id;
    private final Map<ScheduledFuture<?>, Runnable> scheduledFutureRunnableMap;

    public RxThreadPoolState(RxThreadPoolId id) {
        this.id = requireNonNull(id);
        this.scheduledFutureRunnableMap = new ConcurrentHashMap<>();
    }

    public RxThreadPoolId getId() {
        return id;
    }

    public Map<ScheduledFuture<?>, Runnable> getFutures() {
        return Collections.unmodifiableMap(scheduledFutureRunnableMap);
    }

    public Runnable addFuture(ScheduledFuture<?> scheduledFuture, Runnable runnable) {
        return scheduledFutureRunnableMap.put(scheduledFuture, runnable);
    }

    public Runnable removeFuture(ScheduledFuture<?> scheduledFuture) {
        return scheduledFutureRunnableMap.remove(scheduledFuture);
    }

    public void cancelFuturesForRunnable(Runnable runnable, boolean mayInterrupt) {
        // -- debug --
        long start = System.currentTimeMillis();
        // -- debug --

        if (!scheduledFutureRunnableMap.containsValue(runnable)) {
            return;
        }

        List<ScheduledFuture<?>> keys = new ArrayList<>();

        scheduledFutureRunnableMap.entrySet().stream()
                // Filter in: runnable's futures. Also clean up: futures from the past, futures too far into the future.
                .filter(entry -> Objects.equals(entry.getValue(), runnable) || entry.getKey().getDelay(TimeUnit.MILLISECONDS) < 0 || entry.getKey().getDelay(TimeUnit.DAYS) > sixMonthsInTheFuture)
                .forEach(entry -> {
                    ScheduledFuture<?> scheduledFuture = entry.getKey();
                    // Only interrupt runnable's futures
                    if (Objects.equals(entry.getValue(), runnable)) {
                        if (!scheduledFuture.isDone() && !scheduledFuture.isCancelled()) {
                            scheduledFuture.cancel(mayInterrupt);
                        }
                    }

                    keys.add(scheduledFuture);
                });

        keys.forEach(scheduledFutureRunnableMap::remove);

        // -- debug --
        long end = System.currentTimeMillis() - start;
        if (end > 30) {
            log.info("{}: # of futures {}: Removal of {} futures took {} ms", this.getId(), scheduledFutureRunnableMap.size(), keys.size(), end);
        }
        // -- debug --
    }
}
