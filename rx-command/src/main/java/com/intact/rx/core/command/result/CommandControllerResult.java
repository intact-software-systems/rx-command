package com.intact.rx.core.command.result;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.templates.Tuple2;

public final class CommandControllerResult<V> implements CommandResult<V> {
    private final CommandFutureResult<V> result;
    private final WeakReference<CommandController<V>> controller;

    public CommandControllerResult(final CommandFutureResult<V> result, final CommandController<V> controller) {
        this.result = result;
        this.controller = new WeakReference<>(controller);
    }

    @Override
    public List<V> result() {
        return result.result();
    }

    @Override
    public boolean isDone() {
        return result.isDone();
    }

    @Override
    public List<V> get() {
        return result.get();
    }

    @Override
    public List<V> get(long timeout, TimeUnit unit) {
        return result.get(timeout, unit);
    }

    @Override
    public boolean isSuccess() {
        return result.isSuccess();
    }

    @Override
    public boolean isSubscribed() {
        return result.isSubscribed();
    }

    @Override
    public boolean isValid() {
        return result.isValid();
    }

    @Override
    public boolean cancel(boolean mayInterrupt) {
        result.cancel(mayInterrupt);

        CommandController<V> ctrl = controller.get();
        if (ctrl == null) {
            return false;
        }

        controller.clear();

        return ctrl.unsubscribe();
    }

    @Override
    public boolean isCancelled() {
        return result.isCancelled();
    }

    @Override
    public boolean waitFor(long msecs) {
        return result.waitFor(msecs);
    }

    @Override
    public Tuple2<FutureStatus, List<V>> getResult(long msecs) {
        return result.getResult(msecs);
    }
}
