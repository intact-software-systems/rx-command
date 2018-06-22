package com.intact.rx.core.rxcache.acts;

import java.util.ArrayList;
import java.util.List;

import com.intact.rx.core.command.result.CommandFutureResult;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.rxcache.api.Act;

class ActGroupState {
    private final List<Act<?, ?>> chain = new ArrayList<>();
    private final List<Act<?, ?>> currentFinishedExecution = new ArrayList<>();

    private final ExecutionStatus executionStatus = new ExecutionStatus();
    private final CommandFutureResult<Void> result = new CommandFutureResult<>();

    <K, V> void chain(Act<K, V> access) {
        chain.add(access);
    }

    List<Act<?, ?>> getList() {
        //noinspection ReturnOfCollectionOrArrayField
        return chain;
    }

    List<Act<?, ?>> getCurrentFinishedExecution() {
        //noinspection ReturnOfCollectionOrArrayField
        return currentFinishedExecution;
    }

    ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    CommandFutureResult<Void> futureResult() {
        return result;
    }
}
