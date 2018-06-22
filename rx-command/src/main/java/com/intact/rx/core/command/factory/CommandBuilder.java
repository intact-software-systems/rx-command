package com.intact.rx.core.command.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxContext;
import com.intact.rx.api.RxDefault;
import com.intact.rx.api.command.Action0;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.core.command.Command;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.action.*;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.templates.Validate;

@SuppressWarnings("WeakerAccess")
public class CommandBuilder<T> {
    private final List<Action0<T>> actions = new ArrayList<>();
    private final CommandPolicyBuilder commandPolicy;
    private final AtomicReference<CircuitId> circuitBreakerId = new AtomicReference<>(RxDefault.getDefaultCircuitBreakerId());
    private final AtomicReference<RateLimiterId> rateLimiterId = new AtomicReference<>(RxDefault.getDefaultRateLimiterId());

    private CommandBuilder(CommandPolicy commandPolicy) {
        this.commandPolicy = CommandPolicyBuilder.from(commandPolicy);
    }

    // --------------------------------------------
    // Builder with fluent API
    // --------------------------------------------

    public static <V> CommandBuilder<V> withPolicy(CommandPolicy commandPolicy) {
        return new CommandBuilder<>(commandPolicy);
    }

    public static <V> CommandBuilder<V> fromDefault() {
        return new CommandBuilder<>(RxDefault.getDefaultCommandPolicy());
    }

    public CommandBuilder<T> withCircuitBreakerId(CircuitId circuitId) {
        this.circuitBreakerId.set(requireNonNull(circuitId));
        return this;
    }

    public CommandBuilder<T> withRateLimiterId(RateLimiterId rateLimiterId) {
        this.rateLimiterId.set(requireNonNull(rateLimiterId));
        return this;
    }

    public Command<T> build() {
        Validate.assertTrue(!actions.isEmpty());
        return new Command<>(commandPolicy.build(), circuitBreakerId.get(), rateLimiterId.get(), actions);
    }

    @SafeVarargs
    public final CommandBuilder<T> addActions(Action0<T>... actions) {
        for (Action0<T> action : actions) {
            addActionPrivate(action);
        }
        return this;
    }

    @SafeVarargs
    public final CommandBuilder<T> addActions(Strategy0<T>... actions) {
        for (Strategy0<T> action : actions) {
            addActionPrivate(new StrategyAction0<>(action));
        }
        return this;
    }

    // ----------------------------------------------------
    // For void lambdas
    // ----------------------------------------------------

    public CommandBuilder<T> addAction(VoidStrategy0 strategy) {
        return addActionPrivate(
                new VoidAction<>(
                        () -> {
                            strategy.perform();
                            return null;
                        }
                )
        );
    }

    public CommandBuilder<T> addAction(RxContext rxContext, VoidStrategy0 strategy) {
        return addActionPrivate(
                new VoidAction<>(
                        rxContext,
                        () -> {
                            strategy.perform();
                            return null;
                        }
                )
        );
    }

    public CommandBuilder<T> addAction(Strategy0<Boolean> executeCondition, VoidStrategy0 action) {
        return addActionPrivate(
                new VoidAction<>(
                        () -> {
                            if (executeCondition.perform()) {
                                action.perform();
                            }
                            return null;
                        }
                ));
    }

    public CommandBuilder<T> addAction(RxContext rxContext, Strategy0<Boolean> executeCondition, VoidStrategy0 action) {
        return addActionPrivate(
                new VoidAction<>(
                        rxContext,
                        () -> {
                            if (executeCondition.perform()) {
                                action.perform();
                            }
                            return null;
                        }
                ));
    }

    // ----------------------------------------------------
    // For lambdas with return
    // ----------------------------------------------------

    public CommandBuilder<T> addAction(Strategy0<T> strategy) {
        return addActionPrivate(new StrategyAction0<>(strategy));
    }

    public CommandBuilder<T> addAction(RxContext rxContext, Strategy0<T> strategy) {
        return addActionPrivate(new ContextStrategyAction0<>(rxContext, strategy));
    }

    public CommandBuilder<T> addAction(Strategy0<Boolean> executeCondition, Strategy0<T> action) {
        return addActionPrivate(
                new StrategyAction0<>(
                        () -> executeCondition.perform()
                                ? action.perform()
                                : null
                ));
    }

    public CommandBuilder<T> addAction(RxContext rxContext, Strategy0<Boolean> executeCondition, Strategy0<T> action) {
        return addActionPrivate(
                new ContextStrategyAction0<>(
                        rxContext,
                        () -> executeCondition.perform()
                                ? action.perform()
                                : null
                ));
    }

    // ----------------------------------------------------
    // For lambdas with return
    // ----------------------------------------------------

    public CommandBuilder<T> addActionWithFallback(Strategy0<T> strategy) {
        return addActionPrivate(new StrategyAction0<>(strategy));
    }

    public CommandBuilder<T> addActionWithFallback(RxContext rxContext, Strategy0<T> strategy, Strategy0<T> fallback) {
        return addActionPrivate(
                new FallbackStrategyAction<>(
                        rxContext,
                        strategy,
                        new FallbackAction<>(rxContext, fallback)
                )
        );
    }

    public CommandBuilder<T> addActionWithExecuteCondition(Strategy0<Boolean> executeCondition, Strategy0<T> action) {
        return addActionPrivate(
                new StrategyAction0<>(
                        () -> executeCondition.perform()
                                ? action.perform()
                                : null
                ));
    }

    public CommandBuilder<T> addActionWithExecuteCondition(RxContext rxContext, Strategy0<Boolean> executeCondition, Strategy0<T> action) {
        return addActionPrivate(
                new ContextStrategyAction0<>(
                        rxContext,
                        () -> executeCondition.perform()
                                ? action.perform()
                                : null
                ));
    }

    // ----------------------------------------------------
    // For action classes with return
    // ----------------------------------------------------

    public CommandBuilder<T> addAction(Action0<T> action) {
        return addActionPrivate(action);
    }

    // ----------------------------------------------------
    // private functions
    // ----------------------------------------------------

    private CommandBuilder<T> addActionPrivate(Action0<T> action) {
        actions.add(action);
        return this;
    }
}
