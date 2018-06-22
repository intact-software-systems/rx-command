package com.intact.rx.core.cache.data.context;

import static java.util.Objects.requireNonNull;

import com.intact.rx.policy.Extension;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.MementoPolicy;

public class ObjectRootPolicy {
    private static final ObjectRootPolicy foreverNoRenew = new ObjectRootPolicy(Lifetime.forever(), Extension.noRenew(), MementoPolicy.none);

    private final Lifetime lifetime;
    private final Extension extension;
    private final MementoPolicy mementoPolicy;

    private ObjectRootPolicy(Lifetime lifetime, Extension extension, MementoPolicy mementoPolicy) {
        this.lifetime = requireNonNull(lifetime);
        this.extension = requireNonNull(extension);
        this.mementoPolicy = requireNonNull(mementoPolicy);
    }

    public Lifetime getLifetime() {
        return lifetime;
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

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static ObjectRootPolicy create(Lifetime lifetime, Extension extension, MementoPolicy mementoPolicy) {
        return new ObjectRootPolicy(lifetime, extension, mementoPolicy);
    }

    public static ObjectRootPolicy create(Lifetime lifetime, Extension extension) {
        return new ObjectRootPolicy(lifetime, extension, MementoPolicy.none);
    }

    public static ObjectRootPolicy create(Lifetime lifetime) {
        return new ObjectRootPolicy(lifetime, Extension.noRenew(), MementoPolicy.none);
    }

    public static ObjectRootPolicy foreverNoRenew() {
        return foreverNoRenew;
    }

    @Override
    public String toString() {
        return "ObjectRootPolicy{" +
                "lifetime=" + lifetime +
                ", extension=" + extension +
                '}';
    }
}
