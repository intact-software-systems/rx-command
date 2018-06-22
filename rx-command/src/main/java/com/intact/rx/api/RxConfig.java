package com.intact.rx.api;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.command.CommandConfig;
import com.intact.rx.core.machine.context.RxThreadPoolConfig;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.controller.ActsControllerConfig;
import com.intact.rx.policy.FaultPolicy;

@SuppressWarnings({"WeakerAccess", "PublicField"})
public class RxConfig {
    public final ActPolicy actPolicy;
    public final CommandConfig commandConfig;
    public final RxThreadPoolConfig rxThreadPoolConfig;
    public final DomainCacheId domainCacheId;
    public final Duration clientTimeout;
    public final ActsControllerConfig actsControllerConfig;
    public final RxContext rxContext;
    public final CommandConfig writeCommandConfig;
    public final FaultPolicy faultPolicy;

    public RxConfig(ActPolicy actPolicy, CommandConfig commandConfig, RxThreadPoolConfig rxThreadPoolConfig, Duration clientTimeout, ActsControllerConfig actsControllerConfig, RxContext rxContext, CommandConfig writeCommandConfig, FaultPolicy faultPolicy, DomainCacheId domainCacheId) {
        this.actPolicy = requireNonNull(actPolicy);
        this.commandConfig = requireNonNull(commandConfig);
        this.rxThreadPoolConfig = requireNonNull(rxThreadPoolConfig);
        this.clientTimeout = requireNonNull(clientTimeout);
        this.actsControllerConfig = requireNonNull(actsControllerConfig);
        this.rxContext = requireNonNull(rxContext);
        this.writeCommandConfig = requireNonNull(writeCommandConfig);
        this.faultPolicy = requireNonNull(faultPolicy);
        this.domainCacheId = requireNonNull(domainCacheId);
    }

    public static RxConfig fromRxDefault() {
        return new RxConfig(
                RxDefault.getDefaultActPolicy(),
                CommandConfig.create(RxDefault.getDefaultCommandPolicy(), RxDefault.getDefaultCircuitBreakerId(), RxDefault.getDefaultRateLimiterId()),
                RxDefault.getDefaultCommandPoolConfig(),
                RxDefault.getDefaultClientTimeout(),
                ActsControllerConfig.create(RxDefault.getDefaultActsControllerPolicy(), RxDefault.getDefaultCircuitBreakerId(), RxDefault.getDefaultRateLimiterId()),
                RxDefault.getDefaultRxContext(),
                CommandConfig.create(RxDefault.getDefaultWriteCommandPolicy(), RxDefault.getDefaultCircuitBreakerId(), RxDefault.getDefaultRateLimiterId()),
                RxDefault.getDefaultFaultPolicy(),
                RxDefault.getDefaultDomainCacheId());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends RxConfigBuilder<Builder> {
        private final State state;

        public Builder() {
            //noinspection ThisEscapedInObjectConstruction
            state = new State(this);
        }

        public RxConfig build() {
            return buildRxConfig();
        }

        @Override
        protected State state() {
            return state;
        }
    }

    @Override
    public String toString() {
        return "RxConfig{" +
                "actPolicy=" + actPolicy +
                ", commandConfig=" + commandConfig +
                ", rxThreadPoolConfig=" + rxThreadPoolConfig +
                ", domainCacheId=" + domainCacheId +
                ", clientTimeout=" + clientTimeout +
                ", actsControllerConfig=" + actsControllerConfig +
                ", rxContextsBuilder=" + rxContext +
                ", writeCommandConfig=" + writeCommandConfig +
                ", faultPolicy=" + faultPolicy +
                '}';
    }
}
