package com.intact.rx.core.rxcache.act;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.api.subject.RxKeyValueSubject;
import com.intact.rx.api.subject.RxLambdaSubject;
import com.intact.rx.api.subject.RxSubject;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.core.command.Commands;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.core.command.nullobjects.CommandControllerNoOp;
import com.intact.rx.core.command.nullobjects.CommandResultNoOp;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.rxcache.api.Act;
import com.intact.rx.templates.Validate;

public class ActState<K, V> {
    private final CacheHandle cacheHandle;
    private final RxThreadPool commandThreadPool;
    private final Commands<Map<K, V>> commands;

    private final AtomicReference<Commands<Map<K, V>>> commandsWithState;
    private final AtomicReference<CommandResult<Map<K, V>>> result;
    private final AtomicReference<CommandController<Map<K, V>>> controller;

    private final RxSubject<Map<K, V>> rxSubject = new RxSubject<>();
    private final RxLambdaSubject<Map<K, V>> lambdaSubject = new RxLambdaSubject<>();
    private final RxKeyValueSubject<Act<?, ?>, Map<?, ?>> actSubject = new RxKeyValueSubject<>();

    private final ExecutionStatus executionStatus = new ExecutionStatus();
    private final AccessStatus accessStatus = new AccessStatus();

    // ----------------------------------
    // Constructor
    // ----------------------------------

    ActState(CacheHandle cacheHandle, RxThreadPool commandThreadPool, Commands<Map<K, V>> commands) {
        this.cacheHandle = requireNonNull(cacheHandle);
        this.commandThreadPool = requireNonNull(commandThreadPool);
        this.commands = requireNonNull(commands);
        Validate.assertTrue(!commands.isEmpty());

        commandsWithState = new AtomicReference<>(new Commands<>());
        //noinspection unchecked,rawtypes
        this.result = new AtomicReference<>(CommandResultNoOp.instance);
        //noinspection unchecked,rawtypes
        this.controller = new AtomicReference<>(CommandControllerNoOp.instance);
    }

    // ----------------------------------
    // Access to immutables
    // ----------------------------------

    CacheHandle getCacheHandle() {
        return cacheHandle;
    }

    RxThreadPool getCommandThreadPool() {
        return commandThreadPool;
    }

    Commands<Map<K, V>> getCommands() {
        //noinspection ReturnOfCollectionOrArrayField
        return commands;
    }

    void setCommandsWithState(Commands<Map<K, V>> commandsWithState) {
        this.commandsWithState.set(commandsWithState);
    }

    Commands<Map<K, V>> getCommandsWithState() {
        return commandsWithState.get();
    }

    // ----------------------------------
    // Access to thread-safe finals
    // ----------------------------------

    RxLambdaSubject<Map<K, V>> getLambdaSubject() {
        return lambdaSubject;
    }

    RxSubject<Map<K, V>> getRxSubject() {
        return rxSubject;
    }

    RxKeyValueSubject<Act<?, ?>, Map<?, ?>> getActSubject() {
        return actSubject;
    }

    ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    AccessStatus getAccessStatus() {
        return accessStatus;
    }

    // ----------------------------------
    // Callback to subjects
    // ----------------------------------

    void onSubscribe(Subscription subscription) {
        rxSubject.onSubscribe(subscription);
        lambdaSubject.onSubscribe(subscription);
    }

    void onComplete() {
        rxSubject.onComplete();
        lambdaSubject.onComplete();
    }

    void onError(Throwable throwable) {
        rxSubject.onError(throwable);
        lambdaSubject.onError(throwable);
    }

    void onNext(Map<K, V> values) {
        rxSubject.onNext(values);
        lambdaSubject.onNext(values);
    }

    // ----------------------------------
    // Functions
    // ----------------------------------

    CommandResult<Map<K, V>> getResult() {
        return result.get();
    }

    void setResult(CommandResult<Map<K, V>> result) {
        this.result.set(result);
    }

    CommandController<Map<K, V>> getController() {
        return controller.get();
    }

    void setController(CommandController<Map<K, V>> newController) {
        accessStatus.modified();
        controller.set(requireNonNull(newController));
    }

    void resetController() {
        accessStatus.modified();
        //noinspection unchecked
        controller.set(CommandControllerNoOp.instance);
    }

    @Override
    public String toString() {
        return "ActState{cacheHandle=" + cacheHandle + "}";
    }
}
