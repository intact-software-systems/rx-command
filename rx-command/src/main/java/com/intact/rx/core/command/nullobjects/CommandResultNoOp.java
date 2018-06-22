package com.intact.rx.core.command.nullobjects;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.templates.Tuple2;

public class CommandResultNoOp<T> implements CommandResult<T> {
    @SuppressWarnings("rawtypes")
    public static final CommandResultNoOp instance = new CommandResultNoOp<>();

    @Override
    public List<T> result() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public List<T> get() {
        return Collections.emptyList();
    }

    @Override
    public List<T> get(long timeout, TimeUnit unit) {
        return Collections.emptyList();
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isSubscribed() {
        return false;
    }

    @Override
    public boolean isValid() {
        // as long as this class is instantiated it is always invalid
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterrupt) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean waitFor(long msecs) {
        return false;
    }

    @Override
    public Tuple2<FutureStatus, List<T>> getResult(long msecs) {
        return new Tuple2<>(FutureStatus.NotStarted, Collections.emptyList());
    }
}
