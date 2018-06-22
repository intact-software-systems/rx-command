package com.intact.rx.api.cache;

import java.time.Duration;
import java.util.Optional;

import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.LoanReturnPolicy;
import com.intact.rx.policy.Reservation;

/**
 * Access to a optional loaned value. The loan time is represented by a Lifetime duration.
 * The optional loaned value is accessed with read function until loan expires. The loan may be renewed
 *
 * @param <V> type of value represented
 */
public interface Loaned<V> extends AutoCloseable {

    /**
     * @return key to find loaned value
     */
    Object key();

    /**
     * @return optional value as long as loan lifetime is not expired
     */
    Optional<V> read();

    /**
     * @return this after extending loan using existing lifetime
     */
    Loaned<V> extend();

    /**
     * @return this after extending loan with new lifetime
     */
    Loaned<V> extend(Lifetime lifetime);

    /**
     * @return true if loan is expired for current loaned value
     */
    boolean isExpired();

    /**
     * @return lifetime of loan
     */
    Lifetime getLifetime();

    /**
     * @return type of reservation
     */
    Reservation getReservation();

    /**
     * @return start time of loan period
     */
    Duration getLoanStart();

    /**
     * Returns the loan of this value. This may expire the value in the cache, depending on the LoanReturnPolicy.
     *
     * @return value present if removed from cache
     */
    Optional<V> returnLoan();

    /**
     * Alternate return loan, for convenience for "execute around object" pattern, i.e., try with resource close.
     */
    @Override
    void close();

    /**
     * @return handle of stored cache
     */
    CacheHandle getCacheHandle();

    /**
     * @return loan return policy
     */
    LoanReturnPolicy getReturnPolicy();
}
