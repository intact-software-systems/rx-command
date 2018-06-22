package com.intact.rx.core.command.action;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxContext;
import com.intact.rx.api.command.Strategy0;

public class ContextStrategyAction0<T> extends StrategyAction0<T> {

    private final RxContext context;

    public ContextStrategyAction0(
            RxContext context,
            Strategy0<T> action) {
        super(action);
        this.context = requireNonNull(context);
    }

    @Override
    public void before() {
        context.before();
    }

    @Override
    public void after() {
        context.after();
    }

    @Override
    public String toString() {
        return "ContextStrategyAction0{" +
                "context=" + context +
                '}';
    }
}
