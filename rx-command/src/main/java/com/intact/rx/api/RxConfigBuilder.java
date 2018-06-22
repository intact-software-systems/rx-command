package com.intact.rx.api;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.factory.DataCachePolicyBuilder;
import com.intact.rx.core.command.CommandConfig;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.machine.RxThreadPoolId;
import com.intact.rx.core.machine.context.RxThreadPoolConfig;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.controller.ActsControllerConfig;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.policy.*;
import com.intact.rx.templates.RxContexts;

import static com.intact.rx.templates.Validate.assertTrue;

public abstract class RxConfigBuilder<Builder> {

    @SuppressWarnings({"PublicField", "WeakerAccess"})
    protected class State {
        public ActPolicy actPolicy = RxDefault.getDefaultActPolicy();

        public final CommandConfig.Builder commandConfigBuilder = CommandConfig.builder().withCommandPolicy(RxDefault.getDefaultCommandPolicy());

        public RxThreadPoolConfig rxThreadPoolConfig = RxDefault.getDefaultCommandPoolConfig();
        public DomainCacheId domainCacheId = RxDefault.getDefaultDomainCacheId();
        public Duration clientTimeout = RxDefault.getDefaultClientTimeout();

        public final ActsControllerConfig.Builder actsControllerConfigBuilder = ActsControllerConfig.builder();

        public final RxContexts.Builder rxContextsBuilder = RxContexts.create().withRxContext(RxDefault.getDefaultRxContext());
        public CommandPolicy commandPolicyForWrite = RxDefault.getDefaultWriteCommandPolicy();

        public FaultPolicy faultPolicy = RxDefault.getDefaultFaultPolicy();
        public DataCachePolicyBuilder dataCachePolicyBuilder = DataCachePolicyBuilder.from(RxDefault.getDefaultCachePolicy().getDataCachePolicy());

        public final Builder builder;

        public Builder update(RxConfig rxConfig) {
            actPolicy = rxConfig.actPolicy;
            commandConfigBuilder.withConfig(rxConfig.commandConfig);
            rxThreadPoolConfig = rxConfig.rxThreadPoolConfig;
            domainCacheId = rxConfig.domainCacheId;
            clientTimeout = rxConfig.clientTimeout;
            actsControllerConfigBuilder.withConfig(rxConfig.actsControllerConfig);
            rxContextsBuilder.withRxContext(rxConfig.rxContext);
            commandPolicyForWrite = rxConfig.writeCommandConfig.getCommandPolicy();
            faultPolicy = rxConfig.faultPolicy;
            dataCachePolicyBuilder = DataCachePolicyBuilder.from(rxConfig.actPolicy.getCachePolicy().getDataCachePolicy());
            return builder;
        }

        public State(Builder builder) {
            this.builder = builder;
        }
    }

    protected abstract State state();

    protected RxConfig buildRxConfig() {
        return new RxConfig(
                ActPolicy.from(state().actPolicy, CachePolicy.create(state().actPolicy.getCachePolicy().getCacheMasterPolicy(), state().dataCachePolicyBuilder.build())),
                state().commandConfigBuilder.build(),
                state().rxThreadPoolConfig,
                state().clientTimeout,
                state().actsControllerConfigBuilder.build(),
                state().rxContextsBuilder.build(),
                state().commandConfigBuilder.withCommandPolicy(state().commandPolicyForWrite).build(),
                state().faultPolicy,
                state().domainCacheId);
    }

    public Builder withRxConfig(RxConfig rxConfig) {
        return state().update(rxConfig);
    }

    public Builder withControllerConfig(ActsControllerConfig actsControllerConfig) {
        state().actsControllerConfigBuilder.withConfig(actsControllerConfig);
        return state().builder;
    }

    public Builder withControllerCircuitBreakerId(CircuitId circuitId) {
        state().actsControllerConfigBuilder.withCircuitBreakerId(requireNonNull(circuitId));
        return state().builder;
    }

    public Builder withControllerRateLimiterId(RateLimiterId circuitId) {
        state().actsControllerConfigBuilder.withRateLimiterId(requireNonNull(circuitId));
        return state().builder;
    }

    public Builder withExecuteAround(RxContext rxContext) {
        state().rxContextsBuilder.withRxContext(rxContext);
        return state().builder;
    }

    public Builder withExecuteBefore(VoidStrategy0 before) {
        state().rxContextsBuilder.withBefore(before);
        return state().builder;
    }

    public Builder withExecuteAfter(VoidStrategy0 after) {
        state().rxContextsBuilder.withAfter(after);
        return state().builder;
    }

    public Builder withThreadPoolConfig(String threadPoolKey, int threadPoolSize) {
        assertTrue(threadPoolSize > 0);

        state().rxThreadPoolConfig = RxThreadPoolConfig.create(MaxLimit.withLimit(threadPoolSize), RxThreadPoolId.create(threadPoolKey));
        return state().builder;
    }

    public Builder withThreadPoolConfig(RxThreadPoolConfig commandThreadPoolPolicy) {
        state().rxThreadPoolConfig = requireNonNull(commandThreadPoolPolicy);
        return state().builder;
    }

    public Builder withActPolicy(ActPolicy actPolicy) {
        state().actPolicy = requireNonNull(actPolicy);
        return state().builder;
    }

    public Builder withCommandPolicyForWrite(CommandPolicy commandPolicyForWrite) {
        state().commandPolicyForWrite = requireNonNull(commandPolicyForWrite);
        return state().builder;
    }

    public Builder withClientTimeout(Duration clientTimeout) {
        state().clientTimeout = requireNonNull(clientTimeout);
        return state().builder;
    }

    public Builder withCommandConfig(CommandConfig commandConfig) {
        state().commandConfigBuilder.withConfig(commandConfig);
        return state().builder;
    }

    public Builder withCommandPolicy(CommandPolicy commandPolicy) {
        state().commandConfigBuilder.withCommandPolicy(commandPolicy);
        return state().builder;
    }

    public Builder withCommandAttempt(Attempt attempt) {
        state().commandConfigBuilder.withAttempt(attempt);
        return state().builder;
    }

    public Builder withCommandTimeout(Timeout timeout) {
        state().commandConfigBuilder.withTimeout(timeout);
        return state().builder;
    }

    public Builder withCommandInterval(Interval interval) {
        state().commandConfigBuilder.withInterval(interval);
        return state().builder;
    }

    public Builder withCommandRetryInterval(Interval retryInterval) {
        state().commandConfigBuilder.withRetryInterval(retryInterval);
        return state().builder;
    }

    public Builder withCommandCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
        state().commandConfigBuilder.withCircuitBreakerPolicy(circuitBreakerPolicy);
        return state().builder;
    }

    public Builder withCommandRateLimiterId(RateLimiterId rateLimiterId) {
        state().commandConfigBuilder.withRateLimiterId(rateLimiterId);
        return state().builder;
    }

    public Builder withCommandCircuitBreakerId(CircuitId circuitId) {
        state().commandConfigBuilder.withCircuitBreakerId(circuitId);
        return state().builder;
    }

    public Builder withCommandRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy) {
        state().commandConfigBuilder.withRateLimiterPolicy(rateLimiterPolicy);
        return state().builder;
    }

    public Builder withCommandSuccessCriterion(Criterion successCriterion) {
        state().commandConfigBuilder.withSuccessCriterion(successCriterion);
        return state().builder;
    }

    public Builder withCacheResourceLimits(ResourceLimits resourceLimits) {
        state().dataCachePolicyBuilder.withResourceLimits(resourceLimits);
        return state().builder;
    }

    public Builder withCacheMementoPolicy(MementoPolicy mementoPolicy) {
        state().dataCachePolicyBuilder.withDataCacheMementoPolicy(mementoPolicy);
        return state().builder;
    }

    public Builder withCachedObjectLifetime(Lifetime lifetime) {
        state().dataCachePolicyBuilder.withObjectLifetime(lifetime);
        return state().builder;
    }

    public Builder withCachedObjectMementoPolicy(MementoPolicy mementoPolicy) {
        state().dataCachePolicyBuilder.withObjectMementoPolicy(mementoPolicy);
        return state().builder;
    }

    public Builder withCachedObjectExtension(Extension extension) {
        state().dataCachePolicyBuilder.withObjectExtension(extension);
        return state().builder;
    }

    public Builder withFaultPolicy(FaultPolicy faultPolicy) {
        state().faultPolicy = requireNonNull(faultPolicy);
        return state().builder;
    }

    public Builder withDomainCacheId(DomainCacheId domainCacheId) {
        state().domainCacheId = requireNonNull(domainCacheId);
        return state().builder;
    }
}
