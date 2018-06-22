package com.intact.rx.api.rxrepo;

import java.time.Duration;
import java.util.Optional;

import com.intact.rx.api.cache.Loaned;
import com.intact.rx.policy.LoanPolicy;

public interface RxLoanedValueReader<V> extends RxValueReader<V> {

    /**
     * Loan cached value
     *
     * @return access to loaned value
     */
    Loaned<V> loanCached();

    /**
     * Compute if absent and then loan value.
     *
     * @param msecs max wait time
     * @return access to loaned value
     */
    Loaned<V> computeIfAbsentAndLoan(long msecs);

    /**
     * Overrides loan time set by LoanPolicy and sets new loan start-time/lifetime.
     *
     * @param msecs max wait time
     * @return value
     */
    Optional<V> refresh(long msecs);

    /**
     * @return true if loan is expired for current loaned value
     */
    boolean isExpired();

    /**
     * @return lifetime of loan
     */
    LoanPolicy getLoanPolicy();

    /**
     * @return start time of loan period
     */
    Duration getLoanStart();
}
