package com.intact.rx.core.rxcache.controller;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.subject.RxSubject;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.machine.RxThreadPoolScheduler;
import com.intact.rx.core.rxcache.acts.ActGroup;

class ActsSchedulerState {
    private final Lock schedulingMutex = new ReentrantLock();
    private final ExecutionStatus executionStatus = new ExecutionStatus();
    private final RxThreadPoolScheduler scheduler;
    private final AtomicBoolean triggered;
    private final ActsCommandsController actsCommandsController;
    private final RxSubject<ActGroup> rxSubject = new RxSubject<>();

    ActsSchedulerState(RxThreadPoolScheduler scheduler, ActsCommandsController actsCommandsController) {
        this.scheduler = requireNonNull(scheduler);
        this.actsCommandsController = requireNonNull(actsCommandsController);
        this.triggered = new AtomicBoolean(false);
    }

    ActsCommandsController getActsCommandsController() {
        return actsCommandsController;
    }

    Lock schedulingMutex() {
        return schedulingMutex;
    }

    RxThreadPoolScheduler scheduler() {
        return scheduler;
    }

    ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    AtomicBoolean getTriggered() {
        return triggered;
    }

    RxSubject<ActGroup> getRxSubject() {
        return rxSubject;
    }
}
