package com.intact.rx.testdata.command;

import java.util.Optional;

import com.intact.rx.api.command.Action0;

public class SimpleAction0 implements Action0<Result> {

    private final ServiceInterface serviceInterface;

    public SimpleAction0(ServiceInterface access) {
        this.serviceInterface = access;
    }

    @Override
    public Optional<Result> execute() {
        return Optional.of(serviceInterface.getResult());
    }

    @Override
    public void before() {
    }

    @Override
    public void after() {
    }
}

