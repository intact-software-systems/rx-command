package com.intact.rx.api.cache;

import java.util.Set;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxConfigManager;
import com.intact.rx.api.RxConfigSingletonManager;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;

import static com.intact.rx.api.RxDefault.getDefaultDomainCacheId;

public class RxSetData<T> {
    private final MasterCacheId masterCacheId;
    private final DomainCacheId domainCacheId;

    private RxConfigManager policyManager = RxConfigSingletonManager.instance;
    private CachePolicy cachePolicy;

    private RxSetData(DomainCacheId domainCacheId, MasterCacheId masterCacheId) {
        this.masterCacheId = requireNonNull(masterCacheId);
        this.domainCacheId = requireNonNull(domainCacheId);
        this.cachePolicy = null;
    }

    public static <T> RxSetData<T> uuid() {
        return new RxSetData<>(getDefaultDomainCacheId(), MasterCacheId.uuid());
    }

    public static <T> RxSetData<T> in(MasterCacheId masterCacheId) {
        return new RxSetData<>(getDefaultDomainCacheId(), masterCacheId);
    }

    public static <T> RxSetData<T> in(Object masterCacheId) {
        return new RxSetData<>(getDefaultDomainCacheId(), new MasterCacheId(masterCacheId));
    }

    public static <T> RxSetData<T> in(DomainCacheId domainCacheId, MasterCacheId masterCacheId) {
        return new RxSetData<>(domainCacheId, masterCacheId);
    }

    public RxSetData<T> withPoliciesIn(MasterCacheId masterCacheId) {
        policyManager = RxConfigManager.forSystem(requireNonNull(masterCacheId));
        return this;
    }

    public RxSetData<T> withPolicyManager(RxConfigManager policyManager) {
        this.policyManager = requireNonNull(policyManager);
        return this;
    }

    public RxSetData<T> withDefaultRxConfig(RxConfig rxConfig) {
        requireNonNull(rxConfig);
        if (this.policyManager == null) {
            this.policyManager = RxConfigManager.forSystem(MasterCacheId.uuid()).setDefaultRxConfig(rxConfig);
        } else {
            this.policyManager.setDefaultRxConfig(rxConfig);
        }
        return this;
    }

    public RxSetData<T> withCachePolicy(CachePolicy cachePolicy) {
        this.cachePolicy = requireNonNull(cachePolicy);
        return this;
    }

    public RxConfigManager policyManager() {
        return policyManager;
    }

    public MasterCacheId cacheId() {
        return masterCacheId;
    }

    public DomainCacheId domainCacheId() {
        return domainCacheId;
    }

    public void clear() {
        RxCacheAccess.clearCache(domainCacheId, masterCacheId);
    }

    public void expire(Class<T> cachedType) {
        requireNonNull(cachedType);
        RxCacheAccess.expireDataCache(CacheHandle.create(domainCacheId, masterCacheId, cachedType));
    }

    public Set<T> toSet(Class<T> cachedType) {
        return RxCollections.asSet(set(cachedType));
    }

    public RxSet<T> set(Class<T> cachedType) {
        requireNonNull(cachedType);
        return RxCacheAccess.set(
                CacheHandle.create(domainCacheId, masterCacheId, cachedType),
                cachePolicy == null ? policyManager.getRxConfigFor(cachedType).actPolicy.getCachePolicy() : cachePolicy
        );
    }
}
