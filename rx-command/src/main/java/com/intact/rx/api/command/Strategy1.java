package com.intact.rx.api.command;

@FunctionalInterface
public interface Strategy1<Return, Arg1> {
    Return perform(Arg1 arg1);
}
