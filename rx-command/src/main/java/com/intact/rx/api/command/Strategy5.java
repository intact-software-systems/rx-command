package com.intact.rx.api.command;

@FunctionalInterface
public interface Strategy5<Return, Arg1, Arg2, Arg3, Arg4, Arg5> {
    Return perform(Arg1 arg1, Arg2 arg2, Arg3 arg3, Arg4 arg4, Arg5 arg5);
}