package com.intact.rx.core.command.action;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.command.Action0;
import com.intact.rx.api.command.Strategy0;

public class StrategyAction0<T> implements Action0<T> {

    private final Strategy0<T> action;

    public StrategyAction0(Strategy0<T> action) {
        this.action = requireNonNull(action);
    }

    @Override
    public Optional<T> execute() {
        return Optional.ofNullable(action.perform());
    }

    @Override
    public void before() {
    }

    @Override
    public void after() {
    }

    @Override
    public String toString() {
        return "StrategyAction0{" +
                "action=" + action +
                '}';
    }
}
