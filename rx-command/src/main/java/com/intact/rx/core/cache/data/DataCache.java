package com.intact.rx.core.cache.data;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.*;
import com.intact.rx.api.cache.observer.*;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.command.VoidStrategy2;
import com.intact.rx.core.cache.CacheSelection;
import com.intact.rx.core.cache.TransformationSelection;
import com.intact.rx.core.cache.data.api.KeyValueCache;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.context.DataCacheState;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.core.cache.status.AccessStatus.AccessState;
import com.intact.rx.core.cache.strategy.CachePolicyChecker;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.policy.LoanReturnPolicy;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.Pair;
import com.intact.rx.templates.Tuple3;

import static com.intact.rx.core.cache.data.CacheStatusUpdateAlgorithms.processOnModified;
import static com.intact.rx.core.cache.data.CacheStatusUpdateAlgorithms.processOnRead;

/**
 * Access to object values in ObjectCache using simple (key,value) interface.
 * <p>
 * The DataCache notifies all observers attached regarding ObjectRoot status changes and DataCache status changes.
 */
@SuppressWarnings({"PublicMethodNotExposedInInterface", "SynchronizeOnThis"})
public class DataCache<K, V> implements KeyValueCache<K, V> {
    private static final Logger log = LoggerFactory.getLogger(DataCache.class);

    private final ContextObject<DataCachePolicy, DataCacheState<K, V>> context;

    public DataCache(final CacheMaster cacheMaster, final CacheHandle cacheHandle, final DataCachePolicy dataCachePolicy) {
        this.context = new ContextObject<>(dataCachePolicy, new DataCacheState<>(cacheMaster, cacheHandle));
    }

    DataCache(DataCache<K, V> dataCache) {
        this.context = new ContextObject<>(dataCache.config(), new DataCacheState<>(dataCache.state().getCacheMaster(), dataCache.getCacheId()));
    }

    // ---------------------------------------
    // Memento functions
    // ---------------------------------------

    @Override
    public List<V> undoStack(K key) {
        return cache().read(key)
                .map(ObjectRoot::undoStack)
                .orElse(Collections.emptyList());
    }

    @Override
    public List<V> redoStack(K key) {
        return cache().read(key)
                .map(ObjectRoot::redoStack)
                .orElse(Collections.emptyList());
    }

    @Override
    public Optional<V> undo(K key) {
        return cache().undo(key)
                .map((Pair<V, ObjectRoot<K, V>> pair) -> {
                    pair.first().ifPresent((V previousValue) -> {
                        notifyRemovedFromCacheObserver(previousValue);
                        pair.second().ifPresent(root -> processCacheUpdate(root.getKey(), root.getValueNoStatusUpdate(), AccessStatus.AccessState.MODIFIED));
                    });
                    // Return previous value
                    return pair.first();
                })
                .map(previous -> {
                    previous.ifPresent(v -> state().getMementoSubject().onUndo(new SimpleImmutableEntry<>(key, v)));
                    return previous;
                })
                .orElse(Optional.empty());
    }

    @Override
    public Optional<V> redo(K key) {
        return cache().redo(key)
                .map((Pair<V, ObjectRoot<K, V>> pair) -> {
                    pair.first().ifPresent((V previousValue) -> {
                        notifyRemovedFromCacheObserver(previousValue);
                        pair.second().ifPresent(root -> processCacheUpdate(root.getKey(), root.getValueNoStatusUpdate(), AccessStatus.AccessState.MODIFIED));
                    });
                    // Return previous value
                    return pair.first();
                })
                .map(previous -> {
                    previous.ifPresent(v -> state().getMementoSubject().onRedo(new SimpleImmutableEntry<>(key, v)));
                    return previous;
                })
                .orElse(Optional.empty());
    }

    @Override
    public DataCache<K, V> clearRedo(K key) {
        cache().read(key).map(ObjectRoot::clearRedo);
        return this;
    }

    @Override
    public DataCache<K, V> clearUndo(K key) {
        cache().read(key).map(ObjectRoot::clearUndo);
        return this;
    }

    /**
     * Undo last write operation
     *
     * @return key value entry that was unwritten
     */
    @Override
    public Optional<Entry<K, V>> undo() {
        return cache().undo()
                .map(tuple3 -> {
                    // Note: if not expired, then explicitly callback on removal of instance
                    if (tuple3.first != AccessStatus.AccessState.EXPIRED) {
                        notifyRemovedFromCacheObserver(tuple3.second);
                    }

                    processCacheUpdate(tuple3.third.getKey(), tuple3.third.getValueNoStatusUpdate(), tuple3.first);

                    return Optional.<Entry<K, V>>of(new SimpleImmutableEntry<>(tuple3.third.getKey(), tuple3.third.getValueNoStatusUpdate()));
                })
                .map(kvEntry -> {
                    kvEntry.ifPresent(entry -> state().getMementoSubject().onUndo(entry));
                    return kvEntry;
                })
                .orElse(Optional.empty());
    }

    /**
     * Redo last write operation that was undone
     *
     * @return key value entry that was rewritten
     */
    @Override
    public Optional<Entry<K, V>> redo() {
        return cache().redo()
                .map(tuple3 -> {
                    // Note: if not expired, then explicitly callback on removal of instance
                    if (tuple3.first != AccessStatus.AccessState.EXPIRED) {
                        notifyRemovedFromCacheObserver(tuple3.second);
                    }

                    processCacheUpdate(tuple3.third.getKey(), tuple3.third.getValueNoStatusUpdate(), tuple3.first);

                    return Optional.<Entry<K, V>>of(new SimpleImmutableEntry<>(tuple3.third.getKey(), tuple3.third.getValueNoStatusUpdate()));
                })
                .map(kvEntry -> {
                    kvEntry.ifPresent(entry -> state().getMementoSubject().onRedo(entry));
                    return kvEntry;
                })
                .orElse(Optional.empty());
    }

    @Override
    public List<Entry<K, V>> undoStack() {
        return cache().getMemento()
                .undoStack().stream()
                .map(root -> new SimpleImmutableEntry<>(root.getKey(), root.getValueNoStatusUpdate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Entry<K, V>> redoStack() {
        return cache().getMemento()
                .redoStack().stream()
                .map(root -> new SimpleImmutableEntry<>(root.getKey(), root.getValueNoStatusUpdate()))
                .collect(Collectors.toList());
    }

    @Override
    public DataCache<K, V> clearRedo() {
        cache().getMemento().clearRedo();
        return this;
    }

    @Override
    public DataCache<K, V> clearUndo() {
        cache().getMemento().clearUndo();
        return this;
    }

    // ---------------------------------------
    // RxSelection functions
    // ---------------------------------------

    @Override
    public RxSelection<K, V> computeSelectionIfAbsent(Object selectionId, Filter<V> filter) {
        RxFilter<K, V> rxFilter = state().getSelections()
                .computeIfAbsent(
                        selectionId,
                        o -> {
                            RxFilter<K, V> newFilter = new CacheSelection<>(selectionId, filter, this);
                            cache().getRoots().forEach(objectRoot -> newFilter.filter(objectRoot.getKey(), objectRoot.getValueNoStatusUpdate(), objectRoot.isExpired()));
                            return newFilter;
                        }
                );
        processCacheRead();

        //noinspection unchecked
        RxSelection<K, V> selection = (RxSelection<K, V>) rxFilter;
        requireNonNull(selection);

        return selection;
    }

    public Optional<RxSelection<K, V>> readSelection(Object selectionId) {
        return Optional.ofNullable((RxSelection<K, V>) state().getTransformations().get(selectionId));
    }

    public Optional<RxSelection<K, V>> takeSelection(Object selectionId) {
        requireNonNull(selectionId);
        RxFilter<K, V> removedFilter = state().getSelections().remove(selectionId);

        if (removedFilter instanceof CacheSelection) {
            ((CacheSelection) removedFilter).getSubject().onDetach();
        }

        //noinspection unchecked
        return Optional.ofNullable((RxSelection<K, V>) removedFilter);
    }

    public boolean isSelectionAttached(Object id) {
        return state().getSelections().containsKey(id);
    }

    // ---------------------------------------
    // RxSelection functions
    // ---------------------------------------

    public <T> RxSelection<K, T> computeTransformationIfAbsent(Object selectionId, Transformation<T, V> transformer) {
        RxFilter<K, V> rxFilter = state().getTransformations()
                .computeIfAbsent(
                        selectionId,
                        o -> {
                            RxFilter<K, V> newFilter = new TransformationSelection<>(selectionId, transformer, this);
                            cache().getRoots().forEach(objectRoot -> newFilter.filter(objectRoot.getKey(), objectRoot.getValueNoStatusUpdate(), objectRoot.isExpired()));
                            return newFilter;
                        }
                );
        processCacheRead();

        //noinspection unchecked
        RxSelection<K, T> selection = (RxSelection<K, T>) rxFilter;
        requireNonNull(selection);

        return selection;
    }

    public <T> Optional<RxSelection<K, T>> readTransformation(Object transformationId) {
        return Optional.ofNullable((RxSelection<K, T>) state().getTransformations().get(transformationId));
    }

    public <T> Optional<RxSelection<K, T>> takeTransformation(Object selectionId) {
        requireNonNull(selectionId);
        RxFilter<K, V> removedFilter = state().getTransformations().remove(selectionId);

        if (removedFilter instanceof TransformationSelection) {
            ((TransformationSelection) removedFilter).getSubject().onDetach();
        }

        //noinspection unchecked
        return Optional.ofNullable((RxSelection<K, T>) removedFilter);
    }

    public boolean isTransformationAttached(Object transformationId) {
        return state().getTransformations().containsKey(transformationId);
    }

    // ---------------------------------------
    // KeyValueCache interface
    // ---------------------------------------

    @Override
    public Optional<V> write(K key, V value) {
        Pair<V, ObjectRoot<K, V>> previous = cache().write(key, value);

        previous.first().ifPresent(this::notifyRemovedFromCacheObserver);
        previous.second().ifPresent(root -> processCacheUpdate(root.getKey(), root.getValueNoStatusUpdate(), previous.first().isPresent() ? AccessStatus.AccessState.MODIFIED : AccessStatus.AccessState.WRITE));

        return previous.first();
    }

    @Override
    public Optional<V> writeAndGet(K key, Supplier<V> factory) {
        V v = factory.get();
        if (v != null) {
            write(key, v);
        }
        return Optional.ofNullable(v);
    }

    @Override
    public Map<? extends K, ? extends V> writeAll(Map<? extends K, ? extends V> values) {
        Map<K, V> previousValues = new HashMap<>();
        values.forEach((k, v) -> write(k, v).ifPresent(previous -> previousValues.put(k, previous)));
        return previousValues;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> factory) {
        Tuple3<AccessState, V, ObjectRoot<K, V>> tuple = cache().computeIfAbsent(key, factory);

        if (tuple.first == AccessStatus.AccessState.WRITE) {
            notifyRemovedFromCacheObserver(tuple.second);
            processCacheUpdate(key, tuple.third.getValueNoStatusUpdate(), tuple.first);
        }
        return tuple.third.getValueNoStatusUpdate();
    }

    @Override
    public Optional<V> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return cache()
                .computeIfPresent(key, remappingFunction)
                .map(tuple -> {
                    if (tuple.first == AccessStatus.AccessState.EXPIRED) {
                        processCacheUpdate(key, tuple.second, tuple.first);
                        return Optional.<V>empty();
                    }
                    else if (tuple.first == AccessStatus.AccessState.MODIFIED) {
                        notifyRemovedFromCacheObserver(tuple.second);
                        processCacheUpdate(key, tuple.third.getValueNoStatusUpdate(), tuple.first);
                    }
                    return Optional.of(tuple.third.getValueNoStatusUpdate());
                })
                .orElse(Optional.empty());
    }

    @Override
    public Optional<V> compute(K key, BiFunction<? super K, Optional<? super V>, ? extends V> remappingFunction) {
        return cache()
                .compute(key, (k, object) -> Optional.ofNullable(remappingFunction.apply(k, object)))
                .map((Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>> previous) -> {
                            if (previous.first == AccessStatus.AccessState.MODIFIED || previous.first == AccessStatus.AccessState.WRITE) {
                                if (previous.second != null) {
                                    notifyRemovedFromCacheObserver(previous.second);
                                }
                                processCacheUpdate(key, previous.third.getValueNoStatusUpdate(), previous.first);
                                return previous.second;
                            }

                            if (previous.first == AccessStatus.AccessState.EXPIRED) {
                                processCacheUpdate(key, previous.second, previous.first);
                            }
                            return null;
                        }
                );
    }

    @Override
    public boolean compareAndWrite(K key, Supplier<V> expect, Supplier<V> update) {
        return cache()
                .compareAndWrite(key, v -> expect.get(), update)
                .map(previous -> {
                            previous.first().ifPresent(this::notifyRemovedFromCacheObserver);
                            previous.second().ifPresent(root -> processCacheUpdate(root.getKey(), root.getValueNoStatusUpdate(), previous.first().isPresent() ? AccessStatus.AccessState.MODIFIED : AccessStatus.AccessState.WRITE));
                            return previous.second().isPresent();
                        }
                )
                .orElse(false);
    }

    @Override
    public Optional<V> replace(K key, Supplier<V> update) {
        return cache()
                .compute(key, (k, object) -> object.map(object1 -> update.get()))
                .map((Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>> previous) -> {
                            if (previous.first == AccessStatus.AccessState.MODIFIED || previous.first == AccessStatus.AccessState.WRITE) {
                                if (previous.second != null) {
                                    notifyRemovedFromCacheObserver(previous.second);
                                }
                                processCacheUpdate(key, previous.third.getValueNoStatusUpdate(), previous.first);
                                return previous.second;
                            }

                            if (previous.first == AccessStatus.AccessState.EXPIRED) {
                                processCacheUpdate(key, previous.second, previous.first);
                            }
                            return null;
                        }
                );
    }

    @Override
    public Optional<V> merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return cache()
                .merge(key, value, remappingFunction)
                .map(previous -> {
                            if (previous.first == AccessStatus.AccessState.MODIFIED || previous.first == AccessStatus.AccessState.WRITE) {
                                if (previous.second != null) {
                                    notifyRemovedFromCacheObserver(previous.second);
                                }
                                processCacheUpdate(key, previous.third.getValueNoStatusUpdate(), previous.first);
                                return previous.third.getValueNoStatusUpdate();
                            }

                            if (previous.first == AccessStatus.AccessState.EXPIRED) {
                                processCacheUpdate(key, previous.second, previous.first);
                            }
                            return null;
                        }
                );
    }

    @Override
    public Optional<V> computeIfAbsentAndLoan(K key, Function<? super K, ? extends V> factory, LoanPolicy loanPolicy) {
        return cache()
                .computeIfAbsentAndLoan(key, factory, loanPolicy)
                .map((Tuple3<AccessStatus.AccessState, V, ObjectRoot<K, V>> tuple) -> {
                    if (tuple.first == AccessStatus.AccessState.WRITE) {
                        notifyRemovedFromCacheObserver(tuple.second);
                        processCacheUpdate(key, tuple.third.getValueNoStatusUpdate(), tuple.first);
                    }
                    return Optional.of(tuple.third.getValueNoStatusUpdate());
                })
                .orElse(Optional.empty());
    }

    @Override
    public Optional<V> take(K key) {
        return cache()
                .take(key)
                .map(root -> {
                    processCacheUpdate(root.getKey(), root.getValueNoStatusUpdate(), AccessStatus.AccessState.EXPIRED);
                    return Optional.of(root.getValueNoStatusUpdate());
                })
                .orElse(Optional.empty());
    }

    @Override
    public Map<K, V> take(Iterable<? extends K> keys) {
        return cache().take(keys).entrySet().stream()
                .peek(entry -> processCacheUpdate(entry.getKey(), entry.getValue().getValueNoStatusUpdate(), AccessStatus.AccessState.EXPIRED))
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getValueNoStatusUpdate()));
    }

    @Override
    public Map<K, V> takeAll() {
        return cache().takeAll().entrySet().stream()
                .peek(entry -> processCacheUpdate(entry.getKey(), entry.getValue().getValueNoStatusUpdate(), AccessStatus.AccessState.EXPIRED))
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getValueNoStatusUpdate()));
    }

    @Override
    public Map<K, V> takeExpired() {
        return cache().getRoots().stream()
                .filter(ObjectRoot::isExpired)
                .peek(root -> cache().take(root.getKey()))
                .collect(Collectors.toMap(ObjectRoot::getKey, ObjectRoot::getValueNoStatusUpdate));
    }

    @Override
    public boolean containsKey(K key) {
        // TODO: This is accessed from selection !!!!
        //processCacheRead();
        return cache().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cache().getRoots()
                .stream()
                .filter(kvObjectRoot -> Objects.equals(kvObjectRoot.getValueNoStatusUpdate(), value))
                .map((ObjectRoot<K, V> foundValue) -> {
                    foundValue.read();
                    return true;
                })
                .findFirst()
                .orElse(false);
    }

    @Override
    public Optional<V> read(K key) {
        processCacheRead();
        return cache().read(key).map(ObjectRoot::getValueNoStatusUpdate);
    }

    @Override
    public Optional<V> loan(K key, LoanPolicy loanPolicy) {
        processCacheRead();
        return cache().loan(key, loanPolicy).map(ObjectRoot::getValueNoStatusUpdate);
    }

    @Override
    public Optional<V> returnLoan(Loaned<V> loan) {
        @SuppressWarnings("unchecked")
        K key = (K) loan.key();
        if (key == null) {
            return Optional.empty();
        }

        Optional<ObjectRoot<K, V>> value = cache().returnLoan(key, loan.getReturnPolicy());
        if (Objects.equals(loan.getReturnPolicy(), LoanReturnPolicy.REMOVE_ON_NO_LOAN)) {
            value.ifPresent(root -> processCacheUpdate(root.getKey(), root.getValueNoStatusUpdate(), AccessStatus.AccessState.EXPIRED));
        }
        return value.map(ObjectRoot::getValueNoStatusUpdate);
    }

    @Override
    public boolean isLoaned(K key) {
        processCacheRead();
        return cache().isLoaned(key);
    }

    @Override
    public Map<K, V> read(Iterable<? extends K> keys) {
        processCacheRead();
        return cache().read(keys);
    }

    @Override
    public Map<K, V> readAll() {
        processCacheRead();
        return cache().readAll();
    }

    @Override
    public Map<K, V> readExpired() {
        processCacheRead();
        return cache().getRoots().stream()
                .filter(ObjectRoot::isExpired)
                .collect(Collectors.toMap(ObjectRoot::getKey, ObjectRoot::getValueNoStatusUpdate));
    }

    @Override
    public Set<K> keySet() {
        processCacheRead();
        return cache().keySet();
    }

    @Override
    public Set<K> keySetExpired() {
        return cache().getRoots().stream()
                .filter(ObjectRoot::isExpired)
                .map(ObjectRoot::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public List<V> readAsList() {
        // TODO: This is accessed from monitor algorithms!!!!
        //processCacheRead();
        return cache().readAsList();
    }

    @Override
    public int size() {
        if (state().isExpired()) {
            return 0;
        }
        // TODO: This is accessed from clean up algorithms!!!!
        // processCacheRead();
        return cache().size();
    }

    @Override
    public boolean isEmpty() {
        return cache().isEmpty();
    }

    @Override
    public void clear() {
        Map<K, ObjectRoot<K, V>> removed = cache().clear();

        removed.values().stream()
                .filter(root -> root != null && root.getValueNoStatusUpdate() != null)
                .forEach(root -> processCacheUpdate(root.getKey(), root.getValueNoStatusUpdate(), AccessStatus.AccessState.EXPIRED));

        Optional.ofNullable(state().getCacheMaster())
                .ifPresent(master -> master.onClearedCache(state().getCacheHandle()));

        log.debug("Cleared data cache: {}", state().getCacheHandle());
    }

    @Override
    public CacheHandle getCacheId() {
        return state().getCacheHandle();
    }

    @Override
    public boolean isExpired(K key) {
        return !CachePolicyChecker.isInLifetime(state().getAccessStatus(), config().getLifetime()) || cache().isExpired(key);
    }

    @Override
    public boolean isExpired() {
        return !CachePolicyChecker.isInLifetime(state().getAccessStatus(), config().getLifetime());
    }

    // ---------------------------------------
    // Observers
    // ---------------------------------------

    public void addObjectObserver(ObjectObserver<K, V> observer) {
        state().getObjectSubject().connect(observer);
    }

    public void removeObjectObserver(ObjectObserver<K, V> observer) {
        state().getObjectSubject().disconnect(observer);
    }

    public void addObjectTypeObserver(ObjectTypeObserver<V> observer) {
        state().getObjectTypeSubject().connect(observer);
    }

    public void removeObjectTypeObserver(ObjectTypeObserver<V> observer) {
        state().getObjectTypeSubject().disconnect(observer);
    }

    public void addCacheObserver(DataCacheObserver observer) {
        Optional.ofNullable(state().getCacheMaster())
                .map(cacheMaster -> cacheMaster.connect(observer));
    }

    public void removeCacheObserver(DataCacheObserver observer) {
        Optional.ofNullable(state().getCacheMaster())
                .map(cacheMaster -> cacheMaster.disconnect(observer));
    }

    public DataCache<K, V> onObjectCreatedDo(VoidStrategy2<K, V> strategy) {
        state().getObjectLambdaSubject().onObjectCreatedDo(strategy);
        return this;
    }

    public DataCache<K, V> onObjectModifiedDo(VoidStrategy2<K, V> strategy) {
        state().getObjectLambdaSubject().onObjectModifiedDo(strategy);
        return this;
    }

    public DataCache<K, V> onObjectRemovedDo(VoidStrategy2<K, V> strategy) {
        state().getObjectLambdaSubject().onObjectRemovedDo(strategy);
        return this;
    }

    public void addMementoObserver(MementoObserver<Entry<K, V>> observer) {
        state().getMementoSubject().connect(observer);
    }

    public void removeMementoObserver(MementoObserver<Entry<K, V>> mementoObserver) {
        state().getMementoSubject().disconnect(mementoObserver);
    }

    public DataCache<K, V> onUndoDo(VoidStrategy1<Entry<K, V>> strategy) {
        state().getMementoSubject().onUndoDo(strategy);
        return this;
    }

    public DataCache<K, V> onRedoDo(VoidStrategy1<Entry<K, V>> strategy) {
        state().getMementoSubject().onRedoDo(strategy);
        return this;
    }

    public void disconnectAll() {
        state().getObjectSubject().disconnectAll();
        state().getObjectLambdaSubject().disconnectAll();
        state().getObjectTypeSubject().disconnectAll();
        state().getMementoSubject().disconnectAll();
    }

    // ---------------------------------------
    // public functions
    // ---------------------------------------

    public Set<K> findKeysWithAccessStates(final AccessStatus.AccessState... accessStates) {
        final Set<K> match = new HashSet<>();
        for (ObjectRoot<K, V> objectRoot : cache().getRoots()) {
            for (AccessStatus.AccessState accessState : accessStates) {
                if (accessState == objectRoot.getStatus().getAccessState()) {
                    match.add(objectRoot.getKey());
                }
            }
        }

        return match;
    }

    public List<ObjectRoot<K, V>> getRoots() {
        return cache().getRoots();
    }

    public boolean setExpired() {
        if (!state().isExpired()) {
            boolean expired = false;
            synchronized (this) {
                if (!state().isExpired()) {
                    state().doExpire();
                    expired = true;
                }
            }
            if (expired) {
                cache().getRoots().forEach(root -> {
                    root.setExpired();
                    processCacheUpdate(root.getKey(), root.getValueNoStatusUpdate(), AccessStatus.AccessState.EXPIRED);
                });

                Optional.ofNullable(state().getCacheMaster())
                        .ifPresent(master -> master.onRemovedCache(state().getCacheHandle()));
                return true;
            }
        }

        state().doFinalize();
        return false;
    }

    public AccessStatus getAccessStatus() {
        return state().getAccessStatus();
    }

    public DataCachePolicy config() {
        return context.config();
    }

    // ---------------------------------------
    // Private functions
    // ---------------------------------------

    protected DataCacheState<K, V> state() {
        return context.state();
    }

    private void processCacheRead() {
        processOnRead(state().getAccessStatus(), config().getExtension());
    }

    private boolean processCacheUpdate(K key, V value, AccessStatus.AccessState state) {
        requireNonNull(key);
        requireNonNull(value);
        requireNonNull(state);

        state().getSelections().forEach((id, selection) -> selection.filter(key, value, state == AccessStatus.AccessState.EXPIRED));
        state().getTransformations().forEach((id, selection) -> selection.filter(key, value, state == AccessStatus.AccessState.EXPIRED));

        switch (state) {
            case WRITE:
                state().getObjectSubject().onObjectCreated(key, value);
                state().getObjectLambdaSubject().onObjectCreated(key, value);
                state().getObjectTypeSubject().onObjectCreated(value);

                Optional.ofNullable(state().getCacheMaster())
                        .ifPresent(master -> master.onModifiedCache(state().getCacheHandle()));

                processOnModified(state().getAccessStatus(), config().getExtension());

                notifyAddedToCacheObserver(value);

                break;
            case MODIFIED:
                state().getObjectSubject().onObjectModified(key, value);
                state().getObjectLambdaSubject().onObjectModified(key, value);
                state().getObjectTypeSubject().onObjectModified(value);

                Optional.ofNullable(state().getCacheMaster())
                        .ifPresent(master -> master.onModifiedCache(state().getCacheHandle()));

                processOnModified(state().getAccessStatus(), config().getExtension());

                break;
            case EXPIRED:
                state().getObjectSubject().onObjectRemoved(key, value);
                state().getObjectLambdaSubject().onObjectRemoved(key, value);
                state().getObjectTypeSubject().onObjectRemoved(value);

                Optional.ofNullable(state().getCacheMaster())
                        .ifPresent(master -> master.onModifiedCache(state().getCacheHandle()));

                notifyRemovedFromCacheObserver(value);

                break;
            case NOT_MODIFIED:
            case READ:
                break;
        }

        return true;
    }

    private boolean notifyRemovedFromCacheObserver(V value) {
        try {
            if (value instanceof RemovedFromCacheObserver) {
                RemovedFromCacheObserver removedFromCacheObserver = (RemovedFromCacheObserver) value;
                removedFromCacheObserver.onRemovedFromCache();
                return true;
            }
        } catch (RuntimeException e) {
            log.warn("Exception caught when performing callback to RemovedFromCacheObserver: {}", value, e);
        }
        return false;
    }

    private void notifyAddedToCacheObserver(V value) {
        try {
            if (value instanceof AddedToCacheObserver) {
                AddedToCacheObserver observer = (AddedToCacheObserver) value;
                observer.onAddedToCache();
            }
        } catch (RuntimeException e) {
            log.warn("Exception caught when performing callback to AddedToCacheObserver: {}", value, e);
        }
    }

    private ObjectCache<K, V> cache() {
        return Optional.ofNullable(state().getCacheMaster())
                .<ObjectCache<K, V>>map(cacheMaster -> cacheMaster.getOrCreateObjectCache(state().getCacheHandle(), config()))
                .orElseGet(() -> {
                    log.warn("CacheMaster was expired. Handling is quiet.");
                    return new ObjectCacheNoAccess<>(state().getCacheHandle().getDataCacheId(), config());
                });
    }
}
