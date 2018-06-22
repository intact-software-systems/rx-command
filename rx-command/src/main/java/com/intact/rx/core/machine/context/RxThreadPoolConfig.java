package com.intact.rx.core.machine.context;

import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.machine.RxThreadPoolId;
import com.intact.rx.core.machine.factory.RxThreadFactory;
import com.intact.rx.policy.MaxLimit;

public class RxThreadPoolConfig {
    private final ThreadFactory threadFactory;
    private final MaxLimit threadPoolSize;
    private final RxThreadPoolId threadPoolId;

    private RxThreadPoolConfig(MaxLimit threadPoolSize, ThreadFactory threadFactory, RxThreadPoolId threadPoolId) {
        this.threadPoolSize = requireNonNull(threadPoolSize);
        this.threadFactory = requireNonNull(threadFactory);
        this.threadPoolId = requireNonNull(threadPoolId);
    }

    public static RxThreadPoolConfig create(MaxLimit threadPoolSize, ThreadFactory threadFactory, RxThreadPoolId threadPoolId) {
        return new RxThreadPoolConfig(threadPoolSize, threadFactory, threadPoolId);
    }

    public static RxThreadPoolConfig create(MaxLimit threadPoolSize, RxThreadPoolId threadPoolId) {
        return new RxThreadPoolConfig(threadPoolSize, RxThreadFactory.daemonWithName(threadPoolId.getId()), threadPoolId);
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public MaxLimit threadPoolSize() {
        return threadPoolSize;
    }

    public RxThreadPoolId getThreadPoolId() {
        return threadPoolId;
    }

    @Override
    public String toString() {
        return "RxThreadPoolPolicy{" +
                "threadFactory=" + threadFactory +
                ", threadPoolSize=" + threadPoolSize +
                ", threadPoolId=" + threadPoolId +
                '}';
    }
}
