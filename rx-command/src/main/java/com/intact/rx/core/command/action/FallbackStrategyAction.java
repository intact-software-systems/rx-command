package com.intact.rx.core.command.action;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxContext;
import com.intact.rx.api.command.Strategy0;

public class FallbackStrategyAction<T> extends ContextStrategyAction0<T> {

    private final FallbackAction<T> fallbackAction;

    public FallbackStrategyAction(
            RxContext context,
            Strategy0<T> action,
            FallbackAction<T> fallbackAction) {
        super(context, action);
        this.fallbackAction = requireNonNull(fallbackAction);
    }

    public FallbackAction<T> getFallback() {
        return fallbackAction;
    }

    @Override
    public String toString() {
        return "FallbackStrategyAction{" +
                "fallbackAction=" + fallbackAction +
                '}';
    }
}
