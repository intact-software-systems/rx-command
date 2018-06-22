package com.intact.rx.core.rxcache.controller;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.subject.RxSubject;
import com.intact.rx.core.command.result.CommandFutureResult;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.machine.RxThreadPoolSchedulerStripped;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.core.rxcache.acts.ActGroupChain;
import com.intact.rx.core.rxcache.acts.ActGroupChainIterator;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;

class ActsControllerState {
    private final Lock schedulingMutex = new ReentrantLock();
    private final ExecutionStatus executionStatus = new ExecutionStatus();
    private final RxThreadPoolSchedulerStripped scheduler;
    private final ActGroupChain chain;
    private final CommandFutureResult<Void> result = new CommandFutureResult<>();
    private final ActGroupChainIterator iterator;
    private final CircuitId circuitBreakerId;
    private final RateLimiterId rateLimiterId;
    private final RxSubject<ActGroup> rxSubject = new RxSubject<>();

    ActsControllerState(RxThreadPoolSchedulerStripped scheduler, ActGroupChain chain, CircuitId circuitBreakerId, RateLimiterId rateLimiterId) {
        this.scheduler = requireNonNull(scheduler);
        this.chain = requireNonNull(chain);
        this.iterator = new ActGroupChainIterator(chain);
        this.circuitBreakerId = requireNonNull(circuitBreakerId);
        this.rateLimiterId = requireNonNull(rateLimiterId);
    }

    Lock schedulingMutex() {
        return schedulingMutex;
    }

    RxThreadPoolSchedulerStripped scheduler() {
        return scheduler;
    }

    ActGroupChainIterator iterator() {
        return iterator;
    }

    ActGroupChain chain() {
        //noinspection ReturnOfCollectionOrArrayField
        return chain;
    }

    CircuitId getCircuitBreakerId() {
        return circuitBreakerId;
    }

    RateLimiterId getRateLimiterId() {
        return rateLimiterId;
    }

    ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    CommandFutureResult<Void> getResult() {
        return result;
    }

    RxSubject<ActGroup> getRxSubject() {
        return rxSubject;
    }
}
