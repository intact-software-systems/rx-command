package com.intact.rx.core.cache.data;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.cache.data.context.ObjectRootPolicy;
import com.intact.rx.core.cache.data.context.ObjectRootState;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.core.cache.strategy.CachePolicyChecker;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.MementoReference;
import com.intact.rx.templates.Pair;
import com.intact.rx.templates.Validate;
import com.intact.rx.templates.api.Context;

import static com.intact.rx.core.cache.data.CacheStatusUpdateAlgorithms.processOnModified;
import static com.intact.rx.core.cache.data.CacheStatusUpdateAlgorithms.processOnRead;

@SuppressWarnings("SynchronizedMethod")
public class ObjectRoot<K, V> {
    private final Context<ObjectRootPolicy, ObjectRootState<K, V>> context;

    private ObjectRoot(V value, K key, ObjectRootPolicy policy) {
        this.context = new ContextObject<>(
                policy,
                new ObjectRootState<>(
                        key,
                        value,
                        new MementoReference<>(policy.getMementoPolicy().getUndoDepth(), policy.getMementoPolicy().getRedoDepth()))
        );
    }

    private ObjectRoot(ObjectRoot<K, V> root) {
        this.context = new ContextObject<>(root.config(), root.state().copy());
    }

    public static <K, V> ObjectRoot<K, V> create(V value, K key, ObjectRootPolicy policy) {
        return new ObjectRoot<>(value, key, policy);
    }

    public ObjectRoot<K, V> copy() {
        return new ObjectRoot<>(this);
    }

    synchronized void write(V value) {
        requireNonNull(value);

        if (!Objects.equals(value, state().getValue())) {
            state().setValue(value);
            processOnModified(state().getAccessStatus(), config().getExtension());
        } else {
            state().getAccessStatus().notModified();
        }
    }

    public synchronized V read() {
        processOnRead(state().getAccessStatus(), config().getExtension());
        return state().getValue();
    }

    public synchronized AccessStatus getStatus() {
        return state().getAccessStatus();
    }

    public V getValueNoStatusUpdate() {
        return state().getValue();
    }

    public K getKey() {
        return state().getKey();
    }

    // ----------------------------------------------
    // Memento handling
    // ----------------------------------------------

    public synchronized Pair<V, V> undo() {
        if (state().getMemento().isUndoStackEmpty()) {
            return Pair.empty();
        }

        V previousCurrent = state().getMemento().read().orElseThrow(() -> new IllegalStateException("Previous current value should never be null here!"));
        V value = state().getMemento().undo().orElseThrow(() -> new IllegalStateException("Undo value should never be null here!"));

        if (!Objects.equals(value, previousCurrent)) {
            processOnModified(state().getAccessStatus(), config().getExtension());
        } else {
            state().getAccessStatus().notModified();
        }

        return Pair.create(previousCurrent, value);
    }

    public synchronized Pair<V, V> redo() {
        if (state().getMemento().isRedoStackEmpty()) {
            return Pair.empty();
        }

        V previousCurrent = state().getMemento().read().orElseThrow(() -> new IllegalStateException("Previous current value should never be null here!"));
        V value = state().getMemento().redo().orElseThrow(() -> new IllegalStateException("Redo value should never be null here!"));

        if (!Objects.equals(value, previousCurrent)) {
            processOnModified(state().getAccessStatus(), config().getExtension());
        } else {
            state().getAccessStatus().notModified();
        }

        return Pair.create(previousCurrent, value);
    }

    public synchronized ObjectRoot<K, V> clearUndo() {
        state().getMemento().clearUndo();
        return this;
    }

    public synchronized ObjectRoot<K, V> clearRedo() {
        state().getMemento().clearRedo();
        return this;
    }

    public synchronized List<V> undoStack() {
        return state().getMemento().undoStack();
    }

    public synchronized List<V> redoStack() {
        return state().getMemento().redoStack();
    }

    // ----------------------------------------------
    // loan status managemenet
    // ----------------------------------------------

    public synchronized V loan(LoanPolicy loanPolicy) {
        state().getLoanStatus().newLoan(loanPolicy);
        processOnRead(state().getAccessStatus(), config().getExtension());
        return state().getValue();
    }

    public synchronized long returnLoan() {
        long loanCount = state().getLoanStatus().returnLoan();
        Validate.assertTrue(loanCount >= 0);
        if (loanCount <= 0) {
            state().resetLoan();
        }
        return loanCount;
    }

    public ObjectRootState.LoanStatus getLoanStatus() {
        return state().getLoanStatus();
    }

    public boolean isLoaned() {
        return state().isLoaned();
    }

    public boolean isImmutable() {
        return state().isImmutable();
    }

    /**
     * Return false if root is already expired, i.e., setExpired() did nothing.
     */
    public synchronized boolean setExpired() {
        if (!state().getAccessStatus().isExpired()) {
            state().getAccessStatus().expired();
            return true;
        }
        return false;
    }

    public synchronized boolean isExpired() {
        return !CachePolicyChecker.isInLifetime(state().getAccessStatus(), config().getLifetime()) || state().getAccessStatus().isExpired();
    }

    // ---------------------------------------
    // Private functions
    // ---------------------------------------

    private ObjectRootPolicy config() {
        return context.config();
    }

    private ObjectRootState<K, V> state() {
        return context.state();
    }

    // ---------------------------------------
    // Overridden Object functions
    // ---------------------------------------

    @Override
    public String toString() {
        return "ObjectRoot{" +
                "context=" + context +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectRoot<?, ?> that = (ObjectRoot<?, ?>) o;
        return Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context);
    }
}
