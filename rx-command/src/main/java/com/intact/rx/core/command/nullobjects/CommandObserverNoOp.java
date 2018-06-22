package com.intact.rx.core.command.nullobjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.Subscription;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.observer.CommandObserver;

public class CommandObserverNoOp<T> implements CommandObserver<T> {
    @SuppressWarnings("rawtypes")
    public static final CommandObserverNoOp instance = new CommandObserverNoOp();

    private static final Logger log = LoggerFactory.getLogger(CommandObserverNoOp.class);

    @Override
    public void onError(Command<T> command, Throwable throwable) {
        log.error("Calling on NoOp!");
    }

    @Override
    public void onNext(Command<T> command, T values) {
        log.error("Calling on NoOp!");
    }

    @Override
    public void onSubscribe(Command<T> key, Subscription subscription) {
        log.error("Calling on NoOp");
    }

    @Override
    public void onComplete(Command<T> command) {
        log.error("Calling on NoOp!");
    }
}
