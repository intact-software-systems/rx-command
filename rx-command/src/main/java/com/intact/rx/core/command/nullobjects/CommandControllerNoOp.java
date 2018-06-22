package com.intact.rx.core.command.nullobjects;

import java.util.Collection;
import java.util.Collections;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.core.command.Commands;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.core.machine.RxThreadPool;

public class CommandControllerNoOp<T> implements CommandController<T> {

    @SuppressWarnings("rawtypes")
    public static final CommandControllerNoOp instance = new CommandControllerNoOp();

    @Override
    public Commands<T> getCommands() {
        return Commands.emptyList();
    }

    @Override
    public boolean setCommands(Commands<T> commands) {
        return false;
    }

    @Override
    public boolean addCommand(Command<T> command) {
        return false;
    }

    @Override
    public CommandResult<T> subscribe() {
        //noinspection unchecked
        return CommandResultNoOp.instance;
    }

    @Override
    public boolean unsubscribe() {
        return false;
    }

    @Override
    public CommandResult<T> trigger() {
        //noinspection unchecked
        return CommandResultNoOp.instance;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isExecuting() {
        return false;
    }

    @Override
    public boolean areCommandsExecuting() {
        return false;
    }

    @Override
    public int numCommandsExecuting() {
        return 0;
    }

    @Override
    public long timeoutInMs() {
        return Long.MAX_VALUE;
    }

    @Override
    public Collection<Throwable> getExceptions() {
        return Collections.emptyList();
    }

    @Override
    public boolean setCommandThreadPool(RxThreadPool commandThreadPool) {
        return false;
    }

    @Override
    public void disconnectAll() {
    }

    @Override
    public boolean connect(RxObserver<T> observer) {
        return false;
    }

    @Override
    public boolean disconnect(RxObserver<T> observer) {
        return false;
    }
}
