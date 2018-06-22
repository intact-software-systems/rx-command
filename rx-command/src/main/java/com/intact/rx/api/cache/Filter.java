package com.intact.rx.api.cache;

@FunctionalInterface
public interface Filter<T> {
    boolean apply(T value, boolean alreadyMember);
}
