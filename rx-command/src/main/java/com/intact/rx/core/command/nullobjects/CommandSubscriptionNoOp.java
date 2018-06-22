package com.intact.rx.core.command.nullobjects;

import com.intact.rx.api.Subscription;

public class CommandSubscriptionNoOp implements Subscription {
    public static final CommandSubscriptionNoOp instance = new CommandSubscriptionNoOp();

    @Override
    public void request(long n) {

    }

    @Override
    public void cancel() {

    }
}
