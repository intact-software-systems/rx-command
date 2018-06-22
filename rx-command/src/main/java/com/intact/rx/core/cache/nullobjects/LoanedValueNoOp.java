package com.intact.rx.core.cache.nullobjects;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.Loaned;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.LoanReturnPolicy;
import com.intact.rx.policy.Reservation;

public class LoanedValueNoOp<V> implements Loaned<V> {
    @SuppressWarnings("rawtypes")
    public static final LoanedValueNoOp instance = new LoanedValueNoOp();

    @Override
    public Object key() {
        return UUID.randomUUID();
    }

    @Override
    public Optional<V> read() {
        return Optional.empty();
    }

    @Override
    public Loaned<V> extend() {
        return this;
    }

    @Override
    public Loaned<V> extend(Lifetime lifetime) {
        return this;
    }

    @Override
    public boolean isExpired() {
        return true;
    }

    @Override
    public Lifetime getLifetime() {
        return Lifetime.zero();
    }

    @Override
    public Reservation getReservation() {
        return Reservation.NONE;
    }

    @Override
    public Duration getLoanStart() {
        return Duration.ZERO;
    }

    @Override
    public Optional<V> returnLoan() {
        return Optional.empty();
    }

    @Override
    public CacheHandle getCacheHandle() {
        return CacheHandle.uuid();
    }

    @Override
    public LoanReturnPolicy getReturnPolicy() {
        return LoanReturnPolicy.KEEP_ON_NO_LOAN;
    }

    @Override
    public void close() {

    }
}
