package com.intact.rx.core.cache.data.context;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

import com.intact.rx.core.cache.data.ObjectRoot;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.templates.api.Memento;

public class ObjectCacheState<K, V> {

    private final Map<K, ObjectRoot<K, V>> objects = new HashMap<>();
    private final Memento<ObjectRoot<K, V>> mementoReference;
    private final DataCacheId dataCacheId;

    private final AccessStatus accessStatus = new AccessStatus();

    public ObjectCacheState(DataCacheId dataCacheId, Memento<ObjectRoot<K, V>> mementoReference) {
        this.dataCacheId = requireNonNull(dataCacheId);
        this.mementoReference = requireNonNull(mementoReference);
    }

    public Map<K, ObjectRoot<K, V>> getObjects() {
        return objects;
    }

    public Memento<ObjectRoot<K, V>> getMemento() {
        return mementoReference;
    }

    public DataCacheId getDataCacheId() {
        return dataCacheId;
    }

    public AccessStatus getAccessStatus() {
        return accessStatus;
    }
}
