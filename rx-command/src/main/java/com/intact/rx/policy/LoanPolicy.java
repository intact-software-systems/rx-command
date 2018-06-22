package com.intact.rx.policy;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("WeakerAccess")
public final class LoanPolicy {
    private static final LoanPolicy readWriteExpireOnNoLoan = readWrite(Lifetime.forever(), LoanReturnPolicy.REMOVE_ON_NO_LOAN, MaxLimit.unlimited());
    private static final LoanPolicy readWriteKeepOnNoLoan = readWrite(Lifetime.forever(), LoanReturnPolicy.KEEP_ON_NO_LOAN, MaxLimit.unlimited());
    private static final LoanPolicy zero = readWrite(Lifetime.zero(), LoanReturnPolicy.REMOVE_ON_NO_LOAN, MaxLimit.unlimited());
    private static final LoanPolicy exclusiveReadOnlyKeepOnNoLoan = readOnly(Lifetime.forever(), LoanReturnPolicy.KEEP_ON_NO_LOAN, MaxLimit.one());
    private static final LoanPolicy exclusiveReadOnlyExpireOnNoLoan = readOnly(Lifetime.forever(), LoanReturnPolicy.REMOVE_ON_NO_LOAN, MaxLimit.one());

    private final Reservation reservation;
    private final LoanReturnPolicy loanReturn;
    private final Lifetime lifetime;

    private final MaxLimit maxLimit;

    public LoanPolicy(Reservation reservation, LoanReturnPolicy loanReturn, Lifetime lifetime, MaxLimit maxLimit) {
        this.reservation = requireNonNull(reservation);
        this.loanReturn = requireNonNull(loanReturn);
        this.lifetime = requireNonNull(lifetime);
        this.maxLimit = requireNonNull(maxLimit);
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Lifetime getLifetime() {
        return lifetime;
    }

    public LoanReturnPolicy getReturnPolicy() {
        return loanReturn;
    }

    public int getMaxLimit() {
        return maxLimit.getLimit();
    }

    public static LoanPolicy readWrite(Lifetime lifetime, LoanReturnPolicy loanReturn, MaxLimit maxLimit) {
        return new LoanPolicy(Reservation.READ_WRITE, loanReturn, lifetime, maxLimit);
    }

    public static LoanPolicy readOnly(Lifetime lifetime, LoanReturnPolicy loanReturn, MaxLimit maxLimit) {
        return new LoanPolicy(Reservation.READ_ONLY, loanReturn, lifetime, maxLimit);
    }

    public static LoanPolicy exclusiveReadOnlyKeepOnNoLoan() {
        return exclusiveReadOnlyKeepOnNoLoan;
    }

    public static LoanPolicy exclusiveReadOnlyExpireOnNoLoan() {
        return exclusiveReadOnlyExpireOnNoLoan;
    }

    public static LoanPolicy readWriteExpireOnNoLoan() {
        return readWriteExpireOnNoLoan;
    }

    public static LoanPolicy readWriteKeepOnNoLoan() {
        return readWriteKeepOnNoLoan;
    }

    public static LoanPolicy zero() {
        return zero;
    }

    @Override
    public String toString() {
        return "Loan{" +
                "reservation=" + reservation +
                ", lifetime=" + lifetime +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanPolicy)) return false;
        LoanPolicy loanPolicy = (LoanPolicy) o;
        return reservation == loanPolicy.reservation &&
                Objects.equals(lifetime, loanPolicy.lifetime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservation, lifetime);
    }
}
