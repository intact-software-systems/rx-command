package com.intact.rx.core.command.factory;

import java.lang.ref.WeakReference;

import com.intact.rx.api.RxContext;
import com.intact.rx.api.RxDefault;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.Action0;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.command.CommandControllerPolicy;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.machine.factory.RxThreadPoolFactory;
import com.intact.rx.policy.Lifetime;
import com.intact.rx.policy.ResourceLimits;
import com.intact.rx.templates.annotations.Nullable;

/**
 * Singleton factory that controls the creation of commands, controllers, etc.
 * It stores the command controllers in the cache, meanwhile the CommandMonitor monitors them.
 */
@SuppressWarnings("unused")
public final class CommandFactory {

    private static final CachePolicy cachePolicy =
            CachePolicy.create(
                    CacheMasterPolicy.validForever(),
                    DataCachePolicy.leastRecentlyUsedAnd(ResourceLimits.maxSamplesSoft(20000), Lifetime.ofMinutes(10))
            );

    private static final CacheHandle cacheHandle =
            CacheHandle.create(
                    RxDefault.getDefaultRxCommandDomainCacheId(),
                    MasterCacheId.create(CommandFactory.class),
                    CommandController.class
            );

    // ------------------------------------------
    // Access to monitor cache. Note: Caching weak references to allow faster garbage collection of controllers, and still have monitoring.
    // ------------------------------------------

    public static <T> DataCache<Object, WeakReference<CommandController<T>>> monitorCache() {
        return RxCacheAccess.defaultRxCacheFactory().computeDataCacheIfAbsent(cacheHandle, cachePolicy);
    }

    // ------------------------------------------
    // public functions
    // ------------------------------------------

    public static <T> CommandResult<T> executeActions(Strategy0<T>[] actions, @Nullable final RxObserver<T> observer, CommandControllerPolicy commandControllerPolicy, CommandPolicy commandPolicy) {
        final CommandController<T> controller = createAndEnrichCommandController(commandControllerPolicy, commandPolicy, observer, actions);
        return controller.subscribe();
    }


    public static <T> CommandResult<T> executeActions(final Action0<T>[] actions, @Nullable final RxObserver<T> observer, CommandControllerPolicy commandControllerPolicy, CommandPolicy commandPolicy) {
        final CommandController<T> controller = createAndEnrichCommandController(commandControllerPolicy, commandPolicy, observer, actions);
        return controller.subscribe();
    }

    // ----------------------------------------------------
    // Create CommandControllers
    // ----------------------------------------------------

    @SafeVarargs
    public static <T> CommandController<T> createController(CommandControllerPolicy commandControllerPolicy, CommandPolicy commandPolicy, Action0<T>... actions) {
        return createAndEnrichCommandController(commandControllerPolicy, commandPolicy, null, actions);
    }

    public static <T> CommandController<T> createController(CommandControllerPolicy policy) {
        return createCommandController(policy, null, null);
    }

    public static <T> CommandController<T> createController(CommandControllerPolicy commandControllerPolicy, RxObserver<T> observer) {
        return createCommandController(commandControllerPolicy, observer, null);
    }

    public static <T> CommandController<T> createController(CommandControllerPolicy commandControllerPolicy, RxThreadPool threadPool) {
        return createCommandController(commandControllerPolicy, null, threadPool);
    }

    public static <T> CommandController<T> createController(CommandControllerPolicy commandControllerPolicy, RxObserver<T> observer, RxThreadPool threadPool) {
        return createCommandController(commandControllerPolicy, observer, threadPool);
    }

    // ----------------------------------------------------
    // For void lambdas
    // ----------------------------------------------------

    public static Command<Void> createCommand(CommandPolicy policy, VoidStrategy0 action) {
        return CommandBuilder.<Void>withPolicy(policy).addAction(action).build();
    }

    public static Command<Void> createCommand(CommandPolicy policy, RxContext rxContext, VoidStrategy0 action) {
        return CommandBuilder.<Void>withPolicy(policy).addAction(rxContext, action).build();
    }

    public static Command<Void> createCommand(CommandPolicy policy, Strategy0<Boolean> executeCondition, VoidStrategy0 action) {
        return CommandBuilder.<Void>withPolicy(policy).addAction(executeCondition, action).build();
    }

    public static Command<Void> createCommand(CommandPolicy policy, RxContext rxContext, Strategy0<Boolean> executeCondition, VoidStrategy0 action) {
        return CommandBuilder.<Void>withPolicy(policy).addAction(rxContext, executeCondition, action).build();
    }

    // ----------------------------------------------------
    // For lambdas with return
    // ----------------------------------------------------

    public static <T> Command<T> createCommand(CommandPolicy policy, Strategy0<T> action) {
        return CommandBuilder.<T>withPolicy(policy).addAction(action).build();
    }

    public static <T> Command<T> createCommand(CommandPolicy policy, RxContext rxContext, Strategy0<T> action) {
        return CommandBuilder.<T>withPolicy(policy).addAction(rxContext, action).build();
    }

    public static <T> Command<T> createCommand(CommandPolicy policy, Strategy0<Boolean> executeCondition, Strategy0<T> action) {
        return CommandBuilder.<T>withPolicy(policy).addAction(executeCondition, action).build();
    }

    public static <T> Command<T> createCommand(CommandPolicy policy, RxContext rxContext, Strategy0<Boolean> executeCondition, Strategy0<T> action) {
        return CommandBuilder.<T>withPolicy(policy).addAction(rxContext, executeCondition, action).build();
    }

    public static <T> Command<T> createCommand(Action0<T> action, CommandPolicy policy) {
        return CommandBuilder.<T>withPolicy(policy).addAction(action).build();
    }

    // ----------------------------------------------------
    // private functions
    // ----------------------------------------------------

    private static <T> CommandController<T> createAndEnrichCommandController(CommandControllerPolicy commandControllerPolicy, CommandPolicy commandPolicy, RxObserver<T> observer, Strategy0<T>[] actions) {
        CommandController<T> controller = createCommandController(commandControllerPolicy, observer, null);
        controller.addCommand(CommandBuilder.<T>withPolicy(commandPolicy).addActions(actions).build());
        return controller;
    }

    private static <T> CommandController<T> createAndEnrichCommandController(CommandControllerPolicy commandControllerPolicy, CommandPolicy commandPolicy, RxObserver<T> observer, Action0<T>[] actions) {
        CommandController<T> controller = createCommandController(commandControllerPolicy, observer, null);
        controller.addCommand(CommandBuilder.<T>withPolicy(commandPolicy).addActions(actions).build());
        return controller;
    }

    // ------------------------------------------
    // private functions
    // ------------------------------------------

    private static <T> CommandController<T> createCommandController(CommandControllerPolicy commandControllerPolicy, @Nullable RxObserver<T> observer, @Nullable RxThreadPool threadPool) {

        final com.intact.rx.core.command.CommandController<T> controller =
                new com.intact.rx.core.command.CommandController<>(commandControllerPolicy, RxThreadPoolFactory.controllerPool());

        controller.setCommandThreadPool(threadPool != null ? threadPool : RxThreadPoolFactory.defaultCommandPool());

        if (observer != null) {
            controller.connect(observer);
        }

        CommandFactory.<T>monitorCache().write(controller.hashCode(), new WeakReference<>(controller));

        return controller;
    }

    private CommandFactory() {
    }
}
