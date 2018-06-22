package com.intact.rx.core.command;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.Subscription;
import com.intact.rx.core.machine.api.RxScheduledThreadPool;

/**
 * CommandSubscription calls thread pool onInterrupt which cancels command through its
 * future and removes command from schedule queue.
 * <p>
 * OnInterrupt triggers error in command, which triggers onError to Command. If removeCommandObserver
 * is used then no callback to controller is done.
 */
public class CommandActionSubscription implements Subscription {
    private final AtomicReference<WeakReference<Command<?>>> commandReference;
    private final RxScheduledThreadPool<Runnable> observer;
    private final ScheduledFuture<?> future;

    CommandActionSubscription(Command<?> command, RxScheduledThreadPool<Runnable> observer, ScheduledFuture<?> future) {
        this.commandReference = new AtomicReference<>(new WeakReference<>(requireNonNull(command)));
        this.observer = requireNonNull(observer);
        this.future = requireNonNull(future);
    }

    @Override
    public void request(long n) {
        // NOOP
    }

    @Override
    public void cancel() {
        WeakReference<Command<?>> weakReference = commandReference.getAndSet(null);
        if (weakReference == null) {
            return;
        }

        Command<?> command = weakReference.get();
        if (command != null) {
            observer.onInterrupt(future, command);
            weakReference.clear();
        }
    }
}
