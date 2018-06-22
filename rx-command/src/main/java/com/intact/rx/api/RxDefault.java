package com.intact.rx.api;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.machine.RxThreadPoolId;
import com.intact.rx.core.machine.context.RxThreadPoolConfig;
import com.intact.rx.core.machine.factory.RxThreadFactory;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.controller.ActsControllerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.core.rxrepo.RepositoryConfig;
import com.intact.rx.policy.*;

/**
 * Main HUB for default configurations in rx. They are used throughout rx when no user-defined input.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class RxDefault {

    // -------------------------------------------------------
    // Default policy and configuration instances
    // -------------------------------------------------------

    private static final AtomicReference<CachePolicy> defaultCachePolicy = new AtomicReference<>(CachePolicy.create(CacheMasterPolicy.validForever(), DataCachePolicy.leastRecentlyUsedAnd(ResourceLimits.unlimited(), Lifetime.ofHours(2), MementoPolicy.none)));

    private static final AtomicReference<ActPolicy> defaultActPolicy = new AtomicReference<>(ActPolicy.noReload(defaultCachePolicy.get()));

    private static final AtomicReference<CommandPolicy> defaultCommandPolicy = new AtomicReference<>(CommandPolicy.runOnce(Interval.nowThenOfSeconds(2), Timeout.ofMillis(70000), 0));

    private static final AtomicReference<CommandPolicy> defaultWriteCommandPolicy = new AtomicReference<>(CommandPolicy.runOnceNow());

    private static final AtomicReference<CachePolicy> defaultRequestCachePolicy = new AtomicReference<>(CachePolicy.unlimited(Lifetime.ofMinutes(60)));
    private static final AtomicReference<Duration> defaultClientTimeout = new AtomicReference<>(Duration.ofSeconds(60));

    private static final AtomicReference<ActsControllerPolicy> defaultActsControllerPolicy = new AtomicReference<>(ActsControllerPolicy.oneTimePolicy);

    /**
     * Policy on reaction when request fails and API returns optional.
     */
    private static final AtomicReference<FaultPolicy> defaultFaultPolicy = new AtomicReference<>(FaultPolicy.neverThrowWhenOptional());

    /**
     * RxContext used to decorate (before, after) calls to user provided lambdas/actions/commands.
     */
    private static final AtomicReference<RxContext> defaultRxContext = new AtomicReference<>(RxContextNoOp.instance());

    /**
     * Default circuit breaker id
     */
    private static final AtomicReference<CircuitId> defaultCircuitBreakerId = new AtomicReference<>(CircuitId.none());

    /**
     * Default rate limiter id
     */
    private static final AtomicReference<RateLimiterId> defaultRateLimiterId = new AtomicReference<>(RateLimiterId.none());

    /**
     * Timeout to decide when to remove inactive caches that are empty.
     */
    private static final AtomicReference<Timeout> cacheInactiveTimeout = new AtomicReference<>(Timeout.ofMinutes(30));

    /**
     * Default thread pool policy/config used by user-defined commands.
     */
    private static final AtomicReference<RxThreadPoolConfig> defaultCommandPoolConfig =
            new AtomicReference<>(RxThreadPoolConfig.create(MaxLimit.withLimit(50), RxThreadFactory.daemonWithName("rx-command "), RxThreadPoolId.create("rx-command")));

    /**
     * Default thread pool policy/config used by controllers.
     */
    private static final AtomicReference<RxThreadPoolConfig> controllerPoolPolicy =
            new AtomicReference<>(RxThreadPoolConfig.create(MaxLimit.withLimit(5), RxThreadFactory.daemonWithName("rx-controller "), RxThreadPoolId.create("rx-controller")));


    /**
     * Default thread pool policy/config used by monitors
     */
    private static final AtomicReference<RxThreadPoolConfig> monitorPoolPolicy =
            new AtomicReference<>(RxThreadPoolConfig.create(MaxLimit.withLimit(5), RxThreadFactory.daemonWithName("rx-monitor "), RxThreadPoolId.create("rx-monitor")));

    /**
     * Default domain cache id
     */
    private static final AtomicReference<DomainCacheId> defaultDomainCacheId = new AtomicReference<>(new DomainCacheId("RxCommand.DefaultDomainCacheId"));

    /**
     * Used for internal RxCommand data
     */
    private static final AtomicReference<DomainCacheId> defaultRxCommandDomainCacheId = new AtomicReference<>(new DomainCacheId("RxCommand.DefaultRxCommandCacheId"));

    /**
     * Immutable circuit cache handles for circuit breaker cache
     */
    private static final CacheHandle globalCircuitCacheHandle = CacheHandle.create(getDefaultRxCommandDomainCacheId(), MasterCacheId.create("Rx.globalCircuitBreakerScope"), CircuitBreakerPolicy.class);
    private static final CacheHandle globalRateLimiterCacheHandle = CacheHandle.create(getDefaultRxCommandDomainCacheId(), MasterCacheId.create("Rx.globalCircuitBreakerScope"), RateLimiterPolicy.class);
    private static final CacheHandle repositoryCircuitCacheHandle = CacheHandle.create(getDefaultRxCommandDomainCacheId(), MasterCacheId.create("Rx.repositoryCircuitBreakerScope"), RepositoryConfig.class);

    // -------------------------------------------------------
    // Get default policies.
    // -------------------------------------------------------

    public static CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy.get();
    }

    public static CommandPolicy getDefaultCommandPolicy() {
        return defaultCommandPolicy.get();
    }

    public static ActPolicy getDefaultActPolicy() {
        return defaultActPolicy.get();
    }

    public static CommandPolicy getDefaultWriteCommandPolicy() {
        return defaultWriteCommandPolicy.get();
    }

    public static CachePolicy getDefaultRequestCachePolicy() {
        return defaultRequestCachePolicy.get();
    }

    public static Duration getDefaultClientTimeout() {
        return defaultClientTimeout.get();
    }

    public static ActsControllerPolicy getDefaultActsControllerPolicy() {
        return defaultActsControllerPolicy.get();
    }

    public static FaultPolicy getDefaultFaultPolicy() {
        return defaultFaultPolicy.get();
    }

    public static RxContext getDefaultRxContext() {
        return defaultRxContext.get();
    }

    public static CircuitId getDefaultCircuitBreakerId() {
        return defaultCircuitBreakerId.get();
    }

    public static RateLimiterId getDefaultRateLimiterId() {
        return defaultRateLimiterId.get();
    }

    public static Timeout getCacheInactiveTimeout() {
        return cacheInactiveTimeout.get();
    }

    public static RxThreadPoolConfig getDefaultCommandPoolConfig() {
        return defaultCommandPoolConfig.get();
    }

    public static RxThreadPoolConfig getControllerPoolPolicy() {
        return controllerPoolPolicy.get();
    }

    public static RxThreadPoolConfig getMonitorPoolPolicy() {
        return monitorPoolPolicy.get();
    }

    public static DomainCacheId getDefaultDomainCacheId() {
        return defaultDomainCacheId.get();
    }

    public static DomainCacheId getDefaultRxCommandDomainCacheId() {
        return defaultRxCommandDomainCacheId.get();
    }

    public static CacheHandle getActCircuitCacheHandle() {
        return globalCircuitCacheHandle;
    }

    public static CacheHandle getGlobalRateLimiterCacheHandle() {
        return globalRateLimiterCacheHandle;
    }

    public static CacheHandle getRepositoryCircuitCacheHandle() {
        return repositoryCircuitCacheHandle;
    }

    // -------------------------------------------------------
    // Setters update default policies.
    // -------------------------------------------------------

    public static void setDefaultWriteCommandPolicy(CommandPolicy defaultWriteCommandPolicy) {
        RxDefault.defaultWriteCommandPolicy.set(requireNonNull(defaultWriteCommandPolicy));
    }

    public static void setDefaultRequestCachePolicy(CachePolicy defaultRequestCachePolicy) {
        RxDefault.defaultRequestCachePolicy.set(requireNonNull(defaultRequestCachePolicy));
    }

    public static void setDefaultClientTimeout(Duration defaultClientTimeout) {
        RxDefault.defaultClientTimeout.set(requireNonNull(defaultClientTimeout));
    }

    public static void setDefaultActsControllerPolicy(ActsControllerPolicy defaultActsControllerPolicy) {
        RxDefault.defaultActsControllerPolicy.set(requireNonNull(defaultActsControllerPolicy));
    }

    public static void setDefaultFaultPolicy(FaultPolicy defaultFaultPolicy) {
        RxDefault.defaultFaultPolicy.set(requireNonNull(defaultFaultPolicy));
    }

    public static void setDefaultRxContext(RxContext defaultRxContext) {
        RxDefault.defaultRxContext.set(requireNonNull(defaultRxContext));
    }

    public static void setDefaultCircuitBreakerId(CircuitId defaultCircuitBreakerId) {
        RxDefault.defaultCircuitBreakerId.set(requireNonNull(defaultCircuitBreakerId));
    }

    public static void setDefaultRateLimiterId(RateLimiterId defaultRateLimiterId) {
        RxDefault.defaultRateLimiterId.set(requireNonNull(defaultRateLimiterId));
    }

    public static void setCacheInactiveTimeout(Timeout cacheInactiveTimeout) {
        RxDefault.cacheInactiveTimeout.set(requireNonNull(cacheInactiveTimeout));
    }

    public static void setDefaultCommandPoolConfig(RxThreadPoolConfig defaultCommandPoolConfig) {
        RxDefault.defaultCommandPoolConfig.set(requireNonNull(defaultCommandPoolConfig));
    }

    public static void setControllerPoolPolicy(RxThreadPoolConfig controllerPoolPolicy) {
        RxDefault.controllerPoolPolicy.set(requireNonNull(controllerPoolPolicy));
    }

    public static void setMonitorPoolPolicy(RxThreadPoolConfig monitorPoolPolicy) {
        RxDefault.monitorPoolPolicy.set(requireNonNull(monitorPoolPolicy));
    }

    public static void setDefaultDomainCacheId(DomainCacheId defaultDomainCacheId) {
        RxDefault.defaultDomainCacheId.set(requireNonNull(defaultDomainCacheId));
    }

    public static void setDefaultRxCommandDomainCacheId(DomainCacheId defaultRxCommandDomainCacheId) {
        RxDefault.defaultRxCommandDomainCacheId.set(requireNonNull(defaultRxCommandDomainCacheId));
    }

    private RxDefault() {
    }

}
