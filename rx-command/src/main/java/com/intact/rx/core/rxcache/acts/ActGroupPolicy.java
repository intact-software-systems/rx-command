package com.intact.rx.core.rxcache.acts;

import com.intact.rx.policy.Computation;

public class ActGroupPolicy {
    public static final ActGroupPolicy sequential = new ActGroupPolicy(Computation.SEQUENTIAL);
    public static final ActGroupPolicy parallel = new ActGroupPolicy(Computation.PARALLEL);

    private final Computation computation;

    private ActGroupPolicy(Computation computation) {
        this.computation = computation;
    }

    public Computation getComputation() {
        return computation;
    }

    @Override
    public String toString() {
        return "ActGroupPolicy{" +
                "computation=" + computation +
                '}';
    }
}
