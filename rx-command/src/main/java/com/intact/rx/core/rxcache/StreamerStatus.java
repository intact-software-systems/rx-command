package com.intact.rx.core.rxcache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.rxcache.RxStreamerStatus;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.rxcache.acts.ActGroup;

public class StreamerStatus implements
        RxObserver<ActGroup>,
        RxStreamerStatus {

    private final Map<Integer, ActGroup> groups = new ConcurrentHashMap<>();
    private final AtomicReference<Throwable> throwable = new AtomicReference<>(null);
    private final AtomicInteger groupCounter = new AtomicInteger(1);
    private final ExecutionStatus executionStatus = new ExecutionStatus();
    private final AtomicReference<List<Throwable>> throwables = new AtomicReference<>(new ArrayList<>());

    public void setExceptions(List<Throwable> throwables) {
        this.throwables.set(throwables);
    }

    @Override
    public List<Throwable> getActionExceptions() {
        return throwables.get();
    }

    @Override
    public Throwable getException() {
        return throwable.get();
    }

    @Override
    public Map<Integer, ActGroup> finishedGroups() {
        return Collections.unmodifiableMap(groups);
    }

    @Override
    public ActGroup get(int groupIndex) {
        return groups.get(groupIndex);
    }

    @Override
    public ExecutionStatus getExecutionStatus() {
        return executionStatus.copy();
    }

    @Override
    public boolean isDone() {
        return !executionStatus.isExecuting() && !executionStatus.isNeverExecuted();
    }

    @Override
    public boolean isSuccess() {
        return executionStatus.isSuccess();
    }

    @Override
    public boolean isSubscribed() {
        return !executionStatus.isNeverExecuted();
    }

    @Override
    public boolean isDone(int groupIndex) {
        ActGroup group = groups.get(groupIndex);
        return group != null && group.isDone();
    }

    @Override
    public boolean isSuccess(int groupIndex) {
        ActGroup group = groups.get(groupIndex);
        return group != null && group.isSuccess();
    }

    @Override
    public boolean isSubscribed(int groupIndex) {
        ActGroup group = groups.get(groupIndex);
        return group != null && group.isSubscribed();
    }

    // --------------------------------------------
    // Interface RxObserver
    // --------------------------------------------

    @Override
    public void onComplete() {
        executionStatus.success();
    }

    @Override
    public void onError(Throwable throwable) {
        this.throwable.set(throwable);
        executionStatus.failure();
    }

    @Override
    public void onNext(ActGroup value) {
        boolean present = groups.entrySet()
                .stream()
                .anyMatch(entry -> Objects.equals(entry.getValue(), value));
        if (!present) {
            groups.put(groupCounter.getAndIncrement(), value);
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        executionStatus.start();
    }
}
