package com.intact.rx.core.command.action;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxContext;
import com.intact.rx.api.command.Action0;
import com.intact.rx.api.command.Strategy0;

public class FallbackAction<T> implements Action0<T> {

    private final Strategy0<T> strategy;
    private final RxContext context;

    public FallbackAction(RxContext context, Strategy0<T> strategy) {
        this.strategy = requireNonNull(strategy);
        this.context = requireNonNull(context);
    }

    @Override
    public Optional<T> execute() {
        return Optional.ofNullable(strategy.perform());
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
        return "FallbackAction{" +
                "strategy=" + strategy +
                ", context=" + context +
                '}';
    }
}
