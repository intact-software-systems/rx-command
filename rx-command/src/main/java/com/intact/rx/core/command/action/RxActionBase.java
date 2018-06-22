package com.intact.rx.core.command.action;

import com.intact.rx.api.RxContext;
import com.intact.rx.api.RxContextNoOp;
import com.intact.rx.api.command.Action;

public abstract class RxActionBase implements Action {

    private final RxContext context;

    protected RxActionBase(RxContext context) {
        this.context = context;
    }

    protected RxActionBase() {
        this.context = RxContextNoOp.instance();
    }

    @Override
    public void before() {
        context.before();
    }

    @Override
    public void after() {
        context.after();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "RxActionBase{" +
                "context=" + context +
                '}';
    }
}
