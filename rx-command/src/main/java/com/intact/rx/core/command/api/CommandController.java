package com.intact.rx.core.command.api;

import java.util.Collection;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.core.command.Commands;
import com.intact.rx.core.machine.RxThreadPool;

public interface CommandController<T> {
    boolean setCommands(Commands<T> commands);

    boolean addCommand(Command<T> command);

    Commands<T> getCommands();

    CommandResult<T> subscribe();

    boolean unsubscribe();

    CommandResult<T> trigger();

    boolean isDone();

    boolean isSuccess();

    boolean isExecuting();

    boolean areCommandsExecuting();

    int numCommandsExecuting();

    long timeoutInMs();

    Collection<Throwable> getExceptions();

    boolean setCommandThreadPool(RxThreadPool commandThreadPool);

    void disconnectAll();

    boolean connect(RxObserver<T> observer);

    boolean disconnect(RxObserver<T> observer);
}
