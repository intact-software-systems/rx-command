package com.intact.rx.policy;

import java.util.Objects;

import com.intact.rx.templates.Validate;

public final class ResourceLimits {
    private static final ResourceLimits unlimited = new ResourceLimits(Long.MAX_VALUE, Type.Soft);

    public enum Type {
        Hard,
        Soft,
    }

    private final long maxSamples;
    private final Type type;

    public ResourceLimits(long maxSamples, Type type) {
        Validate.assertTrue(maxSamples > 0);

        this.maxSamples = maxSamples;
        this.type = type;
    }

    public static ResourceLimits maxSamplesSoft(long maxSamples) {
        return new ResourceLimits(maxSamples, Type.Soft);
    }

    public static ResourceLimits maxSamplesHard(long maxSamples) {
        return new ResourceLimits(maxSamples, Type.Hard);
    }

    public static ResourceLimits unlimited() {
        return unlimited;
    }

    public long getMaxSamples() {
        return maxSamples;
    }

    public boolean isHard() {
        return Objects.equals(type, Type.Hard);
    }

    @Override
    public String toString() {
        return "ResourceLimits{" +
                "maxSamples=" + maxSamples +
                ", type=" + type +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceLimits)) return false;
        ResourceLimits that = (ResourceLimits) o;
        return maxSamples == that.maxSamples &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxSamples, type);
    }
}
