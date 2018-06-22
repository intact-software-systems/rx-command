package com.intact.rx.core.cache.factory;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.context.ObjectRootPolicy;
import com.intact.rx.policy.Extension;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.MementoPolicy;
import com.intact.rx.policy.ResourceLimits;

public class DataCachePolicyBuilder {
    private ResourceLimits resourceLimits;
    private MementoPolicy dataCacheMementoPolicy;
    private ObjectRootPolicy objectRootPolicy;

    private DataCachePolicyBuilder(DataCachePolicy policy) {
        requireNonNull(policy);

        this.resourceLimits = policy.getResourceLimits();
        this.dataCacheMementoPolicy = policy.getMementoPolicy();
        this.objectRootPolicy = policy.getObjectRootPolicy();
    }

    public static DataCachePolicyBuilder from(DataCachePolicy cachePolicy) {
        return new DataCachePolicyBuilder(cachePolicy);
    }

    public DataCachePolicyBuilder withResourceLimits(ResourceLimits resourceLimits) {
        this.resourceLimits = requireNonNull(resourceLimits);
        return this;
    }

    public DataCachePolicyBuilder withDataCacheMementoPolicy(MementoPolicy mementoPolicy) {
        this.dataCacheMementoPolicy = requireNonNull(mementoPolicy);
        return this;
    }

    public DataCachePolicyBuilder withObjectLifetime(Lifetime lifetime) {
        this.objectRootPolicy = ObjectRootPolicy.create(lifetime, objectRootPolicy.getExtension(), objectRootPolicy.getMementoPolicy());
        return this;
    }

    public DataCachePolicyBuilder withObjectMementoPolicy(MementoPolicy mementoPolicy) {
        this.objectRootPolicy = ObjectRootPolicy.create(objectRootPolicy.getLifetime(), objectRootPolicy.getExtension(), mementoPolicy);
        return this;
    }

    public DataCachePolicyBuilder withObjectExtension(Extension extension) {
        this.objectRootPolicy = ObjectRootPolicy.create(objectRootPolicy.getLifetime(), extension, objectRootPolicy.getMementoPolicy());
        return this;
    }

    public DataCachePolicy build() {
        return DataCachePolicy.leastRecentlyUsedAnd(resourceLimits, Lifetime.forever(), dataCacheMementoPolicy, objectRootPolicy);
    }
}
