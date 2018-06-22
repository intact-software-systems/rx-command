package com.intact.rx.api.command;

@FunctionalInterface
public interface VoidStrategy3<Arg1, Arg2, Arg3> {
    void perform(Arg1 arg1, Arg2 arg2, Arg3 arg3);
}