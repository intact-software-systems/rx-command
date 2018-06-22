package com.intact.rx.testdata.command;

import java.util.Optional;

import com.intact.rx.api.command.Action0;

public class TimeOutAction implements Action0<Result> {

    private final ServiceInterface serviceInterface;

    public TimeOutAction(final ServiceInterface access) {
        this.serviceInterface = access;
    }

    @Override
    public Optional<Result> execute() throws Exception {
        Thread.sleep(300);
        return Optional.of(serviceInterface.getResult());
    }

    @Override
    public void before() {
    }

    @Override
    public void after() {
    }
}
