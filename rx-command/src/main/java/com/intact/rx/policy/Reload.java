package com.intact.rx.policy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

@SuppressWarnings("unused")
public final class Reload {

    public enum Kind {
        NO,
        ON_DELETE,
        ON_INVALIDATE,
        ON_MODIFY,
        ON_CREATE
    }

    @SuppressWarnings("SetReplaceableByEnumSet")
    private final Collection<Kind> kinds = new HashSet<>();

    public Reload(Kind kind) {
        this.kinds.add(kind);
    }

    public Reload(Collection<Kind> kinds) {
        this.kinds.addAll(kinds);
    }

    public Collection<Kind> getKinds() {
        return Collections.unmodifiableCollection(kinds);
    }

    // --------------------------------------------
    // Convenience methods
    // --------------------------------------------

    public boolean isNo() {
        return kinds.contains(Kind.NO);
    }

    public boolean isOnDelete() {
        return kinds.contains(Kind.ON_DELETE);
    }

    public boolean isOnInvalidate() {
        return kinds.contains(Kind.ON_INVALIDATE);
    }

    public boolean isOnModify() {
        return kinds.contains(Kind.ON_MODIFY);
    }

    public boolean isOnCreate() {
        return kinds.contains(Kind.ON_CREATE);
    }

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static Reload no() {
        return new Reload(Kind.NO);
    }

    public static Reload onDelete() {
        return new Reload(Kind.ON_DELETE);
    }

    public static Reload onModify() {
        return new Reload(Kind.ON_MODIFY);
    }

    public static Reload onCreate() {
        return new Reload(Kind.ON_CREATE);
    }

    public static Reload onInvalidate() {
        return new Reload(Kind.ON_INVALIDATE);
    }

    @Override
    public String toString() {
        return "Reload{" +
                "kinds=" + kinds +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reload reload = (Reload) o;
        return Objects.equals(kinds, reload.kinds);
    }

    @Override
    public int hashCode() {
        return kinds.hashCode();
    }
}
