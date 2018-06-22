package com.intact.rx.api.command;

@FunctionalInterface
public interface VoidStrategy2<Arg1, Arg2> {
    void perform(Arg1 arg1, Arg2 arg2);
}