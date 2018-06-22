package com.intact.rx.policy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

/**
 * Automatic renew lifetime of cached objects upon types of updates.
 * <p>
 * Is Kind logical in an extension? Other kinds? Periodic, indefinite, fixed, subscription?
 */
@SuppressWarnings("SetReplaceableByEnumSet")
public final class Extension {
    private static final Extension noRenew = new Extension(Kind.NO_RENEW);
    private static final Extension renewOnAccess = new Extension(Kind.RENEW_ON_ACCESS);
    private static final Extension renewOnWrite = new Extension(Kind.RENEW_ON_WRITE);
    private static final Extension renewOnRead = new Extension(Kind.RENEW_ON_READ);

    public enum Kind {
        RENEW_ON_ACCESS,
        RENEW_ON_WRITE,
        RENEW_ON_READ,
        NO_RENEW
    }

    private final Collection<Kind> kinds = new HashSet<>();

    public Extension(Kind kind) {
        this.kinds.add(kind);
    }

    public Extension(Collection<Kind> kinds) {
        this.kinds.addAll(kinds);
    }

    public Collection<Kind> getKinds() {
        return Collections.unmodifiableCollection(kinds);
    }

    // --------------------------------------------
    // Convenience methods
    // --------------------------------------------

    public boolean isRenewOnAccess() {
        return kinds.contains(Kind.RENEW_ON_ACCESS);
    }

    public boolean isRenewOnWrite() {
        return kinds.contains(Kind.RENEW_ON_WRITE);
    }

    public boolean isRenewOnRead() {
        return kinds.contains(Kind.RENEW_ON_READ);
    }

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static Extension noRenew() {
        return noRenew;
    }

    public static Extension renewOnAccess() {
        return renewOnAccess;
    }

    public static Extension renewOnWrite() {
        return renewOnWrite;
    }

    public static Extension renewOnRead() {
        return renewOnRead;
    }

    @Override
    public String toString() {
        return "Extension{" +
                "kinds=" + kinds +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Extension)) return false;
        Extension extension = (Extension) o;
        return Objects.equals(kinds, extension.kinds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kinds);
    }
}
