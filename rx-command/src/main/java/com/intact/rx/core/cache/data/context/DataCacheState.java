package com.intact.rx.core.cache.data.context;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxFilter;
import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.core.cache.subject.MementoValueSubject;
import com.intact.rx.core.cache.subject.ObjectLambdaSubject;
import com.intact.rx.core.cache.subject.ObjectSubject;
import com.intact.rx.core.cache.subject.ObjectTypeSubject;

public class DataCacheState<K, V> {
    private final WeakReference<CacheMaster> cacheMaster;
    private final CacheHandle cacheHandle;

    private final ObjectSubject<K, V> objectSubject = new ObjectSubject<>();
    private final ObjectLambdaSubject<K, V> objectLambdaSubject = new ObjectLambdaSubject<>();
    private final ObjectTypeSubject<V> objectTypeSubject = new ObjectTypeSubject<>();
    private final MementoValueSubject<Map.Entry<K, V>> mementoSubject = new MementoValueSubject<>();

    private final AccessStatus accessStatus = new AccessStatus();
    private final Map<Object, RxFilter<K, V>> selections = new ConcurrentHashMap<>();
    private final Map<Object, RxFilter<K, V>> transformations = new ConcurrentHashMap<>();

    public DataCacheState(final CacheMaster cacheMaster, final CacheHandle cacheHandle) {
        this.cacheMaster = new WeakReference<>(cacheMaster);
        this.cacheHandle = cacheHandle;
    }

    public void doFinalize() {
        objectSubject.disconnectAll();
        objectTypeSubject.disconnectAll();
        selections.clear();
        transformations.clear();
        cacheMaster.clear();
        if (!accessStatus.isExpired()) {
            accessStatus.expired();
        }
    }

    public ObjectTypeSubject<V> getObjectTypeSubject() {
        return objectTypeSubject;
    }

    public ObjectSubject<K, V> getObjectSubject() {
        return objectSubject;
    }

    public ObjectLambdaSubject<K, V> getObjectLambdaSubject() {
        return objectLambdaSubject;
    }

    public MementoValueSubject<Map.Entry<K, V>> getMementoSubject() {
        return mementoSubject;
    }

    public CacheMaster getCacheMaster() {
        return cacheMaster.get();
    }

    public CacheHandle getCacheHandle() {
        return cacheHandle;
    }

    public AccessStatus getAccessStatus() {
        return accessStatus;
    }

    public Map<Object, RxFilter<K, V>> getSelections() {
        return selections;
    }

    public Map<Object, RxFilter<K, V>> getTransformations() {
        return transformations;
    }

    public void doExpire() {
        accessStatus.expired();
    }

    public boolean isExpired() {
        return accessStatus.isExpired();
    }
}
