package com.intact.rx.core.cache.data.api;

import java.util.Optional;
import java.util.function.Function;

import com.intact.rx.api.cache.*;
import com.intact.rx.policy.LoanPolicy;

public interface KeyValueCache<K, V> extends ReaderWriter<K, V>, MementoCache<K, V> {

    /**
     * @return cache identification
     */
    CacheHandle getCacheId();

    /**
     * @param selectionId id of selection
     * @param filter      data from this cache into selection
     * @return selection
     */
    RxSelection<K, V> computeSelectionIfAbsent(Object selectionId, Filter<V> filter);

    /**
     * @param key        to link with value
     * @param factory    to create value instance V iff key is not present in cache
     * @param loanPolicy policy on loan
     * @return optional  value non-null if loaned
     */
    Optional<V> computeIfAbsentAndLoan(K key, Function<? super K, ? extends V> factory, LoanPolicy loanPolicy);

    /**
     * @param key        to lookup in cache
     * @param loanPolicy type of reservation
     * @return value that is loaned if present
     */
    Optional<V> loan(K key, LoanPolicy loanPolicy);

    /**
     * Expires value represented by key from cache if it
     * a) was loaned, and
     * b) loan-count is zero after reloan (return of loaned value).
     *
     * @param loan to return
     * @return optional value for which loan was reloaned, if not reloaned then empty
     */
    Optional<V> returnLoan(Loaned<V> loan);

    /**
     * @param key to lookup in cache
     * @return true if value found is loaned
     */
    boolean isLoaned(K key);
}
