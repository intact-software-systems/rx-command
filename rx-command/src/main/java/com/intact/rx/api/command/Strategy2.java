package com.intact.rx.api.command;

@FunctionalInterface
public interface Strategy2<Return, Arg1, Arg2> {
    Return perform(Arg1 arg1, Arg2 arg2);
}
