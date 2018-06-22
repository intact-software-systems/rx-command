package com.intact.rx.templates.api;

import java.util.Optional;
import java.util.function.Function;

public interface LoanFunction<T> {

    /**
     * @return loaned value as long as loan lifetime is not expired
     */
    Optional<T> read();

    /**
     * Read loaned value if not expired and present, otherwise loan the value (again). The loanSupplier is called with current loaned value as input.
     *
     * @param loanSupplier with current loaned value as input
     * @return loaned value
     * @throws IllegalStateException if loan returns null
     */
    T get(Function<Optional<T>, T> loanSupplier);

    /**
     * Loan the value (again). The loanSupplier is called with current loaned value as input.
     *
     * @param loanSupplier with current loaned value as input
     * @return loaned value
     * @throws IllegalStateException if loan returns null
     */
    T refresh(Function<Optional<T>, T> loanSupplier);
}
