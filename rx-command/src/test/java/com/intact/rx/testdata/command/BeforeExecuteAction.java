package com.intact.rx.testdata.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.intact.rx.api.command.Action0;

public class BeforeExecuteAction implements Action0<List<String>> {

    private final List<String> result;

    public BeforeExecuteAction() {
        result = new ArrayList<>();
    }

    @Override
    public Optional<List<String>> execute() {
        result.add("execute");
        return Optional.of(result);
    }

    @Override
    public void before() {
        result.add("beforeExecute");
    }

    @Override
    public void after() {
    }
}
