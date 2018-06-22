package com.intact.rx.api;

public class RxContextNoOp implements RxContext {

    private static final RxContext instance = new RxContextNoOp();

    public static RxContext instance() {
        return instance;
    }

    @Override
    public void before() {
    }

    @Override
    public void after() {
    }
}
