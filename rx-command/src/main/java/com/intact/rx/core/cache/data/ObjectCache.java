package com.intact.rx.core.cache.data;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.context.ObjectCachePolicy;
import com.intact.rx.core.cache.data.context.ObjectCacheState;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.core.cache.status.AccessStatus.AccessState;
import com.intact.rx.core.cache.strategy.CachePolicyChecker;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.policy.LoanReturnPolicy;
import com.intact.rx.templates.*;
import com.intact.rx.templates.api.Context;
import com.intact.rx.templates.api.Memento;

/**
 * Thread-safe access to all ObjectRoots. Simple mapping (key,value) interface.
 */
public class ObjectCache<K, V> {
    private static final long ACQUIRE_LOCK_TIMEOUT_IN_MS = 10000L;
    private static final Logger log = LoggerFactory.getLogger(ObjectCache.class);

    private final Context<ObjectCachePolicy, ObjectCacheState<K, V>> context;
    private final ReadWriteLock lock;

    ObjectCache(DataCacheId dataCacheId, DataCachePolicy policy) {
        this.context = new ContextObject<>(
                new ObjectCachePolicy(policy.getObjectRootPolicy(), policy.getResourceLimits(), policy.getMementoPolicy()),
                new ObjectCacheState<K, V>(
                        dataCacheId,
                        policy.isMemento()
                                ? new MementoReference<>(policy.getMementoPolicy().getUndoDepth(), policy.getMementoPolicy().getRedoDepth())
                                : MementoReferenceNoOp.instance
                )
        );
        this.lock = new ReentrantReadWriteLock(true);
    }

    // ----------------------------------------------
    // Memento handling
    // ----------------------------------------------

    public Memento<ObjectRoot<K, V>> getMemento() {
        return state().getMemento();
    }

    public Optional<Pair<V, ObjectRoot<K, V>>> undo(K key) {
        return this.read(key)
                .map(root -> {
                    Pair<V, V> pair = root.undo();
                    return Optional.of(Pair.create(pair.first().orElse(null), root.copy()));
                })
                .orElse(Optional.empty());
    }

    public Optional<Pair<V, ObjectRoot<K, V>>> redo(K key) {
        return this.read(key)
                .map(root -> {
                    Pair<V, V> pair = root.redo();
                    return Optional.of(Pair.create(pair.first().orElse(null), root.copy()));
                })
                .orElse(Optional.empty());
    }

    /**
     * @return tuple, where first is AccessState (EXPIRED or MODIFIED), second is value removed, third is root updated.
     */
    public Optional<Tuple3<AccessState, V, ObjectRoot<K, V>>> undo() {
        if (!config().isMemento()) {
            return Optional.empty();
        }

        if (!acquireWriteLock()) {
            throw new RuntimeException("Could not acquire write lock for cache: " + state().getDataCacheId() + " to undo last write");
        }

        try {
            if (!state().getMemento().read().isPresent()) {
                return Optional.empty();
            }

            // Undo until hit non-expired or undo returns empty
            while (true) {
                Optional<ObjectRoot<K, V>> undo = state().getMemento().read();
                if (undo.isPresent()) {
                    state().getMemento().undo();  // pop value
                    ObjectRoot<K, V> undoWrite = undo.get();

                    if (undoWrite.isExpired()) {
                        // Note: If expired then undo once more
                        continue;
                    }

                    ObjectRoot<K, V> cachedRoot = state().getObjects().get(undoWrite.getKey());
                    if (cachedRoot != null && cachedRoot.isExpired()) {
                        // Note: If expired then undo once more
                        continue;
                    }

                    if (cachedRoot != null) {
                        // Undo last write on (key, value) root
                        Pair<V, V> pair = cachedRoot.undo();
                        if (pair.isEmpty()) {
                            // undo on root is not performed, removal of key is "undo of last write"
                            ObjectRoot<K, V> removed = state().getObjects().remove(undoWrite.getKey());
                            return Optional.of(
                                    new Tuple3<>(
                                            AccessStatus.AccessState.EXPIRED,
                                            removed.getValueNoStatusUpdate(),
                                            removed)
                            );
                        }
                        else {
                            // undo is performed; Undo on (key, value) is "undo of last write"
                            return Optional.of(
                                    new Tuple3<>(
                                            AccessStatus.AccessState.MODIFIED,
                                            pair.first().orElseThrow(() -> new IllegalStateException("Null value not allowed")),
                                            cachedRoot.copy())
                            );
                        }
                    }

                    log.error("Undo failed. Key {} was not present in cache.", undoWrite.getKey());
                    break;
                }
                break;
            }

            return Optional.empty();
        } finally {
            releaseWriteLock();
        }
    }

    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> redo() {
        if (!config().isMemento()) {
            return Optional.empty();
        }

        if (!acquireWriteLock()) {
            throw new RuntimeException("Could not acquire write lock for cache: " + state().getDataCacheId() + " to redo last write");
        }

        try {
            if (state().getMemento().isRedoStackEmpty()) {
                return Optional.empty();
            }

            // Redo until hit non-expired or redo returns empty
            while (true) {
                Optional<ObjectRoot<K, V>> redo = state().getMemento().redo();
                if (redo.isPresent()) {
                    ObjectRoot<K, V> redoWrite = redo.get();

                    if (redoWrite.isExpired()) {
                        // Note: If expired then redo once more
                        continue;
                    }

                    ObjectRoot<K, V> cachedRoot = state().getObjects().get(redoWrite.getKey());
                    if (cachedRoot != null && cachedRoot.isExpired()) {
                        // Note: If expired then redo once more
                        continue;
                    }

                    if (cachedRoot != null) {
                        // Redo on cachedRoot. Redo stack should be available at this point
                        Pair<V, V> pair = cachedRoot.redo();
                        if (!pair.isEmpty()) {
                            return Optional.of(new Tuple3<>(AccessStatus.AccessState.MODIFIED, pair.first().orElse(null), cachedRoot.copy()));  // redo is performed; Redo on (key, value) is "redo of last write"
                        }
                        else {
                            log.error("Redo failed. No Redo stack for key {}.", cachedRoot.getKey());
                            break;
                        }
                    }
                    else {
                        // Redo last write (key, value), no previous value
                        state().getObjects().put(redoWrite.getKey(), redoWrite);
                        return Optional.of(new Tuple3<>(AccessStatus.AccessState.WRITE, null, redoWrite.copy()));
                    }
                }
                break;
            }
            return Optional.empty();
        } finally {
            releaseWriteLock();
        }
    }

    // ----------------------------------------------
    // Read write to object cache
    // ----------------------------------------------

    public Pair<V, ObjectRoot<K, V>> write(final K key, final V value) {
        requireNonNull(key, "Null key is not allowed. RxCache id: " + state().getDataCacheId());
        if (value == null) {
            // Note: Ignore null values, and handle quietly
            return Pair.empty();
        }

        if (!acquireWriteLock()) {
            throw new RuntimeException("Could not acquire write lock for cache: " + state().getDataCacheId() + " to write key : " + key);
        }

        try {
            final ObjectRoot<K, V> current = state().getObjects().get(key);
            if (current != null && !current.isExpired()) {
                CachePolicyChecker.assertOverwritePossible(current);

                if (config().isMemento()) {
                    state().getMemento().set(current.copy());
                }

                V oldValue = current.getValueNoStatusUpdate();
                current.write(value);
                return Pair.create(oldValue, current.copy());
            }
            else {
                CachePolicyChecker.assertWritingPossible(state().getObjects().size(), 1, config().getResourceLimits());

                ObjectRoot<K, V> newRoot = ObjectRoot.create(value, key, config().getRootPolicy());
                ObjectRoot<K, V> previous = state().getObjects().put(key, newRoot); // Note: previous == current

                if (config().isMemento()) {
                    state().getMemento().set(newRoot.copy());
                }

                return previous == null
                        ? Pair.create(null, newRoot.copy())
                        : Pair.create(previous.getValueNoStatusUpdate(), newRoot.copy());
            }
        } finally {
            releaseWriteLock();
        }
    }

    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (!acquireWriteLock()) {
            throw new RuntimeException("Could not acquire write lock for cache: " + state().getDataCacheId() + " to write key : " + key);
        }

        try {
            return read(key)
                    .map(existingRoot -> {
                        V newValue = remappingFunction.apply(existingRoot.getValueNoStatusUpdate(), value);
                        if (newValue == null) {
                            return take(key)
                                    .map(removedRoot -> Optional.of(new Tuple3<>(AccessStatus.AccessState.EXPIRED, removedRoot.getValueNoStatusUpdate(), removedRoot)))
                                    .orElseThrow(() -> new IllegalStateException("Expected removal of root"));
                        }
                        Pair<V, ObjectRoot<K, V>> pair = write(key, newValue);
                        return Optional.of(new Tuple3<>(AccessStatus.AccessState.MODIFIED, pair.first().orElseThrow(() -> new IllegalStateException("Expected existing value")), pair.second().orElseThrow(() -> new IllegalStateException("Expected root"))));
                    })
                    .orElseGet(() -> {
                        Pair<V, ObjectRoot<K, V>> pair = write(key, value);
                        return Optional.of(new Tuple3<>(AccessStatus.AccessState.WRITE, pair.first().orElse(null), pair.second().orElseThrow(() -> new IllegalStateException("Expected new root"))));
                    });
        } finally {
            releaseWriteLock();
        }
    }

    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> compute(K key, BiFunction<? super K, Optional<? super V>, Optional<? extends V>> remappingFunction) {
        if (!acquireWriteLock()) {
            throw new RuntimeException("Could not acquire write lock for cache: " + state().getDataCacheId() + " to write key : " + key);
        }

        try {
            return read(key)
                    .map((ObjectRoot<K, V> existing) ->
                            remappingFunction
                                    .apply(key, Optional.of(existing.getValueNoStatusUpdate()))
                                    .map(remappedValue ->
                                            Optional.of(write(key, remappedValue))
                                                    .map((Pair<V, ObjectRoot<K, V>> vObjectRootPair) -> new Tuple3<>(AccessStatus.AccessState.MODIFIED, existing.getValueNoStatusUpdate(), vObjectRootPair.second().orElseThrow(() -> new IllegalStateException("Expected new root")))))
                                    .orElseGet(() -> take(key).map(removedRoot -> new Tuple3<>(AccessStatus.AccessState.EXPIRED, removedRoot.getValueNoStatusUpdate(), null))))
                    .orElseGet(() ->
                            remappingFunction
                                    .apply(key, Optional.empty())
                                    .<Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>>>map(
                                            remappedValue ->
                                                    Optional.of(write(key, remappedValue))
                                                            .map((Pair<V, ObjectRoot<K, V>> vObjectRootPair) -> new Tuple3<>(AccessStatus.AccessState.WRITE, null, vObjectRootPair.second().orElseThrow(() -> new IllegalStateException("Expected new root"))))
                                    )
                                    .orElseGet(() -> take(key).map(removedRoot -> new Tuple3<>(AccessStatus.AccessState.EXPIRED, removedRoot.getValueNoStatusUpdate(), null))));
        } finally {
            releaseWriteLock();
        }
    }

    public Optional<Pair<V, ObjectRoot<K, V>>> compareAndWrite(K key, Function<V, V> expect, Supplier<V> update) {
        if (!acquireWriteLock()) {
            throw new RuntimeException("Could not acquire write lock for cache: " + state().getDataCacheId() + " to write key : " + key);
        }

        try {
            return read(key)
                    .<Optional<Pair<V, ObjectRoot<K, V>>>>map(
                            r -> {
                                V expectedValue = expect.apply(r.getValueNoStatusUpdate());
                                if (expectedValue != null && expectedValue.equals(r.getValueNoStatusUpdate())) {
                                    return Optional.of(write(key, update.get()));
                                }
                                return Optional.empty();
                            })
                    .orElseGet(
                            () -> {
                                V expectedValue = expect.apply(null);
                                return expectedValue == null
                                        ? Optional.of(write(key, update.get()))
                                        : Optional.empty();
                            }
                    );
        } finally {
            releaseWriteLock();
        }
    }


    public Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>> computeIfAbsent(final K key, final Function<? super K, ? extends V> factory) {
        requireNonNull(key, "Null key is not allowed. RxCache id: " + state().getDataCacheId());

        if (!acquireWriteLock()) {
            throw new RuntimeException("Could not acquire write lock for cache: " + state().getDataCacheId() + " to write key : " + key);
        }

        try {
            return computeIfAbsentPrivate(key, factory);
        } finally {
            releaseWriteLock();
        }
    }

    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> factory) {
        requireNonNull(key, "Null key is not allowed. RxCache id: " + state().getDataCacheId());

        if (!acquireWriteLock()) {
            throw new RuntimeException("Could not acquire write lock for cache: " + state().getDataCacheId() + " to write key : " + key);
        }

        try {
            final ObjectRoot<K, V> currentRoot = state().getObjects().get(key);

            boolean currentRootIsNull = currentRoot == null;
            boolean currentRootHasNullValue = currentRoot != null && currentRoot.getValueNoStatusUpdate() == null;
            boolean currentValueIsExpired = currentRoot != null && currentRoot.isExpired();

            if (currentRootIsNull || currentRootHasNullValue || currentValueIsExpired) {
                return Optional.empty();
            }
            else {

                V newValue = factory.apply(key, currentRoot.getValueNoStatusUpdate());
                if (newValue == null) {
                    return take(key)
                            .map(removedRoot -> new Tuple3<>(AccessStatus.AccessState.EXPIRED, removedRoot.getValueNoStatusUpdate(), removedRoot));
                }
                else {
                    Pair<V, ObjectRoot<K, V>> previous = write(key, newValue);
                    return Optional.of(
                            new Tuple3<>(
                                    AccessStatus.AccessState.MODIFIED,
                                    previous.first().orElse(null),
                                    previous.second().orElseThrow(() -> new IllegalStateException("Expected non null current root " + key))
                            )
                    );
                }
            }
        } finally {
            releaseWriteLock();
        }
    }

    public Optional<Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>>> computeIfAbsentAndLoan(final K key, final Function<? super K, ? extends V> factory, final LoanPolicy loanPolicy) {
        requireNonNull(key, "Null key is not allowed. RxCache id: " + state().getDataCacheId());

        if (!acquireWriteLock()) {
            throw new RuntimeException("Could not acquire write lock for cache: " + state().getDataCacheId() + " to write key : " + key);
        }

        try {
            ObjectRoot<K, V> root = state().getObjects().get(key);
            if (root != null && !root.isExpired() && root.getLoanStatus().isLoanedOut()) {
                return Optional.empty();
            }

            CachePolicyChecker.assertLoanReservationPossible(root, loanPolicy.getReservation());

            Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>> pair = computeIfAbsentPrivate(key, factory);
            state().getObjects().get(key).loan(loanPolicy);
            return Optional.of(pair);
        } finally {
            releaseWriteLock();
        }
    }

    private Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>> computeIfAbsentPrivate(final K key, final Function<? super K, ? extends V> factory) {
        final ObjectRoot<K, V> currentRoot = state().getObjects().get(key);

        boolean currentRootIsNull = currentRoot == null;
        boolean currentRootHasNullValue = currentRoot != null && currentRoot.getValueNoStatusUpdate() == null;
        boolean currentValueIsExpired = currentRoot != null && currentRoot.isExpired();

        if (currentRootIsNull || currentRootHasNullValue || currentValueIsExpired) {
            CachePolicyChecker.assertWritingPossible(state().getObjects().size(), 1, config().getResourceLimits());

            V newValue = factory.apply(key);
            requireNonNull(newValue, "Factory function cannot return null value!");

            ObjectRoot<K, V> newRoot = ObjectRoot.create(newValue, key, config().getRootPolicy());
            state().getObjects().put(key, newRoot);

            if (config().isMemento()) {
                state().getMemento().set(newRoot.copy());
            }
            return new Tuple3<>(AccessStatus.AccessState.WRITE, currentRoot != null ? currentRoot.getValueNoStatusUpdate() : null, newRoot.copy());
        }

        // current value exists and is valid
        return new Tuple3<>(AccessStatus.AccessState.READ, currentRoot.getValueNoStatusUpdate(), currentRoot.copy());
    }

    public Optional<ObjectRoot<K, V>> take(final K key) {
        if (key == null) {
            return Optional.empty();
        }

        if (!acquireWriteLock()) {
            return Optional.empty();
        }

        try {
            ObjectRoot<K, V> root = state().getObjects().get(key);
            if (root != null) {
                CachePolicyChecker.assertRemovalPossible(root);

                if (config().isMemento() && !state().getMemento().isAllEmpty()) {
                    state().getMemento().clearAll();
                }
                state().getObjects().remove(key);
            }
            return Optional.ofNullable(root);
        } finally {
            releaseWriteLock();
        }
    }

    public Map<K, ObjectRoot<K, V>> take(Iterable<? extends K> keys) {
        if (keys == null) {
            return Collections.emptyMap();
        }
        final Map<K, ObjectRoot<K, V>> objects = new HashMap<>();

        keys.forEach(key -> take(key).ifPresent(root -> objects.put(key, root)));
        return objects;
    }


    public Map<K, ObjectRoot<K, V>> takeAll() {
        final Map<K, ObjectRoot<K, V>> objects = new HashMap<>();

        if (!acquireWriteLock()) {
            return objects;
        }

        try {
            if (!state().getObjects().isEmpty()) {

                if (config().isMemento() && !state().getMemento().isAllEmpty()) {
                    state().getMemento().clearAll();
                }

                objects.putAll(state().getObjects());
                state().getObjects().clear();
            }

            return objects;
        } finally {
            releaseWriteLock();
        }
    }

    public boolean containsKey(final K key) {
        if (key == null) {
            return false;
        }

        if (!acquireReadLock()) {
            return false;
        }

        try {
            final ObjectRoot<K, V> objectRoot = state().getObjects().get(key);
            return objectRoot != null && !objectRoot.isExpired();
        } finally {
            releaseReadLock();
        }
    }

    public Optional<ObjectRoot<K, V>> read(final K key) {
        if (key == null) {
            return Optional.empty();
        }

        if (!acquireReadLock()) {
            return Optional.empty();
        }

        try {
            final ObjectRoot<K, V> objectRoot = state().getObjects().get(key);

            if (objectRoot == null) {
                return Optional.empty();
            }
            if (objectRoot.isExpired()) {
                return Optional.empty();
            }

            objectRoot.read();
            return Optional.of(objectRoot);
        } finally {
            releaseReadLock();
        }
    }

    public Optional<ObjectRoot<K, V>> loan(final K key, final LoanPolicy loanPolicy) {
        if (key == null) {
            return Optional.empty();
        }

        if (!acquireReadLock()) {
            return Optional.empty();
        }

        try {
            final ObjectRoot<K, V> objectRoot = state().getObjects().get(key);

            if (objectRoot == null) {
                return Optional.empty();
            }
            if (objectRoot.isExpired()) {
                return Optional.empty();
            }
            if (objectRoot.getLoanStatus().isLoanedOut()) {
                return Optional.empty();
            }

            CachePolicyChecker.assertLoanReservationPossible(objectRoot, loanPolicy.getReservation());

            objectRoot.loan(loanPolicy);
            return Optional.of(objectRoot.copy());
        } finally {
            releaseReadLock();
        }
    }

    public Optional<ObjectRoot<K, V>> returnLoan(K key, LoanReturnPolicy unused) {
        requireNonNull(key);
        if (!acquireWriteLock()) {
            return Optional.empty();
        }

        try {
            ObjectRoot<K, V> objectRoot = state().getObjects().get(key);
            if (objectRoot != null &&
                    objectRoot.isLoaned() &&
                    objectRoot.returnLoan() <= 0 &&
                    Objects.equals(unused, LoanReturnPolicy.REMOVE_ON_NO_LOAN)) {

                if (config().isMemento() && !state().getMemento().isAllEmpty()) {
                    state().getMemento().clearAll();
                }

                return Optional.of(state().getObjects().remove(key));
            }
            return Optional.empty();
        } finally {
            releaseWriteLock();
        }
    }

    public boolean isLoaned(final K key) {
        requireNonNull(key);
        if (!acquireReadLock()) {
            return false;
        }

        try {
            ObjectRoot<K, V> objectRoot = state().getObjects().get(key);
            return objectRoot != null && objectRoot.isLoaned();
        } finally {
            releaseReadLock();
        }
    }

    public Map<K, V> read(Iterable<? extends K> keys) {
        if (keys == null) {
            return Collections.emptyMap();
        }

        Map<K, V> objects = new HashMap<>();
        keys.forEach(k -> read(k).map(ObjectRoot::getValueNoStatusUpdate).ifPresent(v -> objects.put(k, v)));
        return objects;
    }

    /**
     * Returns a map of all values in cache that are not null and not expired.
     */
    public Map<K, V> readAll() {
        final Map<K, V> entries = new HashMap<>();

        if (!acquireReadLock()) {
            return entries;
        }

        try {
            for (Entry<K, ObjectRoot<K, V>> entry : state().getObjects().entrySet()) {
                final ObjectRoot<K, V> objectRoot = entry.getValue();

                if (isValid(objectRoot)) {
                    entries.put(objectRoot.getKey(), objectRoot.read());
                }
            }
            return entries;
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Returns a list of all values in cache that are not null and not expired.
     */
    public List<V> readAsList() {
        final List<V> values = new ArrayList<>();

        if (!acquireReadLock()) {
            return values;
        }

        try {
            for (Entry<K, ObjectRoot<K, V>> entry : state().getObjects().entrySet()) {
                final ObjectRoot<K, V> objectRoot = entry.getValue();

                if (isValid(objectRoot)) {
                    values.add(objectRoot.read());
                }
            }
            return values;
        } finally {
            releaseReadLock();
        }
    }

    public DataCacheId getCacheId() {
        return state().getDataCacheId();
    }

    public int size() {
        if (!acquireReadLock()) {
            return -1;
        }

        try {
            return state().getObjects().size();
        } finally {
            releaseReadLock();
        }
    }

    public boolean isEmpty() {
        if (!acquireReadLock()) {
            return false;
        }

        try {
            return state().getObjects().isEmpty();
        } finally {
            releaseReadLock();
        }
    }

    public Map<K, ObjectRoot<K, V>> clear() {
        Map<K, ObjectRoot<K, V>> objects = new HashMap<>();

        if (!acquireWriteLock()) {
            return objects;
        }

        try {
            for (Entry<K, ObjectRoot<K, V>> entry : state().getObjects().entrySet()) {
                objects.put(entry.getKey(), entry.getValue());
            }

            if (config().isMemento() && !state().getMemento().isAllEmpty()) {
                state().getMemento().clearAll();
            }

            state().getObjects().clear();
            return objects;
        } finally {
            releaseWriteLock();
        }
    }

    public boolean isExpired(K key) {
        if (key == null) {
            return false;
        }
        if (!acquireReadLock()) {
            return false;
        }

        try {
            return !state().getObjects().containsKey(key) || state().getObjects().get(key).isExpired();
        } finally {
            releaseReadLock();
        }
    }

    public boolean isExpired() {
        if (!acquireReadLock()) {
            return false;
        }

        try {
            return !CachePolicyChecker.isInLifetime(state().getAccessStatus(), config().getRootPolicy().getLifetime());
        } finally {
            releaseReadLock();
        }
    }

    // -----------------------------------------------------------
    // Public functions
    // -----------------------------------------------------------

    public Set<K> keySet() {
        if (!acquireReadLock()) {
            return Collections.emptySet();
        }

        try {
            Set<K> keys = new HashSet<>();

            for (Entry<K, ObjectRoot<K, V>> entry : state().getObjects().entrySet()) {
                final ObjectRoot<K, V> objectRoot = entry.getValue();

                if (objectRoot == null) {
                    log.warn("Null root in cache: {}", entry);
                    continue;
                }
                if (objectRoot.isExpired()) {
                    continue;
                }

                keys.add(entry.getKey());
            }

            return keys;
        } finally {
            releaseReadLock();
        }
    }

    public Set<Entry<K, V>> entrySet() {
        if (!acquireReadLock()) {
            return Collections.emptySet();
        }

        try {
            return state()
                    .getObjects()
                    .entrySet()
                    .stream()
                    .map(entry -> new SimpleImmutableEntry<>(entry.getKey(), entry.getValue().getValueNoStatusUpdate()))
                    .collect(Collectors.toSet());
        } finally {
            releaseReadLock();
        }
    }

    /**
     * @return a mutable list of references to object roots stored in the cache (not copied)
     */
    public List<ObjectRoot<K, V>> getRoots() {
        if (!acquireReadLock()) {
            return Collections.emptyList();
        }

        try {
            return new ArrayList<>(state().getObjects().values());
        } finally {
            releaseReadLock();
        }
    }

    // -----------------------------------------------------------
    // Private functions
    // -----------------------------------------------------------

    ObjectCachePolicy config() {
        return context.config();
    }

    private ObjectCacheState<K, V> state() {
        return context.state();
    }

    private boolean isValid(ObjectRoot<K, V> objectRoot) {
        return !(objectRoot == null || objectRoot.isExpired() || objectRoot.getValueNoStatusUpdate() == null);
    }

    private boolean acquireWriteLock() {
        try {
            return lock.writeLock().tryLock() || lock.writeLock().tryLock(ACQUIRE_LOCK_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("{} interrupted with acquiring read lock", this, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void releaseWriteLock() {
        lock.writeLock().unlock();
    }

    private boolean acquireReadLock() {
        try {
            return lock.readLock().tryLock() || lock.readLock().tryLock(ACQUIRE_LOCK_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("{} interrupted with acquiring read lock", this, e);
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private void releaseReadLock() {
        lock.readLock().unlock();
    }
}

