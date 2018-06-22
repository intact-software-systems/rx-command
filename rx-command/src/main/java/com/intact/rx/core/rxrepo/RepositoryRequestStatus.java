package com.intact.rx.core.rxrepo;

import java.util.List;
import java.util.Map;

import com.intact.rx.api.rxcache.RxStreamerStatus;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.rxcache.StreamerStatus;
import com.intact.rx.core.rxcache.acts.ActGroup;

public class RepositoryRequestStatus implements RxRequestStatus {

    private final RxStreamerStatus rxStreamerStatus;

    private RepositoryRequestStatus(RxStreamerStatus rxStreamerStatus) {
        this.rxStreamerStatus = rxStreamerStatus;
    }

    public static RepositoryRequestStatus create(RxStreamerStatus rxStreamerStatus) {
        return new RepositoryRequestStatus(rxStreamerStatus);
    }

    public static RepositoryRequestStatus no() {
        return new RepositoryRequestStatus(new StreamerStatus());
    }

    @Override
    public List<Throwable> getActionExceptions() {
        return rxStreamerStatus.getActionExceptions();
    }

    @Override
    public Throwable getException() {
        return rxStreamerStatus.getException();
    }

    @Override
    public Map<Integer, ActGroup> finishedGroups() {
        return rxStreamerStatus.finishedGroups();
    }

    @Override
    public ActGroup get(int n) {
        return rxStreamerStatus.get(n);
    }

    @Override
    public ExecutionStatus getExecutionStatus() {
        return rxStreamerStatus.getExecutionStatus();
    }

    @Override
    public boolean isDone() {
        return rxStreamerStatus.isDone();
    }

    @Override
    public boolean isSuccess() {
        return rxStreamerStatus.isSuccess();
    }

    @Override
    public boolean isSubscribed() {
        return rxStreamerStatus.isSubscribed();
    }

    @Override
    public boolean isDone(int groupIndex) {
        return rxStreamerStatus.isDone(groupIndex);
    }

    @Override
    public boolean isSuccess(int groupIndex) {
        return rxStreamerStatus.isSuccess(groupIndex);
    }

    @Override
    public boolean isSubscribed(int groupIndex) {
        return rxStreamerStatus.isSubscribed(groupIndex);
    }
}
