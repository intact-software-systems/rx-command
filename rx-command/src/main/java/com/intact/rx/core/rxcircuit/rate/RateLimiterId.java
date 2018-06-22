package com.intact.rx.core.rxcircuit.rate;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.core.cache.data.id.MasterCacheId;

@SuppressWarnings("WeakerAccess")
public final class RateLimiterId {
    private final ValueHandle<Object> handle;

    public RateLimiterId(ValueHandle<Object> handle) {
        this.handle = requireNonNull(handle);
    }

    public boolean isNone() {
        return this.equals(Builder.noRateLimiterId);
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

    public static Builder buildFrom(RateLimiterId rateLimiterId) {
        return new Builder(requireNonNull(rateLimiterId));
    }

    public static class Builder {
        public static final RateLimiterId noRateLimiterId = new RateLimiterId(ValueHandle.create(CacheHandle.uuid(), "no-rateLimiter-id"));

        private static final String SINGLETON = "singleton-ratelimiter-id";
        private static final RateLimiterId singletonRateLimiterId = new RateLimiterId(ValueHandle.create(CacheHandle.voidHandle(), SINGLETON));

        private CacheHandle cacheHandle;
        private Object rateLimiterId;

        public Builder() {
            this.cacheHandle = RxDefault.getGlobalRateLimiterCacheHandle();
            this.rateLimiterId = SINGLETON;
        }

        public Builder(RateLimiterId rateLimiterId) {
            requireNonNull(rateLimiterId);
            this.cacheHandle = rateLimiterId.getHandle().getCacheHandle();
            this.rateLimiterId = rateLimiterId.getHandle().getKey();
        }

        public Builder withRateLimiterScope(CacheHandle cacheHandle) {
            this.cacheHandle = requireNonNull(cacheHandle);
            return this;
        }

        public Builder withIsolatedScope() {
            this.cacheHandle = CacheHandle.uuid();
            return this;
        }

        public Builder withRateLimiterId(Object rateLimiterId) {
            this.rateLimiterId = requireNonNull(rateLimiterId);
            return this;
        }

        public RateLimiterId build() {
            return new RateLimiterId(ValueHandle.create(cacheHandle, rateLimiterId));
        }
    }

    public static RateLimiterId none() {
        return Builder.noRateLimiterId;
    }

    public static RateLimiterId singleton() {
        return Builder.singletonRateLimiterId;
    }

    public static RateLimiterId uuid() {
        return new RateLimiterId(ValueHandle.create(CacheHandle.uuid(), Builder.SINGLETON));
    }

    public static <T> RateLimiterId sharedByType(Class<T> requestorType) {
        return new RateLimiterId(ValueHandle.create(RxDefault.getGlobalRateLimiterCacheHandle(), requestorType));
    }

    public static RateLimiterId sharedById(Object rateLimiterId) {
        return new RateLimiterId(ValueHandle.create(RxDefault.getGlobalRateLimiterCacheHandle(), rateLimiterId));
    }

    public static <T> RateLimiterId isolated(Class<T> requestorType) {
        return new RateLimiterId(ValueHandle.create(CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), MasterCacheId.uuid(), RateLimiterPolicy.class), requestorType));
    }

    public static RateLimiterId isolated(Object rateLimiterId) {
        return new RateLimiterId(ValueHandle.create(CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), MasterCacheId.uuid(), RateLimiterPolicy.class), rateLimiterId));
    }

    @Override
    public String toString() {
        return "RateLimiterId{" +
                "handle=" + handle +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RateLimiterId rateLimiterId = (RateLimiterId) o;

        return Objects.equals(handle, rateLimiterId.handle);
    }

    @Override
    public int hashCode() {
        return handle != null ? handle.hashCode() : 0;
    }
}
