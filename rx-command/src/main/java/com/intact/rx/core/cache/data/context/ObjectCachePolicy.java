package com.intact.rx.core.cache.data.context;

import static java.util.Objects.requireNonNull;

import com.intact.rx.policy.MementoPolicy;
import com.intact.rx.policy.ResourceLimits;

public class ObjectCachePolicy {
    private final ObjectRootPolicy rootPolicy;
    private final ResourceLimits resourceLimits;
    private final MementoPolicy mementoPolicy;

    public ObjectCachePolicy(ObjectRootPolicy policy, ResourceLimits resourceLimits, MementoPolicy mementoPolicy) {
        this.rootPolicy = requireNonNull(policy);
        this.resourceLimits = requireNonNull(resourceLimits);
        this.mementoPolicy = requireNonNull(mementoPolicy);
    }

    public ObjectRootPolicy getRootPolicy() {
        return rootPolicy;
    }

    public ResourceLimits getResourceLimits() {
        return resourceLimits;
    }

    public MementoPolicy getMementoPolicy() {
        return mementoPolicy;
    }

    public boolean isMemento() {
        return mementoPolicy.isAnyDepth();
    }

    @Override
    public String toString() {
        return "ObjectCachePolicy{" +
                "rootPolicy=" + rootPolicy +
                ", resourceLimits=" + resourceLimits +
                '}';
    }
}
