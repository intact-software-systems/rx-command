package com.intact.rx.api.command;

@FunctionalInterface
public interface Strategy3<Return, Arg1, Arg2, Arg3> {
    Return perform(Arg1 arg1, Arg2 arg2, Arg3 arg3);
}