package com.intact.rx.core.cache.data.context;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.policy.Reservation;
import com.intact.rx.templates.AtomicSupplier;
import com.intact.rx.templates.Validate;
import com.intact.rx.templates.api.Memento;

public class ObjectRootState<K, V> {
    private final Memento<V> memento;
    private final K key;
    private final AccessStatus status;
    private final AtomicSupplier<LoanStatus> loanStatus;

    public ObjectRootState(K key, V value, Memento<V> memento) {
        this.key = requireNonNull(key);
        this.memento = requireNonNull(memento);
        this.memento.set(value);

        this.status = new AccessStatus();
        this.loanStatus = new AtomicSupplier<>(LoanStatus::new);
    }

    private ObjectRootState(ObjectRootState<K, V> state) {
        this.key = requireNonNull(state.key);
        this.memento = requireNonNull(state.memento.copy());

        this.status = state.status.copy();

        // ----------------------------------
        // Copy loan status if initialized
        // ----------------------------------
        Supplier<LoanStatus> supplyLoanStatus = LoanStatus::new;
        if (state.loanStatus.isSet()) {
            LoanStatus copy = state.loanStatus.get().copy();
            supplyLoanStatus = () -> copy;
        }
        this.loanStatus = new AtomicSupplier<>(supplyLoanStatus);
    }

    public ObjectRootState<K, V> copy() {
        return new ObjectRootState<>(this);
    }

    public Memento<V> getMemento() {
        return memento;
    }

    public AccessStatus getAccessStatus() {
        return status;
    }

    public V getValue() {
        return memento.get();
    }

    public K getKey() {
        return key;
    }

    public void setValue(V value) {
        memento.set(value);
    }

    // ----------------------------------------------
    // Loan status management
    // ----------------------------------------------

    public void resetLoan() {
        loanStatus.clear();
    }

    public boolean isLoaned() {
        return loanStatus.isSet() && loanStatus.get().count.get() > 0;
    }

    public boolean isImmutable() {
        return loanStatus.isSet() && loanStatus.get().isImmutable();
    }

    public LoanStatus getLoanStatus() {
        return loanStatus.get();
    }

    // ----------------------------------------------
    // Loan status
    // ----------------------------------------------

    @SuppressWarnings("PackageVisibleField")
    public static class LoanStatus {
        // Note: loan status
        final AtomicLong count;

        // Note: current loan policies
        Reservation type;
        int maxLimit;

        LoanStatus() {
            this.count = new AtomicLong(0);
            this.type = Reservation.NONE;
            this.maxLimit = 0;
        }

        LoanStatus(LoanStatus loanStatus) {
            this.count = new AtomicLong(loanStatus.count.intValue());
            this.type = loanStatus.type;
            this.maxLimit = loanStatus.maxLimit;
        }

        public LoanStatus copy() {
            return new LoanStatus(this);
        }

        boolean isImmutable() {
            return Objects.equals(type, Reservation.READ_ONLY);
        }

        public boolean isAcceptableReservation(Reservation reservation) {
            // Example: type = READ_WRITE(1000) >= reservation = READ_ONLY(100) -> true
            // Example: type = READ_ONLY(100) >= reservation = READ_WRITE(1000) -> false
            return type.equals(Reservation.NONE) || type.getPriveliges() >= reservation.getPriveliges();
        }

        public boolean isLoanedOut() {
            return maxLimit != 0 && count.get() >= maxLimit;
        }

        public long returnLoan() {
            return count.decrementAndGet();
        }

        public long newLoan(LoanPolicy loanPolicy) {
            Reservation reservation = loanPolicy.getReservation();

            if (Objects.equals(type, Reservation.NONE)) {
                type = reservation;
            } else {
                Validate.assertTrue(type.getPriveliges() >= reservation.getPriveliges());
                type = type.getPriveliges() > reservation.getPriveliges() ? type : reservation;
            }

            if (this.maxLimit <= 0) {
                this.maxLimit = loanPolicy.getMaxLimit();
            }

            return count.incrementAndGet();
        }

        @Override
        public String toString() {
            return "LoanStatus{" +
                    "count=" + count +
                    ", type=" + type +
                    '}';
        }
    }

    // ----------------------------------------------
    // Object stuff
    // ----------------------------------------------

    @Override
    public String toString() {
        return "ObjectRootState{" +
                "value=" + memento.get() +
                ", key=" + key +
                ", status=" + status +
                ", loanStatus=" + loanStatus +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectRootState)) return false;
        ObjectRootState<?, ?> that = (ObjectRootState<?, ?>) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
