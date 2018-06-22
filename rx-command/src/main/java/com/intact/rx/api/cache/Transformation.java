package com.intact.rx.api.cache;

import java.util.Optional;

@FunctionalInterface
public interface Transformation<Return, T> {
    Optional<Return> apply(T value, boolean alreadyMember);
}
