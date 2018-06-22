package com.intact.rx.api.rxcache;

import java.util.List;
import java.util.Map;

import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.rxcache.acts.ActGroup;

public interface RxStreamerStatus {

    List<Throwable> getActionExceptions();

    Throwable getException();

    Map<Integer, ActGroup> finishedGroups();

    ActGroup get(int n);

    ExecutionStatus getExecutionStatus();

    boolean isDone();

    boolean isSuccess();

    boolean isSubscribed();

    boolean isDone(int groupIndex);

    boolean isSuccess(int groupIndex);

    boolean isSubscribed(int groupIndex);
}
