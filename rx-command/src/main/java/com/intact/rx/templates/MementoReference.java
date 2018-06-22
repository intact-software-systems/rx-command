package com.intact.rx.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

import com.intact.rx.templates.api.Memento;

public final class MementoReference<T> implements Memento<T> {
    private final BlockingDeque<T> undos;
    private final BlockingDeque<T> redos;

    private final AtomicReference<T> reference;

    private final MementoReference.Policy policy;

    public MementoReference(int undoDepth, int redoDepth) {
        this.undos = new LinkedBlockingDeque<>();
        this.redos = new LinkedBlockingDeque<>();
        this.reference = new AtomicReference<>(null);
        this.policy = new Policy(undoDepth, redoDepth);
    }

    private MementoReference(MementoReference<T> reference) {
        this.undos = new LinkedBlockingDeque<>(reference.undos);
        this.redos = new LinkedBlockingDeque<>(reference.redos);
        this.reference = new AtomicReference<>(reference.reference.get());
        this.policy = reference.policy;
    }

    @Override
    public Memento<T> copy() {
        return new MementoReference<>(this);
    }

    @Override
    public T get() {
        return reference.get();
    }

    @Override
    public Optional<T> read() {
        return Optional.ofNullable(reference.get());
    }

    @Override
    public Memento<T> set(T newValue) {
        T previousCurrent = reference.getAndSet(newValue);
        if (previousCurrent != null && policy.getUndoDepth() > 0) {

            // Remove an undo read if full
            if (undos.size() >= policy.getUndoDepth()) {
                undos.pollLast();
            }

            // NB! Add to front, like a stack
            undos.addFirst(previousCurrent);
        }
        redos.clear();
        return this;
    }

    @Override
    public boolean compareAndSet(T expect, T update) {
        boolean isSet = reference.compareAndSet(expect, update);
        if (isSet && expect != null && policy.getUndoDepth() > 0) {
            // Remove an undo read if full
            if (undos.size() >= policy.getUndoDepth()) {
                undos.pollLast();
            }

            // NB! Add to front, like a stack
            undos.addFirst(expect);
            redos.clear();
        }
        return isSet;
    }

    @Override
    public T getAndSet(T newValue) {
        T previousCurrent = reference.getAndSet(newValue);
        if (previousCurrent != null && policy.getUndoDepth() > 0) {

            // Remove an undo read if full
            if (undos.size() >= policy.getUndoDepth()) {
                undos.pollLast();
            }

            // NB! Add to front, like a stack
            undos.addFirst(previousCurrent);
        }
        redos.clear();
        return previousCurrent;
    }

    @Override
    public List<T> undoStack() {
        return new ArrayList<>(undos);
    }

    @Override
    public List<T> redoStack() {
        return new ArrayList<>(redos);
    }

    @Override
    public Optional<T> undo() {
        T undoToCurrent = undos.poll();
        T previousCurrent = reference.getAndSet(undoToCurrent);

        if (previousCurrent != null && policy.getRedoDepth() > 0) {

            // Remove a redo read if full
            if (redos.size() >= policy.getRedoDepth()) {
                redos.pollLast();
            }

            // NB! Add to front, like a stack
            redos.addFirst(previousCurrent);
        }

        return Optional.ofNullable(undoToCurrent);
    }


    @Override
    public Optional<T> redo() {
        T redoValue = redos.poll();
        T previousCurrent = reference.getAndSet(redoValue);

        if (previousCurrent != null && policy.getUndoDepth() > 0) {

            // Remove an undo read if full
            if (undos.size() >= policy.getUndoDepth()) {
                undos.pollLast();
            }

            // NB! Add to front, like a stack
            undos.addFirst(previousCurrent);
        }
        return Optional.ofNullable(redoValue);
    }

    @Override
    public Memento<T> clearRedo() {
        redos.clear();
        return this;
    }

    @Override
    public Memento<T> clearUndo() {
        undos.clear();
        return this;
    }

    @Override
    public Memento<T> clearAll() {
        undos.clear();
        redos.clear();
        reference.set(null);
        return this;
    }

    @Override
    public boolean isUndoStackEmpty() {
        return undos.isEmpty();
    }

    @Override
    public boolean isRedoStackEmpty() {
        return redos.isEmpty();
    }

    @Override
    public boolean isAllEmpty() {
        return reference.get() == null && undos.isEmpty() && redos.isEmpty();
    }

    public static final class Policy {
        private final int undoDepth;
        private final int redoDepth;

        Policy(int undoDepth, int redoDepth) {
            Validate.assertTrue(undoDepth >= 0);
            Validate.assertTrue(redoDepth >= 0);

            this.undoDepth = undoDepth;
            this.redoDepth = redoDepth;
        }

        int getUndoDepth() {
            return undoDepth;
        }

        int getRedoDepth() {
            return redoDepth;
        }
    }
}
