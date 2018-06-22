package com.intact.rx.core.cache.data.context;

import java.util.Set;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.command.Strategy2;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.strategy.CacheCleanupAlgorithms;
import com.intact.rx.policy.Extension;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.MementoPolicy;
import com.intact.rx.policy.ResourceLimits;

public class DataCachePolicy {
    public static final Strategy2<Boolean, DataCachePolicy, DataCache<?, ?>> DEFAULT_CACHE_CLEANUP = CacheCleanupAlgorithms::defaultCacheCleanup;
    public static final Strategy2<Set<?>, DataCache<?, ?>, Long> REMOVE_N_LEAST_RECENTLY_USED = CacheCleanupAlgorithms::removeNLeastRecentlyUsed;
    public static final Strategy2<Set<?>, DataCache<?, ?>, Long> REMOVE_N_LEAST_FREQUENTLY_USED = CacheCleanupAlgorithms::removeNLeastFrequentlyUsed;

    private final Lifetime lifetime;
    private final ResourceLimits resourceLimits;
    private final Extension extension;
    private final MementoPolicy mementoPolicy;
    private final ObjectRootPolicy objectRootPolicy;
    private final Strategy2<Set<?>, DataCache<?, ?>, Long> evictionStrategy;
    private final Strategy2<Boolean, DataCachePolicy, DataCache<?, ?>> cleanupStrategy;

    public DataCachePolicy(
            Lifetime lifetime,
            ResourceLimits resourceLimits,
            Extension extension,
            MementoPolicy mementoPolicy,
            ObjectRootPolicy objectRootPolicy,
            Strategy2<Set<?>, DataCache<?, ?>, Long> evictionStrategy) {
        this.lifetime = requireNonNull(lifetime);
        this.resourceLimits = requireNonNull(resourceLimits);
        this.extension = requireNonNull(extension);
        this.mementoPolicy = requireNonNull(mementoPolicy);
        this.objectRootPolicy = requireNonNull(objectRootPolicy);
        this.evictionStrategy = requireNonNull(evictionStrategy);
        this.cleanupStrategy = DEFAULT_CACHE_CLEANUP;
    }

    public Lifetime getLifetime() {
        return lifetime;
    }

    public ResourceLimits getResourceLimits() {
        return resourceLimits;
    }

    public Extension getExtension() {
        return extension;
    }

    public MementoPolicy getMementoPolicy() {
        return mementoPolicy;
    }

    public boolean isMemento() {
        return mementoPolicy.isAnyDepth();
    }

    public ObjectRootPolicy getObjectRootPolicy() {
        return objectRootPolicy;
    }

    public Strategy2<Set<?>, DataCache<?, ?>, Long> getEvictionStrategy() {
        return evictionStrategy;
    }

    public Strategy2<Boolean, DataCachePolicy, DataCache<?, ?>> getCleanupStrategy() {
        return cleanupStrategy;
    }

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static DataCachePolicy leastRecentlyUsedAnd(ResourceLimits resourceLimits, Lifetime lifetime) {
        return new DataCachePolicy(lifetime, resourceLimits, Extension.noRenew(), MementoPolicy.none, ObjectRootPolicy.create(lifetime, Extension.noRenew()), REMOVE_N_LEAST_RECENTLY_USED);
    }

    public static DataCachePolicy leastRecentlyUsedAnd(ResourceLimits resourceLimits, Lifetime lifetime, MementoPolicy mementoPolicy) {
        return new DataCachePolicy(lifetime, resourceLimits, Extension.noRenew(), mementoPolicy, ObjectRootPolicy.create(lifetime, Extension.noRenew()), REMOVE_N_LEAST_RECENTLY_USED);
    }

    public static DataCachePolicy leastRecentlyUsedAnd(ResourceLimits resourceLimits, Lifetime lifetime, ObjectRootPolicy objectRootPolicy) {
        return new DataCachePolicy(lifetime, resourceLimits, Extension.noRenew(), MementoPolicy.none, objectRootPolicy, REMOVE_N_LEAST_RECENTLY_USED);
    }

    public static DataCachePolicy leastRecentlyUsedAnd(ResourceLimits resourceLimits, Lifetime lifetime, MementoPolicy mementoPolicy, ObjectRootPolicy objectRootPolicy) {
        return new DataCachePolicy(lifetime, resourceLimits, Extension.noRenew(), mementoPolicy, objectRootPolicy, REMOVE_N_LEAST_RECENTLY_USED);
    }

    public static DataCachePolicy leastRecentlyUsedAnd(ResourceLimits resourceLimits, Lifetime lifetime, Extension extension, ObjectRootPolicy objectRootPolicy) {
        return new DataCachePolicy(lifetime, resourceLimits, extension, MementoPolicy.none, objectRootPolicy, REMOVE_N_LEAST_RECENTLY_USED);
    }

    public static DataCachePolicy leastFrequentlyUsedAnd(ResourceLimits resourceLimits, Lifetime lifetime, MementoPolicy mementoPolicy) {
        return new DataCachePolicy(lifetime, resourceLimits, Extension.noRenew(), mementoPolicy, ObjectRootPolicy.create(lifetime, Extension.noRenew()), REMOVE_N_LEAST_FREQUENTLY_USED);
    }

    public static DataCachePolicy unlimitedForever() {
        return new DataCachePolicy(Lifetime.forever(), ResourceLimits.unlimited(), Extension.noRenew(), MementoPolicy.none, ObjectRootPolicy.foreverNoRenew(), REMOVE_N_LEAST_RECENTLY_USED);
    }

    @Override
    public String toString() {
        return "DataCachePolicy{" +
                "lifetime=" + lifetime +
                ", resourceLimits=" + resourceLimits +
                ", extension=" + extension +
                ", mementoPolicy=" + mementoPolicy +
                ", objectRootPolicy=" + objectRootPolicy +
                ", evictionStrategy=" + evictionStrategy +
                ", cleanupStrategy=" + cleanupStrategy +
                '}';
    }
}
