package com.intact.rx.api.command;

import java.util.Optional;

public interface Action0<T> extends Action {
    Optional<T> execute() throws Exception;

    default boolean isVoid() {
        return false;
    }
}