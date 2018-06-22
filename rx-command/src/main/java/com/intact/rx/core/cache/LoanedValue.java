package com.intact.rx.core.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.Loaned;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.core.cache.strategy.CachePolicyChecker;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.policy.LoanReturnPolicy;
import com.intact.rx.policy.Reservation;
import com.intact.rx.templates.AtomicLoan;

public class LoanedValue<K, V> implements Loaned<V> {
    private static final Logger log = LoggerFactory.getLogger(LoanedValue.class);

    private final K key;
    private final AtomicReference<Lifetime> lifetime;
    private final Strategy1<Optional<V>, Loaned<V>> returnLoan;
    private final Strategy0<Optional<V>> cachedValueAccess;
    private final CacheHandle cacheHandle;
    private final LoanPolicy loanPolicy;
    private final AccessStatus accessStatus;

    private AtomicLoan<V> atomicLoan;

    public LoanedValue(K key, Strategy0<Optional<V>> cachedValueAccess, Strategy1<Optional<V>, Loaned<V>> returnLoan, CacheHandle cacheHandle, LoanPolicy loanPolicy) {
        this.key = requireNonNull(key);
        this.cachedValueAccess = requireNonNull(cachedValueAccess);
        this.cacheHandle = requireNonNull(cacheHandle);
        this.loanPolicy = requireNonNull(loanPolicy);
        this.lifetime = new AtomicReference<>(requireNonNull(loanPolicy.getLifetime()));
        this.returnLoan = requireNonNull(returnLoan);
        this.accessStatus = new AccessStatus();
        this.accessStatus.read();
    }

    @Override
    public K key() {
        return key;
    }


    @Override
    public Optional<V> read() {
        return CachePolicyChecker.isInLifetime(accessStatus, lifetime.get())
                ? computeAtomicLoanIfAbsent()
                : Optional.empty();
    }

    @Override
    public Loaned<V> extend() {
        if (!accessStatus.isExpired()) {
            atomicLoan = null;
            accessStatus.read();
        }
        return this;
    }

    @Override
    public Loaned<V> extend(Lifetime newLifetime) {
        if (!accessStatus.isExpired()) {
            atomicLoan = null;
            accessStatus.read();
            lifetime.set(newLifetime);
        }
        return this;
    }

    @Override
    public boolean isExpired() {
        return !CachePolicyChecker.isInLifetime(accessStatus, lifetime.get());
    }

    @Override
    public Lifetime getLifetime() {
        return lifetime.get();
    }

    @Override
    public Reservation getReservation() {
        return loanPolicy.getReservation();
    }

    @Override
    public Duration getLoanStart() {
        return Duration.ofMillis(accessStatus.getTime().getTimeSinceRead());
    }

    @Override
    public Optional<V> returnLoan() {
        // already returned?
        if (accessStatus.isExpired()) {
            return Optional.empty();
        }

        atomicLoan = null;
        accessStatus.expired();
        return returnLoan.perform(this);
    }


    @Override
    public void close() {
        returnLoan();
    }

    @Override
    public CacheHandle getCacheHandle() {
        return cacheHandle;
    }

    @Override
    public LoanReturnPolicy getReturnPolicy() {
        return loanPolicy.getReturnPolicy();
    }

    @Override
    public String toString() {
        return "LoanedValue{" +
                "key=" + key +
                ", lifetime=" + lifetime +
                ", loanPolicy=" + loanPolicy +
                ", accessStatus=" + accessStatus +
                '}';
    }

    private Optional<V> computeAtomicLoanIfAbsent() {
        try {
            if (atomicLoan != null) {
                return atomicLoan.isExpired()
                        ? Optional.ofNullable(atomicLoan.get())
                        : this.atomicLoan.read();
            }

            atomicLoan =
                    new AtomicLoan<>(
                            () -> cachedValueAccess.perform().orElse(null),
                            lifetime.get().duration(),
                            (V v) -> {
                                boolean isExpired = isExpired();
                                if (isExpired) {
                                    returnLoan();
                                }
                                return !isExpired;
                            },
                            AtomicLoan.Policy.LoanOnceIfNull
                    );

            return Optional.ofNullable(atomicLoan.get());
        } catch (Exception e) {
            log.warn("Exception caught when loaning value. Returning Optional.empty().", e);
            return Optional.empty();
        }
    }
}
