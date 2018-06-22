package com.intact.rx.core.machine.factory;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

public class RxThreadFactory implements ThreadFactory {
    private final String threadPrefix;
    private final boolean daemonFlag;

    private RxThreadFactory(String threadPrefix, boolean daemonFlag) {
        this.threadPrefix = requireNonNull(threadPrefix);
        this.daemonFlag = daemonFlag;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName(threadPrefix + thread.getId());
        thread.setDaemon(daemonFlag);
        return thread;
    }

    public static RxThreadFactory daemonWithName(String prefix) {
        return new RxThreadFactory(prefix, true);
    }
}
