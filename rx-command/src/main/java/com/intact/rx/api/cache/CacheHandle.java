package com.intact.rx.api.cache;

import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;

public class CacheHandle {
    private static final CacheHandle voidHandle;
    private static final DomainCacheId uuidDomain = DomainCacheId.uuid();

    static {
        MasterCacheId voidUuid = MasterCacheId.uuid();
        voidHandle = new CacheHandle(new DomainCacheId("VoidDomain"), voidUuid, DataCacheId.create(Void.class, voidUuid));
    }

    public static CacheHandle voidHandle() {
        return voidHandle;
    }

    private final DomainCacheId domainCacheId;
    private final MasterCacheId masterCacheId;
    private final DataCacheId dataCacheId;

    private CacheHandle(DomainCacheId domainCacheId, MasterCacheId masterCacheId, DataCacheId dataCacheId) {
        this.domainCacheId = requireNonNull(domainCacheId);
        this.masterCacheId = requireNonNull(masterCacheId);
        this.dataCacheId = requireNonNull(dataCacheId);
    }

    public static CacheHandle uuid() {
        MasterCacheId uuid = MasterCacheId.uuid();
        return new CacheHandle(uuidDomain, uuid, DataCacheId.uuid(uuid));
    }

    public static CacheHandle create(DomainCacheId domainCacheId, DataCacheId dataCacheId) {
        return new CacheHandle(domainCacheId, requireNonNull(dataCacheId.getOwner()), dataCacheId);
    }

    public static <V> CacheHandle create(DomainCacheId domainCacheId, MasterCacheId masterCacheId, Class<V> cachedType) {
        return new CacheHandle(domainCacheId, masterCacheId, DataCacheId.create(cachedType, masterCacheId));
    }

    public static <V> Optional<Class<V>> findCachedType(CacheHandle cacheHandle) {
        return cacheHandle.getDataCacheId().getId().getTypedClazz();
    }

    public DomainCacheId getDomainCacheId() {
        return domainCacheId;
    }

    public MasterCacheId getMasterCacheId() {
        return masterCacheId;
    }

    public DataCacheId getDataCacheId() {
        return dataCacheId;
    }

    @Override
    public String toString() {
        return "CacheHandle{" +
                "domainCacheId=" + domainCacheId +
                ", masterCacheId=" + masterCacheId +
                ", dataCacheId=" + dataCacheId +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheHandle)) return false;
        CacheHandle that = (CacheHandle) o;
        return Objects.equals(domainCacheId, that.domainCacheId) &&
                Objects.equals(masterCacheId, that.masterCacheId) &&
                Objects.equals(dataCacheId, that.dataCacheId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domainCacheId, masterCacheId, dataCacheId);
    }
}
