package com.intact.rx.templates;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxContext;
import com.intact.rx.api.command.VoidStrategy0;

public class RxContexts implements RxContext {
    private final List<VoidStrategy0> beforeFunctions;
    private final List<VoidStrategy0> afterFunctions;
    private final List<RxContext> rxContexts;

    private RxContexts(List<VoidStrategy0> beforeFunctions, List<VoidStrategy0> afterFunctions, List<RxContext> rxContexts) {
        this.beforeFunctions = requireNonNull(beforeFunctions);
        this.afterFunctions = requireNonNull(afterFunctions);
        this.rxContexts = requireNonNull(rxContexts);
    }

    @Override
    public void before() {
        beforeFunctions.forEach(VoidStrategy0::perform);
        rxContexts.forEach(RxContext::before);
    }

    @Override
    public void after() {
        afterFunctions.forEach(VoidStrategy0::perform);
        rxContexts.forEach(RxContext::after);
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private final List<VoidStrategy0> beforeFunctions = new ArrayList<>();
        private final List<VoidStrategy0> afterFunctions = new ArrayList<>();
        private final List<RxContext> rxContexts = new ArrayList<>();

        public Builder withRxContext(RxContext rxContext) {
            this.rxContexts.add(requireNonNull(rxContext));
            return this;
        }

        public Builder withRxContexts(List<RxContext> rxContexts) {
            this.rxContexts.addAll(requireNonNull(rxContexts));
            return this;
        }

        public Builder withBefore(VoidStrategy0 before) {
            this.beforeFunctions.add(requireNonNull(before));
            return this;
        }

        public Builder withBeforeFunctions(List<VoidStrategy0> beforeFunctions) {
            this.beforeFunctions.addAll(requireNonNull(beforeFunctions));
            return this;
        }

        public Builder withAfter(VoidStrategy0 after) {
            this.afterFunctions.add(requireNonNull(after));
            return this;
        }

        public Builder withAfterFunctions(List<VoidStrategy0> afterFunctions) {
            this.beforeFunctions.addAll(requireNonNull(afterFunctions));
            return this;
        }

        public RxContext build() {
            return new RxContexts(beforeFunctions, afterFunctions, rxContexts);
        }
    }
}
