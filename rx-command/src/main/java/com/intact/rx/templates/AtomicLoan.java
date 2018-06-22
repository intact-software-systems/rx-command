package com.intact.rx.templates;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.templates.api.LoanFunction;

/**
 * Thread safe approach to loan a value provided by an input supplier for a configurable duration.
 *
 * @param <T> type of the loaned value
 */
public class AtomicLoan<T> implements LoanFunction<T>, Supplier<T> {
    private static final Duration maximumLoanDuration = Duration.ofMillis(Long.MAX_VALUE);
    private static final long maxWaitTimeMsecs = Long.MAX_VALUE;
    private static final Logger log = LoggerFactory.getLogger(AtomicLoan.class);
    private static final String singleRefresher = "RefresherId";

    // -------------------------------------------------------
    // Private structures
    // -------------------------------------------------------

    private enum Intention {
        Refresh,
        GetNonExpired
    }

    private enum State {
        NotExecuting,
        Executing
    }

    private enum ResultState {
        None,
        Success,
        Failure
    }

    public enum Policy {
        LoanOnceIfNull,
        ReloanIfExpired
    }

    @SuppressWarnings("PackageVisibleField")
    private static class Command {
        final Lock mutex;
        final Condition condition;

        final AtomicReference<State> executionState;
        final AtomicReference<ResultState> resultState;

        Command() {
            this.mutex = new ReentrantLock();
            this.condition = mutex.newCondition();
            this.executionState = new AtomicReference<>(State.NotExecuting);
            this.resultState = new AtomicReference<>(ResultState.None);
        }
    }

    // -------------------------------------------------------
    // Class fields
    // -------------------------------------------------------

    private final Policy policy;
    private final Duration loanDuration;
    private final Supplier<T> supplier;
    private final Function<T, Boolean> validityChecker;

    private final AtomicReference<T> reference;
    private final AtomicLong loanStartMs;
    private final Map<String, Command> command;

    // -------------------------------------------------------
    // Constructor and factories
    // -------------------------------------------------------

    /**
     * @param supplier        to supply the value to be loaned
     * @param loanDuration    the duration of the loan before the value is inaccessible
     * @param validityChecker checks if loaned value is valid (return true) or due for a refresh (return false) (overrides loan duration)
     * @param policy          controls the reloan, either "loan once if null" or "reloan if expired".
     */
    public AtomicLoan(Supplier<T> supplier, Duration loanDuration, Function<T, Boolean> validityChecker, Policy policy) {
        this.supplier = requireNonNull(supplier);
        this.loanDuration = requireNonNull(loanDuration);
        this.validityChecker = requireNonNull(validityChecker);

        this.reference = new AtomicReference<>(null);
        this.loanStartMs = new AtomicLong(0);
        this.command = new ConcurrentHashMap<>();
        this.policy = policy;
    }

    // -------------------------------------------------------
    // Factories for AtomicLoan: With supplier
    // -------------------------------------------------------

    /**
     * Create with supplier, never expire value.
     *
     * @param supplier the stored supplier
     * @param <T>      type of loaned value
     * @return AtomicLoan
     */
    public static <T> AtomicLoan<T> create(Supplier<T> supplier) {
        return new AtomicLoan<>(supplier, maximumLoanDuration, value -> true, Policy.ReloanIfExpired);
    }

    /**
     * Create with supplier and loan duration.
     *
     * @param supplier     the stored supplier
     * @param loanDuration the duration of the loan before the value is inaccessible
     * @param <T>          type of loaned value
     * @return AtomicLoan
     */
    public static <T> AtomicLoan<T> create(Supplier<T> supplier, Duration loanDuration) {
        return new AtomicLoan<>(supplier, loanDuration, value -> true, Policy.ReloanIfExpired);
    }

    /**
     * Create with supplier and validity checker.
     *
     * @param supplier        the stored supplier
     * @param validityChecker checks if loaned value is valid (return true) or due for a refresh (return false) (overrides loan duration)
     * @param <T>             type of loaned value
     * @return AtomicLoan
     */
    public static <T> AtomicLoan<T> create(Supplier<T> supplier, Function<T, Boolean> validityChecker) {
        return new AtomicLoan<>(supplier, maximumLoanDuration, validityChecker, Policy.ReloanIfExpired);
    }

    /**
     * Create with supplier, loan duration and validity checker.
     *
     * @param supplier        the stored supplier
     * @param loanDuration    the duration of the loan before the value is inaccessible
     * @param validityChecker checks if loaned value is valid (return true) or due for a refresh (return false) (overrides loan duration)
     * @param <T>             type of loaned value
     * @return AtomicLoan
     */
    public static <T> AtomicLoan<T> create(Supplier<T> supplier, Duration loanDuration, Function<T, Boolean> validityChecker) {
        return new AtomicLoan<>(supplier, loanDuration, validityChecker, Policy.ReloanIfExpired);
    }

    // -------------------------------------------------------
    // Factories for LoanFunction: Without supplier
    // -------------------------------------------------------

    /**
     * Create without supplier. Never expire value,
     *
     * @param <T> type of loaned value
     * @return LoanFunction
     */
    public static <T> LoanFunction<T> create() {
        return new AtomicLoan<>(() -> null, maximumLoanDuration, value -> true, Policy.ReloanIfExpired);
    }

    /**
     * Create without supplier. With loan duration.
     *
     * @param loanDuration the duration of the loan before the value is inaccessible
     * @param <T>          type of loaned value
     * @return LoanFunction
     */
    public static <T> LoanFunction<T> create(Duration loanDuration) {
        return new AtomicLoan<>(() -> null, loanDuration, value -> true, Policy.ReloanIfExpired);
    }

    /**
     * Create without supplier. With validity checker.
     *
     * @param validityChecker checks if loaned value is valid (return true) or due for a refresh (return false) (overrides loan duration)
     * @param <T>             type of loaned value
     * @return LoanFunction
     */
    public static <T> LoanFunction<T> create(Function<T, Boolean> validityChecker) {
        return new AtomicLoan<>(() -> null, maximumLoanDuration, validityChecker, Policy.ReloanIfExpired);
    }

    /**
     * Create without supplier. With loan duration and validity checker.
     *
     * @param loanDuration    the duration of the loan before the value is inaccessible
     * @param validityChecker checks if loaned value is valid (return true) or due for a refresh (return false) (overrides loan duration)
     * @param <T>             type of loaned value
     * @return LoanFunction
     */
    public static <T> LoanFunction<T> create(Duration loanDuration, Function<T, Boolean> validityChecker) {
        return new AtomicLoan<>(() -> null, loanDuration, validityChecker, Policy.ReloanIfExpired);
    }


    // -------------------------------------------------------
    // Public API
    // -------------------------------------------------------

    /**
     * @return loaned value as long as loan lifetime is not expired
     */
    @Override
    public Optional<T> read() {
        T loanedValue = reference.get();
        return isLoanExpired(loanedValue) ? Optional.empty() : Optional.of(loanedValue);
    }

    /**
     * @return loaned value or null regardless of lifetime and status
     */
    public Optional<T> readByForce() {
        return Optional.ofNullable(reference.get());
    }

    /**
     * Get the loaned value or, if loan expired or not loaned before, loans the value again
     *
     * @return loaned value
     * @throws IllegalStateException if loan returns null
     */
    @Override
    public T get() {
        return performLoanOfValue(Intention.GetNonExpired, supplier);
    }

    /**
     * Read loaned value if not expired and present, otherwise loan the value (again). The loanSupplier is called with current loaned value as input.
     *
     * @param loanSupplier with current loaned value as input
     * @return loaned value
     * @throws IllegalStateException if loan returns null
     */
    @Override
    public T get(Function<Optional<T>, T> loanSupplier) {
        return performLoanOfValue(Intention.GetNonExpired, () -> loanSupplier.apply(Optional.ofNullable(reference.get())));
    }

    /**
     * Loans the value again.
     *
     * @return loaned value
     * @throws IllegalStateException if loan returns null
     */
    public T refresh() {
        return performLoanOfValue(Intention.Refresh, supplier);
    }

    /**
     * Loan the value (again). The loanSupplier is called with current loaned value as input.
     *
     * @param loanSupplier with current loaned value as input
     * @return loaned value
     * @throws IllegalStateException if loan returns null
     */
    @Override
    public T refresh(Function<Optional<T>, T> loanSupplier) {
        return performLoanOfValue(Intention.Refresh, () -> loanSupplier.apply(Optional.ofNullable(reference.get())));
    }

    /**
     * Take out loaned value, regardless if expired or not, and leave no loaned value (null).
     *
     * @return loaned value or null
     */
    public T take() {
        return reference.getAndSet(null);
    }

    /**
     * Take out loaned value if expired.
     *
     * @return loaned value if expired, null if not expired (or null)
     */
    public T takeIfExpired() {
        T loanedValue = reference.get();
        return isLoanExpired(loanedValue)
                ? reference.compareAndSet(loanedValue, null) ? loanedValue : null
                : null;
    }

    /**
     * @return true if loaned value is null or expired.
     */
    public boolean isExpired() {
        return isLoanExpired(reference.get());
    }

    // -------------------------------------------------------
    // Private implementation
    // -------------------------------------------------------

    /**
     * @param intention is used to distinguish get and refresh
     * @param supplier  of the value to loan
     * @return the value
     * @throws IllegalStateException if loan is null
     */
    private T performLoanOfValue(Intention intention, Supplier<T> supplier) {
        Command cmd = command.computeIfAbsent(singleRefresher, state -> new Command());

        {
            // If the intention is to get a non expired value, then check if we already have a valid loan
            T loanedValue = reference.get();
            if (Objects.equals(intention, Intention.GetNonExpired)) {

                // Is current value not expired yet?
                if (!isLoanExpired(loanedValue)) {
                    return loanedValue;
                }
                // Is new loan acceptable?
                else if (!isLoanAcceptable(loanedValue)) {
                    if (loanedValue == null) {
                        throw new IllegalStateException("Loan expired, new loan not allowed");
                    }
                    return loanedValue;
                }
            }
            // Else if the intention is to refresh then we always do that
        }

        State previous = cmd.executionState.getAndSet(State.Executing);
        if (previous != State.Executing) {
            // This thread set executionState to State.Executing and has exclusive right to loan value
            return performLoanThenSignalAll(cmd, supplier);
        }

        // The reamining threads have to wait for the loan to be performed
        T value = waitForLoan(cmd);
        if (value == null) {
            throw new IllegalStateException("No value in supplier");
        } else if (Objects.equals(cmd.resultState.get(), ResultState.Failure)) {
            throw new IllegalStateException("Value was not retrieved");
        }

        return requireNonNull(value);
    }

    /**
     * @param loanedValue current loaned value
     * @return true if loan is expired
     */
    private boolean isLoanExpired(T loanedValue) {
        // if value null then default to expired
        return loanedValue == null

                // if never loaned (executed) then default to expired
                || loanStartMs.get() == 0

                // if time since loan > loan duration, then loan of value is expired
                || System.currentTimeMillis() - loanStartMs.get() > loanDuration.toMillis()

                // if validity checker returns false, then loan of value is expired
                || !validityChecker.apply(loanedValue);
    }

    /**
     * @param loanedValue current loaned value
     * @return true if loaning value is acceptable
     */
    private boolean isLoanAcceptable(T loanedValue) {
        return Objects.equals(this.policy, Policy.LoanOnceIfNull) && loanedValue == null

                // Auto reloan if current loan is expired
                || Objects.equals(this.policy, Policy.ReloanIfExpired) && isLoanExpired(loanedValue);
    }

    /**
     * Perform the loan by calling the supplier.get
     *
     * @param cmd      with state and sync primitives
     * @param supplier of the value to loan
     * @return loaned value never null
     * @throws IllegalStateException if loan value is null
     */
    private T performLoanThenSignalAll(Command cmd, Supplier<T> supplier) {
        T newValue;
        try {
            newValue = supplier.get();
            if (newValue == null) {
                throw new IllegalStateException("Null value returned from supplier");
            }

            reference.set(newValue);
            loanStartMs.set(System.currentTimeMillis());

            cmd.resultState.set(ResultState.Success);

        } catch (Throwable e) {
            cmd.resultState.set(ResultState.Failure);
            throw e;
        } finally {
            try {
                cmd.mutex.lock();
                cmd.condition.signalAll(); // Notify the loan waiters
            } finally {
                cmd.mutex.unlock();
            }
            command.remove(singleRefresher);
        }
        return newValue;
    }

    /**
     * Wait for the loan to be carried through
     *
     * @param cmd with state and sync primitives
     * @return loaned value (never null)
     * @throws IllegalStateException if loan value is null
     */
    private T waitForLoan(Command cmd) {
        if (Objects.equals(cmd.resultState.get(), ResultState.None)) {
            try {
                long startTime = System.currentTimeMillis();
                long waitTime = maxWaitTimeMsecs;

                cmd.mutex.lock();
                while (Objects.equals(cmd.resultState.get(), ResultState.None) && waitTime > 0) {
                    cmd.condition.await(waitTime, TimeUnit.MILLISECONDS);
                    waitTime = Math.max(0, maxWaitTimeMsecs - (System.currentTimeMillis() - startTime));
                }
            } catch (InterruptedException e) {
                Thread.interrupted();
                log.warn("{} interrupted while waiting for context to complete", this, e);
            } finally {
                cmd.mutex.unlock();
            }
        }
        return reference.get();
    }
}
