package com.intact.rx.core.rxcircuit.breaker;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.core.cache.data.id.MasterCacheId;

@SuppressWarnings("WeakerAccess")
public final class CircuitId {
    private final ValueHandle<Object> handle;

    public CircuitId(ValueHandle<Object> handle) {
        this.handle = requireNonNull(handle);
    }

    public boolean isNone() {
        return this.equals(Builder.noCircuitId);
    }

    public ValueHandle<Object> getHandle() {
        return handle;
    }

    public CacheHandle getCacheHandle() {
        return handle.getCacheHandle();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder buildFrom(CircuitId circuitId) {
        return new Builder(requireNonNull(circuitId));
    }

    public static class Builder {
        public static final CircuitId noCircuitId = new CircuitId(ValueHandle.create(CacheHandle.uuid(), "no-circuit-id"));

        private static final String SINGLETON = "singleton";
        private static final CircuitId singletonCircuitId = new CircuitId(ValueHandle.create(CacheHandle.voidHandle(), SINGLETON));

        private CacheHandle cacheHandle;
        private Object circuitBreakerId;

        public Builder() {
            this.cacheHandle = RxDefault.getActCircuitCacheHandle();
            this.circuitBreakerId = SINGLETON;
        }

        public Builder(CircuitId circuitId) {
            requireNonNull(circuitId);
            this.cacheHandle = circuitId.getHandle().getCacheHandle();
            this.circuitBreakerId = circuitId.getHandle().getKey();
        }

        public Builder withCircuitScope(CacheHandle cacheHandle) {
            this.cacheHandle = requireNonNull(cacheHandle);
            return this;
        }

        public Builder withIsolatedScope() {
            this.cacheHandle = CacheHandle.uuid();
            return this;
        }

        public Builder withCircuitBreakerId(Object circuitBreakerId) {
            this.circuitBreakerId = requireNonNull(circuitBreakerId);
            return this;
        }

        public CircuitId build() {
            return new CircuitId(ValueHandle.create(cacheHandle, circuitBreakerId));
        }
    }

    public static CircuitId none() {
        return Builder.noCircuitId;
    }

    public static CircuitId singleton() {
        return Builder.singletonCircuitId;
    }

    public static CircuitId uuid() {
        return new CircuitId(ValueHandle.create(CacheHandle.uuid(), Builder.SINGLETON));
    }

    public static <T> CircuitId sharedByType(Class<T> requestorType) {
        return new CircuitId(ValueHandle.create(RxDefault.getActCircuitCacheHandle(), requestorType));
    }

    public static CircuitId sharedById(Object circuitId) {
        return new CircuitId(ValueHandle.create(RxDefault.getActCircuitCacheHandle(), circuitId));
    }

    public static <T> CircuitId isolated(Class<T> requestorType) {
        return new CircuitId(ValueHandle.create(CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), MasterCacheId.uuid(), CircuitBreakerPolicy.class), requestorType));
    }

    public static CircuitId isolated(Object circuitId) {
        return new CircuitId(ValueHandle.create(CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), MasterCacheId.uuid(), CircuitBreakerPolicy.class), circuitId));
    }

    @Override
    public String toString() {
        return "CircuitId{" +
                "handle=" + handle +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CircuitId circuitId = (CircuitId) o;

        return Objects.equals(handle, circuitId.handle);
    }

    @Override
    public int hashCode() {
        return handle != null ? handle.hashCode() : 0;
    }
}
