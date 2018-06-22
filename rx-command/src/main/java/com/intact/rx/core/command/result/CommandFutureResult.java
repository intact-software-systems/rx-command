package com.intact.rx.core.command.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.templates.Tuple2;
import com.intact.rx.templates.Validate;

public class CommandFutureResult<T> implements
        CommandResult<T>,
        RxObserver<T> {
    private CommandResultState<T> context;
    private final Lock mutex;
    private final Condition waitForData;

    private static final Logger log = LoggerFactory.getLogger(CommandFutureResult.class);

    public CommandFutureResult() {
        this.context = new CommandResultState<>();
        this.mutex = new ReentrantLock();
        this.waitForData = this.mutex.newCondition();
    }

    // -----------------------------------------------------------
    // Interface CommandResult<T>
    // -----------------------------------------------------------

    @Override
    public List<T> result() {
        try {
            mutex.lock();
            return context.getResult();
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public boolean isDone() {
        try {
            mutex.lock();
            return context.isDone();
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public boolean isSuccess() {
        try {
            mutex.lock();
            return context.isSuccess();
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public boolean isSubscribed() {
        try {
            mutex.lock();
            return context.isSubscribed();
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public boolean isCancelled() {
        try {
            mutex.lock();
            return context.isCancelled();
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public boolean isValid() {
        // as long as this class is instantiated it is always valid
        return true;
    }

    @Override
    public boolean cancel(boolean mayInterrupt) {
        try {
            mutex.lock();
            context.cancel();
            return true;
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public boolean waitFor(long msecs) {
        FutureStatus futureStatus = waitForPrivate(msecs).first;
        return futureStatus.equals(FutureStatus.Success) || futureStatus.equals(FutureStatus.Failed);
    }

    @Override
    public Tuple2<FutureStatus, List<T>> getResult(long msecs) {
        return waitForPrivate(msecs);
    }

    @Override
    public List<T> get() {
        return waitForPrivate(Long.MAX_VALUE).second;
    }

    @Override
    public List<T> get(long timeout, TimeUnit unit) {
        return waitForPrivate(unit.toMillis(timeout)).second;
    }

    // -----------------------------------------------------------
    // Interface RxObserver<T>
    // -----------------------------------------------------------

    @Override
    public void onComplete() {
        onSuccessProcessor();
    }

    @Override
    public void onError(Throwable throwable) {
        onErrorProcessor(throwable);
    }

    @Override
    public void onNext(T values) {
        onNextProcessor(values);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        onSubscribeProcessor();
    }

    // -----------------------------------------------------------
    // Public functions only called by command core library
    // -----------------------------------------------------------

    public void resetAndSubscribe() {
        try {
            mutex.lock();
            context = new CommandResultState<>();
            context.subscribe();
        } finally {
            mutex.unlock();
        }
    }

    // -----------------------------------------------------------
    // Private functions and structures
    // -----------------------------------------------------------

    private Tuple2<FutureStatus, List<T>> waitForPrivate(long msecs) {
        Validate.assertTrue(msecs >= 0);

        Tuple2<FutureStatus, List<T>> result;
        long startTime = System.currentTimeMillis();
        try {
            long waitTime = msecs;
            mutex.lock();
            while (!context.isDone() && context.isSubscribed() && waitTime > 0) {
                waitForData.await(waitTime, TimeUnit.MILLISECONDS);

                long elapsed = System.currentTimeMillis() - startTime;
                waitTime = Math.max(0, msecs - elapsed);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("{} interrupted after {}ms while waiting for context to complete", this, System.currentTimeMillis() - startTime, e);
        } finally {
            try {
                result = new Tuple2<>(
                        context.isInFinalState() ? context.getStatus()
                                : !context.isSubscribed() ? FutureStatus.NotStarted
                                : FutureStatus.Timedout,
                        context.getResult());
            } finally {
                mutex.unlock();
            }
        }
        return requireNonNull(result);
    }

    private void onErrorProcessor(Throwable throwable) {
        try {
            mutex.lock();
            context.error(throwable);
            waitForData.signalAll();
        } finally {
            mutex.unlock();
        }
    }

    private void onNextProcessor(T value) {
        try {
            mutex.lock();
            context.next(value);
        } finally {
            mutex.unlock();
        }
    }

    private void onSuccessProcessor() {
        try {
            mutex.lock();
            context.success();
            waitForData.signalAll();
        } finally {
            mutex.unlock();
        }
    }

    private void onSubscribeProcessor() {
        try {
            mutex.lock();
            context.subscribe();
        } finally {
            mutex.unlock();
        }
    }

    private static class CommandResultState<T> {
        private boolean isSubscribed;
        private boolean isError;
        private boolean isDone;
        private boolean isSuccess;
        private boolean isCancelled;
        private FutureStatus status;
        private final List<T> result;
        private Throwable throwable;

        private CommandResultState() {
            isSubscribed = false;
            isError = false;
            isDone = false;
            isSuccess = false;
            isCancelled = false;
            status = FutureStatus.NotStarted;
            result = new ArrayList<>();
            throwable = new Throwable("No error");
        }

        private void error(Throwable e) {
            isError = true;
            isDone = true;
            isSuccess = false;
            status = FutureStatus.Failed;
            throwable = e;
        }

        private void next(T value) {
            result.add(value);
        }

        private void success() {
            isError = false;
            isDone = true;
            isSuccess = true;
            status = FutureStatus.Success;
        }

        private void subscribe() {
            isError = false;
            isDone = false;
            isSuccess = false;
            isSubscribed = true;
            status = FutureStatus.Subscribed;
        }

        private void cancel() {
            isDone = true;
            isCancelled = true;
        }

        private boolean isSubscribed() {
            return isSubscribed;
        }

        private boolean isError() {
            return isError;
        }

        private boolean isDone() {
            return this.isDone;
        }

        private boolean isSuccess() {
            return this.isSuccess;
        }

        private boolean isCancelled() {
            return isCancelled;
        }

        private List<T> getResult() {
            return Collections.unmodifiableList(this.result);
        }

        private Throwable getThrowable() {
            return this.throwable;
        }

        public FutureStatus getStatus() {
            return status;
        }

        private boolean isInFinalState() {
            return status == FutureStatus.Success || status == FutureStatus.Failed;
        }
    }
}
