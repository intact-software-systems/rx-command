package com.intact.rx.core.command;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.api.subject.RxSubject;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.nullobjects.CommandResultNoOp;
import com.intact.rx.core.command.result.CommandFutureResult;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.machine.RxThreadPool;

public final class CommandControllerState<T> {
    private final Lock schedulingMutex = new ReentrantLock();
    private final AtomicBoolean triggered = new AtomicBoolean(false);
    private final RxSubject<T> rxSubject = new RxSubject<>();

    private final RxThreadPool controllerThreadPool;
    private final AtomicReference<RxThreadPool> commandThreadPool;
    private final AtomicReference<Commands<T>> commands;
    private final AtomicReference<ExecutionStatus> executionStatus;
    private final AtomicReference<CommandResult<T>> commandResult;

    // ----------------------------------
    // Constructor
    // ----------------------------------

    CommandControllerState(RxThreadPool threadPool) {
        this.controllerThreadPool = requireNonNull(threadPool);
        this.commandThreadPool = new AtomicReference<>(requireNonNull(threadPool));
        this.commands = new AtomicReference<>(new Commands<>());
        this.executionStatus = new AtomicReference<>(new ExecutionStatus());
        //noinspection unchecked,rawtypes
        this.commandResult = new AtomicReference<>(CommandResultNoOp.instance);
    }

    // ----------------------------------
    // Functions
    // ----------------------------------

    Lock schedulingMutex() {
        return schedulingMutex;
    }

    boolean setCommands(Commands<T> commands) {
        // Precondition: Validate.assertTrue(!commands.areCommandsExecuting());
        this.commands.set(requireNonNull(commands));
        return true;
    }

    boolean addCommand(Command<T> command) {
        return commands.get().add(command);
    }

    Commands<T> getCommands() {
        return commands.get();
    }

    void setCommandResult(CommandResult<T> commandResult) {
        this.commandResult.set(requireNonNull(commandResult));
    }

    CommandResult<T> getCommandResult() {
        return commandResult.get();
    }

    // -----------------------------------------------------------
    // Note: It is important that CommandResult is notified last because
    // other threads/observers may be using CommandResult to know when data
    // is available in cache, etc.
    // -----------------------------------------------------------

    void onComplete() {
        rxSubject.onComplete();

        CommandResult<T> ref = commandResult.get();
        if (ref instanceof CommandFutureResult) {
            ((CommandFutureResult<T>) ref).onComplete();
        }
    }

    void onError(Throwable throwable) {
        rxSubject.onError(throwable);

        CommandResult<T> ref = commandResult.get();
        if (ref instanceof CommandFutureResult) {
            ((CommandFutureResult<T>) ref).onError(throwable);
        }
    }

    void onNext(T values) {
        rxSubject.onNext(values);

        CommandResult<T> ref = commandResult.get();
        if (ref instanceof CommandFutureResult) {
            ((CommandFutureResult<T>) ref).onNext(values);
        }
    }

    void onSubscribe(Subscription subscription) {
        rxSubject.onSubscribe(subscription);

        CommandResult<T> ref = commandResult.get();
        if (ref instanceof CommandFutureResult) {
            ((CommandFutureResult<T>) ref).onSubscribe(subscription);
        }
    }

    // ----------------------------------
    // Execution management
    // ----------------------------------

    ExecutionStatus getExecutionStatus() {
        return executionStatus.get();
    }

    AtomicBoolean getTriggered() {
        return triggered;
    }

    void resetSubscription() {
        //noinspection unchecked
        commandResult.set(CommandResultNoOp.instance);
        executionStatus.set(new ExecutionStatus());
        commands.get().reset();
    }

    // ----------------------------------
    // Thread pool management
    // ----------------------------------

    RxThreadPool getControllerThreadPool() {
        return controllerThreadPool;
    }

    RxThreadPool getCommandThreadPool() {
        return commandThreadPool.get();
    }

    void setCommandThreadPool(RxThreadPool commandThreadPool) {
        this.commandThreadPool.set(requireNonNull(commandThreadPool));
    }

    // ----------------------------------
    // Subjects - observer management
    // ----------------------------------


    RxSubject<T> getRxSubject() {
        return rxSubject;
    }

    @Override
    public String toString() {
        return "CommandControllerState@" + hashCode() + " {\n" +
                ", \n           executionStatus=" + executionStatus +
                ", \n           commandResult=" + commandResult +
                ", \n           commands=" + commands +
                "\n         }";
    }
}
