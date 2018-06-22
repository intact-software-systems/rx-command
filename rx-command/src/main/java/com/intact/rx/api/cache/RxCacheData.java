package com.intact.rx.api.cache;

import java.util.Map;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxConfigManager;
import com.intact.rx.api.RxConfigSingletonManager;
import com.intact.rx.core.cache.CacheMap;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;

import static com.intact.rx.api.RxDefault.getDefaultDomainCacheId;

@SuppressWarnings({"ProtectedField", "WeakerAccess"})
public class RxCacheData {
    private final DomainCacheId domainCacheId;
    private final MasterCacheId masterCacheId;

    private RxConfigManager policyManager = RxConfigSingletonManager.instance;
    private CachePolicy cachePolicy;

    RxCacheData(DomainCacheId domainCacheId, MasterCacheId masterCacheId) {
        this.domainCacheId = requireNonNull(domainCacheId);
        this.masterCacheId = requireNonNull(masterCacheId);
        this.cachePolicy = null;
    }

    public static RxCacheData uuid() {
        return new RxCacheData(getDefaultDomainCacheId(), MasterCacheId.uuid());
    }

    public static RxCacheData in(MasterCacheId masterCacheId) {
        return new RxCacheData(getDefaultDomainCacheId(), masterCacheId);
    }

    public static RxCacheData in(Object masterCacheId) {
        return new RxCacheData(getDefaultDomainCacheId(), new MasterCacheId(masterCacheId));
    }

    public static RxCacheData in(DomainCacheId domainCacheId, MasterCacheId masterCacheId) {
        return new RxCacheData(domainCacheId, masterCacheId);
    }

    public RxCacheData withPoliciesIn(MasterCacheId masterCacheId) {
        this.policyManager = RxConfigManager.forSystem(requireNonNull(masterCacheId));
        return this;
    }

    public RxCacheData withPolicyManager(RxConfigManager policyManager) {
        this.policyManager = requireNonNull(policyManager);
        return this;
    }

    public RxCacheData withDefaultRxConfig(RxConfig rxConfig) {
        requireNonNull(rxConfig);
        if (this.policyManager == null) {
            this.policyManager = RxConfigManager.forSystem(MasterCacheId.uuid()).setDefaultRxConfig(rxConfig);
        } else {
            this.policyManager.setDefaultRxConfig(rxConfig);
        }
        return this;
    }

    public RxCacheData withCachePolicy(CachePolicy cachePolicy) {
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

    public <V> void expire(Class<V> cachedType) {
        requireNonNull(cachedType);
        RxCacheAccess.expireDataCache(CacheHandle.create(domainCacheId, masterCacheId, cachedType));
    }

    public <K, V> Reader<K, V> reader(Class<V> cachedType) {
        return cache(cachedType);
    }

    public <K, V> Writer<K, V> writer(Class<V> cachedType) {
        return cache(cachedType);
    }

    public <K, V> Map<K, V> toMap(Class<V> cachedType) {
        return new CacheMap<>(cache(cachedType));
    }

    public <K, V> RxCache<K, V> cache(Class<V> cachedType) {
        requireNonNull(cachedType);
        return RxCacheAccess.cache(
                CacheHandle.create(domainCacheId, masterCacheId, cachedType),
                cachePolicy == null ? policyManager.getRxConfigFor(cachedType).actPolicy.getCachePolicy() : cachePolicy
        );
    }
}
