package com.intact.rx.core.rxrepo.reader;

import java.time.Duration;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.*;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxrepo.RxLoanedValueReader;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.api.rxrepo.RxValueReader;
import com.intact.rx.core.cache.nullobjects.LoanedValueNoOp;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.core.cache.strategy.CachePolicyChecker;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;
import com.intact.rx.exception.ExecutionNotStartedException;
import com.intact.rx.policy.LoanPolicy;

public class LoanedValueReaderOfCachedData<K, V> implements RxLoanedValueReader<V> {
    private final CacheHandle dataCacheHandle;
    private final CachePolicy dataCachePolicy;
    private final K key;
    private final LoanPolicy loanPolicy;
    private final AccessStatus accessStatus;

    public LoanedValueReaderOfCachedData(CacheHandle dataCacheHandle, CachePolicy dataCachePolicy, K key, LoanPolicy loanPolicy) {
        this.dataCacheHandle = requireNonNull(dataCacheHandle);
        this.dataCachePolicy = requireNonNull(dataCachePolicy);
        this.key = requireNonNull(key);
        this.loanPolicy = requireNonNull(loanPolicy);
        this.accessStatus = new AccessStatus();
    }

    @Override
    public Loaned<V> loanCached() {
        return isExpiredLoan()
                ? LoanedValueNoOp.instance
                : cache().loan(key, loanPolicy);
    }

    @Override
    public Loaned<V> computeIfAbsentAndLoan(long msecs) {
        return isExpiredLoan()
                ? LoanedValueNoOp.instance
                : cache().loan(key, loanPolicy);
    }

    @Override
    public Optional<V> refresh(long msecs) {
        Optional<V> value = cache().read(key);
        value.ifPresent(v -> accessStatus.read());
        return value;
    }

    @Override
    public boolean isExpired() {
        return isExpiredLoan();
    }

    @Override
    public LoanPolicy getLoanPolicy() {
        return loanPolicy;
    }

    @Override
    public Duration getLoanStart() {
        return accessStatus.isRead()
                ? Duration.ofMillis(accessStatus.getTime().getTimeSinceRead())
                : Duration.ZERO;
    }

    @Override
    public RxValueAccess<V> accessCached() {
        return cache().access(key);
    }

    @Override
    public RxRequestStatus getStatus() {
        return RepositoryRequestStatus.no();
    }

    @Override
    public Optional<V> computeIfAbsent(long msecs) {
        return isExpiredLoan()
                ? Optional.empty()
                : cache().read(key);
    }

    @Override
    public Optional<V> computeIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        return isExpiredLoan()
                ? Optional.empty()
                : acceptCachedOrCompute(key, computeResolver);
    }

    @Override
    public V computeValueIfAbsent(long msecs) {
        if (isExpiredLoan()) {
            throw new ExecutionNotStartedException("Request of key " + key + " not started");
        }

        return cache().read(key).orElseThrow(() -> new ExecutionNotStartedException("Request of key " + key + " not started"));
    }

    @Override
    public V computeValueIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        if (isExpiredLoan()) {
            throw new ExecutionNotStartedException("Request of key " + key + " not started");
        }

        return acceptCachedOrCompute(key, computeResolver).orElseThrow(() -> new ExecutionNotStartedException("Request of key " + key + " not started"));
    }

    @Override
    public Optional<V> compute(long msecs) {
        return isExpiredLoan()
                ? Optional.empty()
                : cache().read(key);
    }

    @Override
    public V computeValue(long msecs) {
        if (isExpiredLoan()) {
            throw new ExecutionNotStartedException("Request of key " + key + " not started");
        }

        return cache().read(key).orElseThrow(() -> new ExecutionNotStartedException("Request of key " + key + " not started"));
    }

    @Override
    public RxValueReader<V> subscribe() {
        return this;
    }

    @Override
    public RxValueReader<V> waitFor(long msecs) {
        return this;
    }

    // -----------------------------------------------------------
    // Callback functions
    // -----------------------------------------------------------

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
    public RxValueReader<V> connect(RxObserver<V> observer) {
        return this;
    }

    @Override
    public RxValueReader<V> disconnect(RxObserver<V> observer) {
        return this;
    }

    @Override
    public RxValueReader<V> disconnectAll() {
        return this;
    }

    // -----------------------------------------------------------
    // private implementation
    // -----------------------------------------------------------

    private boolean isExpiredLoan() {
        return !(CachePolicyChecker.isInLifetime(accessStatus, loanPolicy.getLifetime()) && accessStatus.isRead());
    }

    protected RxCache<K, V> cache() {
        return RxCacheAccess.cache(dataCacheHandle, dataCachePolicy);
    }

    private Optional<V> acceptCachedOrCompute(K key, Strategy1<Boolean, V> computeResolver) {
        Optional<V> value = this.cache().read(key);
        if (!value.isPresent()) {
            return value;
        }

        return !computeResolver.perform(value.get())
                ? value
                : Optional.empty();
    }
}
