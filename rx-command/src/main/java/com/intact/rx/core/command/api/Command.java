package com.intact.rx.core.command.api;

import java.util.Map;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.Action;
import com.intact.rx.api.command.Action0;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.observer.CommandObserver;
import com.intact.rx.core.command.observer.RxActionObserver;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;

public interface Command<T> extends Runnable, RxObserver<T>, RxActionObserver<T>, Subscription {

    Subscription subscribe(CommandObserver<T> observer, RxThreadPool threadPool);

    void unsubscribe();

    long readyInMs();

    long timeoutInMs();

    boolean isReady();

    boolean isSuccess();

    boolean isExecuting();

    boolean isDone();

    boolean isTimeout();

    boolean isCancelled();

    boolean isPolicyViolated();

    void reset();

    void doTimeout();

    CommandPolicy getPolicy();

    ExecutionStatus getExecutionStatus();

    Map<Action, Throwable> getExceptions();

    CircuitId getCircuitBreakerId();

    RateLimiterId getRateLimiterId();

    Iterable<Action0<T>> getActions();
}
