package com.intact.rx.templates;

import java.util.Objects;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import com.intact.rx.templates.api.Context;

public class ContextObject<Config, State> implements Context<Config, State> {
    private final Config config;
    private final State state;

    public ContextObject(Config config, State state) {
        this.config = requireNonNull(config);
        this.state = requireNonNull(state);
    }

    @Override
    public Config config() {
        return config;
    }

    @Override
    public State state() {
        return state;
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextObject<?, ?> that = (ContextObject<?, ?>) o;
        return Objects.equals(config, that.config) &&
                Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return hash(config, state);
    }

    @Override
    public String toString() {
        return "ContextObject{" +
                "\n         config=" + config +
                "\n         state=" + state +
                "\n     }";
    }
}
