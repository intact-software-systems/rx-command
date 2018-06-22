package com.intact.rx.testdata.command;

import java.util.Optional;

import com.intact.rx.api.command.Action0;

public class ExceptionThrowingAction implements Action0<Result> {

    @Override
    public Optional<Result> execute() throws Exception {
        throw new Exception("FAILED");
    }

    @Override
    public void before() {
    }

    @Override
    public void after() {
    }
}
