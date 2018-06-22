package com.intact.rx.policy;

import com.intact.rx.templates.Validate;

public final class Attempt {
    private static final Attempt attemptOnce = new Attempt(Kind.UNTIL_SUCCESS, 1, 1);
    private static final Attempt attemptForever = new Attempt(Kind.FOREVER, Long.MAX_VALUE, Long.MAX_VALUE);
    private static final Attempt retryUntilSuccess = new Attempt(Kind.UNTIL_SUCCESS, 1, Long.MAX_VALUE);

    public enum Kind {
        UNTIL_SUCCESS,
        NUM_SUCCESSFUL_TIMES,
        FOREVER
    }

    private final Kind kind;
    private final long numSuccessfulTimes;
    private final long maxNumAttempts;

    public Attempt(Kind kind, long numSuccessfulTimes, long maxNumAttempts) {
        Validate.assertTrue(numSuccessfulTimes > 0);
        Validate.assertTrue(maxNumAttempts >= numSuccessfulTimes);

        this.kind = kind;
        this.numSuccessfulTimes = numSuccessfulTimes;
        this.maxNumAttempts = maxNumAttempts;
    }

    public Kind getKind() {
        return kind;
    }

    public long getMaxNumAttempts() {
        return maxNumAttempts;
    }

    public long getMinNumSuccesses() {
        return numSuccessfulTimes;
    }

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static Attempt once() {
        return attemptOnce;
    }

    public static Attempt retry(long retry) {
        return new Attempt(Kind.UNTIL_SUCCESS, 1, retry + 1);
    }

    public static Attempt retryUntilSuccess() {
        return retryUntilSuccess;
    }

    public static Attempt numSuccessfulTimes(long numSuccessfulTimes, long maxNumAttempts) {
        return new Attempt(Kind.NUM_SUCCESSFUL_TIMES, numSuccessfulTimes, maxNumAttempts);
    }

    public static Attempt forever() {
        return attemptForever;
    }

    @Override
    public String toString() {
        return "Attempt{" +
                "kind=" + kind +
                ", numSuccessfulTimes=" + numSuccessfulTimes +
                ", maxNumAttempts=" + maxNumAttempts +
                '}';
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Attempt)) {
            return false;
        }

        Attempt attempt = (Attempt) o;

        if (maxNumAttempts != attempt.maxNumAttempts) {
            return false;
        }
        if (numSuccessfulTimes != attempt.numSuccessfulTimes) {
            return false;
        }
        return kind == attempt.kind;

    }

    @Override
    public int hashCode() {
        int result = kind != null ? kind.hashCode() : 0;
        result = 31 * result + (int) (numSuccessfulTimes ^ numSuccessfulTimes >>> 32);
        result = 31 * result + (int) (maxNumAttempts ^ maxNumAttempts >>> 32);
        return result;
    }
}
