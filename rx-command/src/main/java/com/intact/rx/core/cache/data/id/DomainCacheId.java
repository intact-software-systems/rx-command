package com.intact.rx.core.cache.data.id;

import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class DomainCacheId {
    private static final DomainCacheId emptyId = new DomainCacheId("anemptydomaincacheid");

    private final Object id;

    public DomainCacheId(Object id) {
        this.id = requireNonNull(id);
    }

    public Object getId() {
        return id;
    }

    public boolean isValid() {
        return !this.equals(emptyId);
    }

    public static DomainCacheId empty() {
        return emptyId;
    }

    public static DomainCacheId create(Object id) {
        return new DomainCacheId(id);
    }

    public static DomainCacheId uuid() {
        return new DomainCacheId(UUID.randomUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof DomainCacheId) {
            DomainCacheId that = (DomainCacheId) o;
            return Objects.equals(id, that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    }
}
