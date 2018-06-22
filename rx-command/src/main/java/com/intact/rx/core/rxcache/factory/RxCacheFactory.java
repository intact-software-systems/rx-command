package com.intact.rx.core.rxcache.factory;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.Action0;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.Commands;
import com.intact.rx.core.machine.context.RxThreadPoolConfig;
import com.intact.rx.core.machine.factory.RxThreadPoolFactory;
import com.intact.rx.core.rxcache.act.ActCommands;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.core.rxcache.acts.ActGroupChain;
import com.intact.rx.core.rxcache.api.Act;
import com.intact.rx.core.rxcache.api.ActsController;
import com.intact.rx.core.rxcache.controller.ActsCommandsController;
import com.intact.rx.core.rxcache.controller.ActsControllerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.ResourceLimits;
import com.intact.rx.templates.Validate;

public final class RxCacheFactory {

    private static final CachePolicy monitorCachePolicy = CachePolicy.create(
            CacheMasterPolicy.validForever(),
            DataCachePolicy.leastRecentlyUsedAnd(
                    ResourceLimits.maxSamplesSoft(20000),
                    Lifetime.ofMinutes(10)
            )
    );

    private static final CacheHandle actsControllerHandle = CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), MasterCacheId.create(RxCacheFactory.class), ActsController.class);
    private static final CacheHandle actHandle = CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), MasterCacheId.create(RxCacheFactory.class), Act.class);

    public static <K, V> DataCache<Object, WeakReference<Act<K, V>>> actMonitorCache() {
        return RxCacheAccess.defaultRxCacheFactory().computeDataCacheIfAbsent(actHandle, monitorCachePolicy);
    }

    public static DataCache<Object, WeakReference<ActsController>> actsControllerMonitorCache() {
        return RxCacheAccess.defaultRxCacheFactory().computeDataCacheIfAbsent(actsControllerHandle, monitorCachePolicy);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static ActsCommandsController createActsController(ActsControllerPolicy policy, ActGroupChain chain, CircuitId circuitBreakerId, RateLimiterId rateLimiterId, Optional<RxObserver<ActGroup>> observer) {
        requireNonNull(policy);
        requireNonNull(chain);
        requireNonNull(circuitBreakerId);
        requireNonNull(rateLimiterId);
        requireNonNull(observer);

        ActsCommandsController controller = new ActsCommandsController(policy, RxThreadPoolFactory.controllerPool(), chain, circuitBreakerId, rateLimiterId);
        observer.map(controller::connect);

        actsControllerMonitorCache().write(controller.hashCode(), new WeakReference<>(controller));

        return controller;
    }

    @SafeVarargs
    public static <K, V> Act<K, V> createAct(CacheHandle cacheHandle, CircuitId circuitBreakerId, RateLimiterId rateLimiterId, RxThreadPoolConfig commandThreadPool, ActPolicy actPolicy, CommandPolicy commandPolicy, Action0<Map<K, V>>... actions) {
        requireNonNull(cacheHandle);
        requireNonNull(circuitBreakerId);
        requireNonNull(rateLimiterId);
        requireNonNull(commandThreadPool);
        requireNonNull(actPolicy);
        requireNonNull(commandPolicy);
        Validate.requireNoNullElements(actions);

        Act<K, V> act = new ActCommands<>(
                cacheHandle,
                actPolicy,
                RxThreadPoolFactory.computeIfAbsent(commandThreadPool),
                Commands.actionsAsCommands(commandPolicy, circuitBreakerId, rateLimiterId, actions)
        );

        RxCacheFactory.<K, V>actMonitorCache().write(act.hashCode(), new WeakReference<>(act));

        return act;
    }

    private RxCacheFactory() {
    }
}
