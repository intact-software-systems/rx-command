package com.intact.rx.core.cache.strategy;

import com.intact.rx.core.cache.data.ObjectRoot;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.policy.*;

public final class CachePolicyChecker {

    private CachePolicyChecker() {
    }

    public static boolean isInLifetime(AccessStatus status, Lifetime lifetime) {
        return !status.isExpired() &&
                status.getTime().getTimeSinceStarted() <= lifetime.inMillis();
    }

    public static boolean isInRefreshInterval(AccessStatus status, Interval refreshInterval) {
        return !status.isExpired() &&
                status.getTime().getTimeSinceModified() >= refreshInterval.getPeriodMs();
    }

    private static boolean isWritingPossible(int totalNumOfObjects, ResourceLimits resourceLimits) {
        return !(resourceLimits.isHard() && totalNumOfObjects >= resourceLimits.getMaxSamples());
    }

    public static boolean isInactive(AccessStatus status, Timeout timeout) {
        return status.getTime().getTimeSinceModified() > timeout.toMillis();
    }

    public static void assertWritingPossible(int currentSize, int toBeAdded, ResourceLimits resourceLimits) {
        if (!isWritingPossible(currentSize + toBeAdded, resourceLimits)) {
            throw new IllegalStateException("Cache configured with " + resourceLimits + " resource limit. Cannot add " + toBeAdded + " new values. Current cache size " + currentSize);
        }
    }

    public static <K, V> void assertLoanReservationPossible(ObjectRoot<K, V> root, Reservation reservation) {
        if (root == null || root.isExpired()) {
            return;
        }

        if (!root.getLoanStatus().isAcceptableReservation(reservation)) {
            throw new IllegalStateException("Cannot loan with stricter reservation type " + reservation + " than existing " + root.getLoanStatus());
        }
    }

    public static <K, V> void assertOverwritePossible(ObjectRoot<K, V> root) {
        if (root == null || root.isExpired()) {
            return;
        }

        if (root.isLoaned() && root.isImmutable()) {
            throw new IllegalStateException("Cannot overwrite immutable currently loaned value for key " + root.getKey() + ". Loan: " + root.getLoanStatus());
        }
    }

    public static <K, V> void assertRemovalPossible(ObjectRoot<K, V> root) {
        if (root == null || root.isExpired()) {
            return;
        }

        if (root.isLoaned()) {
            throw new IllegalStateException("Cannot remove currently loaned value for key " + root.getKey() + ". Loan: " + root.getLoanStatus());
        }
    }
}
