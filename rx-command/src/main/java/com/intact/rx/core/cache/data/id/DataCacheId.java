package com.intact.rx.core.cache.data.id;

import java.util.Objects;
import java.util.UUID;

public class DataCacheId {
    private static final DataCacheId dataCacheId = new DataCacheId(DataCacheId.class, MasterCacheId.empty());

    private final Typename id;
    private final MasterCacheId masterCacheId;

    private DataCacheId(final Class<?> aClass, final MasterCacheId ownedBy) {
        this.id = Typename.create(Objects.requireNonNull(aClass));
        this.masterCacheId = Objects.requireNonNull(ownedBy);
    }

    private DataCacheId(final Typename id, final MasterCacheId ownedBy) {
        this.id = Objects.requireNonNull(id);
        this.masterCacheId = Objects.requireNonNull(ownedBy);
    }

    public Typename getId() {
        return id;
    }

    public MasterCacheId getOwner() {
        return masterCacheId;
    }

    public static DataCacheId create(final Class<?> aClass, final MasterCacheId masterCacheId) {
        return new DataCacheId(Typename.create(aClass), masterCacheId);
    }

    public static DataCacheId uuid(final MasterCacheId masterCacheId) {
        return new DataCacheId(Typename.create(UUID.randomUUID().toString()), masterCacheId);
    }

    public static DataCacheId empty() {
        return dataCacheId;
    }

    @Override
    public String toString() {
        return "DataCacheId{" +
                "id='" + id + '\'' +
                ", masterCacheId=" + masterCacheId +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataCacheId that = (DataCacheId) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(masterCacheId, that.masterCacheId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, masterCacheId);
    }
}
