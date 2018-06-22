package com.intact.rx.core.command.action;

import com.intact.rx.api.RxContext;
import com.intact.rx.api.RxContextNoOp;
import com.intact.rx.api.command.Strategy0;

public class VoidAction<T> extends ContextStrategyAction0<T> {
    public VoidAction(RxContext context, Strategy0<T> action) {
        super(context, action);
    }

    public VoidAction(Strategy0<T> action) {
        super(RxContextNoOp.instance(), action);
    }

    @Override
    public boolean isVoid() {
        return true;
    }
}
