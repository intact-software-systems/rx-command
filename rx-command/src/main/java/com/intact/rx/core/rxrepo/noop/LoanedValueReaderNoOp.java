package com.intact.rx.core.rxrepo.noop;

import java.time.Duration;
import java.util.Optional;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.Loaned;
import com.intact.rx.api.cache.RxValueAccess;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxrepo.RxLoanedValueReader;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.api.rxrepo.RxValueReader;
import com.intact.rx.core.cache.nullobjects.LoanedValueNoOp;
import com.intact.rx.core.cache.nullobjects.ValueAccessNoOp;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;
import com.intact.rx.policy.LoanPolicy;

public class LoanedValueReaderNoOp<K, V> implements RxLoanedValueReader<V> {
    @SuppressWarnings("rawtypes")
    public static final LoanedValueReaderNoOp instance = new LoanedValueReaderNoOp();

    @Override
    public Loaned<V> loanCached() {
        //noinspection unchecked
        return LoanedValueNoOp.instance;
    }

    @Override
    public Loaned<V> computeIfAbsentAndLoan(long msecs) {
        //noinspection unchecked
        return LoanedValueNoOp.instance;
    }

    @Override
    public Optional<V> refresh(long msecs) {
        return Optional.empty();
    }

    @Override
    public boolean isExpired() {
        return true;
    }

    @Override
    public LoanPolicy getLoanPolicy() {
        return LoanPolicy.zero();
    }

    @Override
    public Duration getLoanStart() {
        return Duration.ZERO;
    }

    @Override
    public RxValueAccess<V> accessCached() {
        //noinspection unchecked
        return ValueAccessNoOp.instance;
    }

    @Override
    public RxRequestStatus getStatus() {
        return RepositoryRequestStatus.no();
    }

    @Override
    public Optional<V> computeIfAbsent(long msecs) {
        return Optional.empty();
    }

    @Override
    public Optional<V> computeIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        return Optional.empty();
    }

    @Override
    public V computeValueIfAbsent(long msecs) {
        throw new IllegalAccessError("No value accessible");
    }

    @Override
    public V computeValueIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        throw new IllegalAccessError("No value accessible");
    }

    @Override
    public Optional<V> compute(long msecs) {
        return Optional.empty();
    }

    @Override
    public V computeValue(long msecs) {
        throw new IllegalAccessError("No value accessible");
    }

    @Override
    public RxValueReader<V> subscribe() {
        return this;
    }

    @Override
    public RxValueReader<V> waitFor(long msecs) {
        return this;
    }

    @Override
    public RxValueReader<V> onCompleteDo(VoidStrategy0 completedFunction) {
        return this;
    }

    @Override
    public RxValueReader<V> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        return this;
    }

    @Override
    public RxValueReader<V> onNextDo(VoidStrategy1<V> nextFunction) {
        return this;
    }

    @Override
    public RxValueReader<V> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        return this;
    }

    @Override
    public RxValueReader<V> connect(RxObserver<V> vRxObserver) {
        return this;
    }

    @Override
    public RxValueReader<V> disconnect(RxObserver<V> vRxObserver) {
        return this;
    }

    @Override
    public RxValueReader<V> disconnectAll() {
        return this;
    }
}
