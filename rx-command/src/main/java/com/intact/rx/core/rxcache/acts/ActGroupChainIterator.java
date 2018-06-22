package com.intact.rx.core.rxcache.acts;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("SynchronizedMethod")
public class ActGroupChainIterator implements Iterator<ActGroup> {

    private ActGroup currentGroup;
    private ActGroupIterator currentGroupIterator;
    private AtomicInteger current;

    private final ActGroupChain chain;

    public ActGroupChainIterator(ActGroupChain chain) {
        this.chain = chain;
        this.current = new AtomicInteger(0);
    }

    public boolean isAtStart() {
        return current.get() == 0;
    }
    
    public synchronized void rewind() {
        current = new AtomicInteger(0);
        currentGroup = null;
        currentGroupIterator = null;
    }

    public synchronized ActGroup current() {
        return currentGroup;
    }

    public synchronized ActGroupIterator currentGroupIterator() {
        return currentGroupIterator;
    }

    @Override
    public synchronized ActGroup next() {
        // Precondition: Validate.assertTrue(chain.size() > current.get());
        currentGroup = chain.get(current.getAndIncrement());
        currentGroupIterator = new ActGroupIterator(currentGroup);
        return currentGroup;
    }

    @Override
    public void remove() {
        // not supported
    }

    @Override
    public synchronized boolean hasNext() {
        return current.get() < chain.size();
    }
}
