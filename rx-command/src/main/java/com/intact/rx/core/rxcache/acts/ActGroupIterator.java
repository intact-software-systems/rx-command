package com.intact.rx.core.rxcache.acts;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.intact.rx.core.rxcache.api.Act;

public class ActGroupIterator implements Iterator<Act<?, ?>> {
    private final AtomicInteger current;
    private final ActGroup chain;
    private final AtomicReference<Act<?, ?>> currentAct;

    ActGroupIterator(ActGroup chain) {
        this.chain = chain;
        this.current = new AtomicInteger(0);
        this.currentAct = new AtomicReference<>(null);
    }

    public boolean isAtStart() {
        return current.get() == 0;
    }

    public ActGroupPolicy getConfig() {
        return chain.config();
    }

    public Optional<Act<?, ?>> current() {
        return Optional.ofNullable(currentAct.get());
    }

    @Override
    public boolean hasNext() {
        return current.get() < chain.getList().size();
    }

    @Override
    public Act<?, ?> next() {
        Act<?, ?> act = chain.getList().get(current.getAndIncrement());
        currentAct.set(act);
        return act;
    }

    @Override
    public void remove() {
        // Not supported
    }

    @Override
    public String toString() {
        return "ActGroupIterator{" +
                "chain=" + chain +
                '}';
    }
}
