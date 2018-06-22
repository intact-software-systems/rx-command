package com.intact.rx.templates.api;

/**
 * A parametrized interface for implementation of software pattern "Context Object"
 */
public interface Context<Config, State> {

    Config config();

    State state();
}
