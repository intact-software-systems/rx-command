package com.intact.rx.core.cache.data.id;

import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class MasterCacheId {

    private static final MasterCacheId emptyId = new MasterCacheId("anemptymastercacheid");

    private final Object id;

    public MasterCacheId(Object id) {
        this.id = requireNonNull(id);
    }

    public Object getId() {
        return id;
    }

    public boolean isValid() {
        return !this.equals(emptyId);
    }

    public static MasterCacheId empty() {
        return emptyId;
    }

    public static MasterCacheId create(Object id) {
        return new MasterCacheId(id);
    }

    public static MasterCacheId uuid() {
        return new MasterCacheId(UUID.randomUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof MasterCacheId) {
            MasterCacheId that = (MasterCacheId) o;
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
