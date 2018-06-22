package com.intact.rx.core.rxrepo.factory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxContext;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.Strategy2;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxrepo.*;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.cache.data.id.DomainCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.nullobjects.RxCacheNoOp;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxrepo.RepositoryConfig;
import com.intact.rx.core.rxrepo.RequestCache;
import com.intact.rx.core.rxrepo.noop.*;
import com.intact.rx.core.rxrepo.reader.LoanedValueReaderOfCachedData;
import com.intact.rx.core.rxrepo.reader.RepositoryReaderOfCachedData;
import com.intact.rx.core.rxrepo.reader.RepositoryStreamReaderCollection;
import com.intact.rx.core.rxrepo.reader.ValueReaderOfCachedData;
import com.intact.rx.core.rxrepo.writer.RepositoryWriterToCache;
import com.intact.rx.policy.Access;
import com.intact.rx.policy.LoanPolicy;

@SuppressWarnings("WeakerAccess")
public class RxRepositoryFactoryWithAccessControl<K, V> extends RxRepositoryFactory<K, V> {

    public RxRepositoryFactoryWithAccessControl(RxConfig rxConfig, RepositoryConfig repositoryConfig, RequestCache<RxStreamer> requestCache, RxSubjectCombi<Map<K, V>> subject) {
        super(rxConfig, repositoryConfig, requestCache, subject);
    }

    @Override
    public <RequestKey> void clear(RequestKey requestKey, MasterCacheId masterCacheId) {
        if (isCachedDataAccessible()) {
            super.clear(requestKey, masterCacheId);
        }
    }

    @Override
    public <RequestKey> void clear(RequestKey requestKey, CacheHandle cacheHandle) {
        if (isCachedDataAccessible()) {
            super.clear(requestKey, cacheHandle);
        }
    }

    @Override
    public void clearSingleton(Class<V> cachedType) {
        if (isCachedDataAccessible()) {
            super.clearSingleton(cachedType);
        }
    }

    @Override
    public void clearCache(MasterCacheId masterCacheId, Class<V> cachedType) {
        if (isCachedDataAccessible()) {
            super.clearCache(masterCacheId, cachedType);
        }
    }

    @Override
    public boolean clearCachedTypeFrom(MasterCacheId masterCacheId, Class<V> cachedType) {
        return isCachedDataAccessible() && super.clearCachedTypeFrom(masterCacheId, cachedType);
    }

    @Override
    public <K1, T> RxCache<K1, T> cache(MasterCacheId masterCacheId, Class<T> cachedType) {
        return isCachedDataAccessible()
                ? super.cache(masterCacheId, cachedType)
                : RxCacheNoOp.instance;
    }

    @Override
    public Optional<RxCache<K, V>> findCache(CacheHandle cacheHandle) {
        return isCachedDataAccessible()
                ? super.findCache(cacheHandle)
                : Optional.empty();
    }

    @Override
    public RxRepositoryReaderCollection<K, V> createRepositoryReaderCollection(MasterCacheId masterCacheId, Class<V> cachedType, Map<K, Strategy0<RxStreamer>> creators) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return RepositoryReaderCollectionNoOp.instance;
            case CACHE:
                return createRepositoryReaderCollectionOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, creators, rxConfig.actPolicy.getCachePolicy());
            case ALL:
        }
        return super.createRepositoryReaderCollection(masterCacheId, cachedType, creators);
    }

    @Override
    public RxRepositoryReaderCollection<K, V> createReusableRepositoryReaderCollection(MasterCacheId masterCacheId, Class<V> cachedType, Map<K, Strategy0<RxStreamer>> creators) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return RepositoryReaderCollectionNoOp.instance;
            case CACHE:
                return createRepositoryReaderCollectionOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, creators, rxConfig.actPolicy.getCachePolicy());
            case ALL:
        }
        return super.createReusableRepositoryReaderCollection(masterCacheId, cachedType, creators);
    }


    @Override
    public <RequestKey> RxRepositoryReader<K, V> createReusableRequestFor(MasterCacheId masterCacheId, RequestKey requestKey, Strategy0<RxStreamer> creator, Class<V> cachedType) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return RepositoryReaderNoOp.instance;
            case CACHE:
                return createRepositoryReaderOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, rxConfig.actPolicy.getCachePolicy());
            case ALL:
        }
        return super.createReusableRequestFor(masterCacheId, requestKey, creator, cachedType);
    }

    @Override
    public RxRepositoryWriter<K, V> createRepositoryWriter(MasterCacheId masterCacheId, Class<V> cachedType, RxContext rxContext, Strategy2<Boolean, K, V> putter, CachePolicy cachePolicy) {
        switch (getWriteAccess()) {
            case NONE:
                //noinspection unchecked
                return RepositoryWriterNoOp.instance;
            case CACHE:
                return createRepositoryWriterToCache(rxConfig.domainCacheId, masterCacheId, cachedType, cachePolicy);
            case ALL:
        }
        return super.createRepositoryWriter(masterCacheId, cachedType, rxContext, putter, cachePolicy);
    }

    @Override
    public RxRepositoryReader<K, V> createRepositoryReaderWithStreamer(MasterCacheId masterCacheId, RxStreamer streamer, Class<V> cachedType) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return RepositoryReaderNoOp.instance;
            case CACHE:
                return createRepositoryReaderOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, streamer.getRxConfig().actPolicy.getCachePolicy());
            case ALL:
        }
        return super.createRepositoryReaderWithStreamer(masterCacheId, streamer, cachedType);
    }

    @Override
    public <RequestKey> RxRepositoryReader<K, V> createLazyRepositoryReader(MasterCacheId masterCacheId, Class<V> cachedType, RequestKey requestKey, Strategy0<RxStreamer> factory) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return RepositoryReaderNoOp.instance;
            case CACHE:
                return createRepositoryReaderOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, rxConfig.actPolicy.getCachePolicy());
            case ALL:
        }
        return super.createLazyRepositoryReader(masterCacheId, cachedType, requestKey, factory);
    }

    @Override
    public <RequestKey> RxRepositoryReader<K, V> createLazyRepositoryReader(MasterCacheId masterCacheId, Class<V> cachedType, RxContext rxContext, RequestKey requestKey, Strategy0<Map<K, V>> actor) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return RepositoryReaderNoOp.instance;
            case CACHE:
                return createRepositoryReaderOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, rxConfig.actPolicy.getCachePolicy());
            case ALL:
        }
        return super.createLazyRepositoryReader(masterCacheId, cachedType, rxContext, requestKey, actor);
    }

    @Override
    public <RequestKey> RxValueReader<V> createLazyValueReader(RequestKey requestKey, MasterCacheId masterCacheId, Class<V> cachedType, RxContext rxContext, K key, Strategy1<V, K> actor) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return ValueReaderNoOp.instance;
            case CACHE:
                return createValueReaderOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, key, rxConfig.actPolicy.getCachePolicy());
            case ALL:
        }
        return super.createLazyValueReader(requestKey, masterCacheId, cachedType, rxContext, key, actor);
    }

    @Override
    public <RequestKey> RxLoanedValueReader<V> createLazyLoanedValueReader(RequestKey requestKey, MasterCacheId masterCacheId, Class<V> cachedType, RxContext rxContext, K key, Strategy1<V, K> actor, LoanPolicy loanPolicy) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return LoanedValueReaderNoOp.instance;
            case CACHE:
                return createLoanedValueReaderOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, key, rxConfig.actPolicy.getCachePolicy(), loanPolicy);
            case ALL:
        }
        return super.createLazyLoanedValueReader(requestKey, masterCacheId, cachedType, rxContext, key, actor, loanPolicy);
    }

    @Override
    public RxRepositoryReader<K, V> createSingletonLazyRepositoryReader(MasterCacheId masterCacheId, Class<V> cachedType, Strategy0<RxStreamer> factory) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return RepositoryReaderNoOp.instance;
            case CACHE:
                return createRepositoryReaderOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, rxConfig.actPolicy.getCachePolicy());
            case ALL:
        }
        return super.createSingletonLazyRepositoryReader(masterCacheId, cachedType, factory);
    }

    @Override
    public RxRepositoryReader<K, V> createLazySingletonRepositoryReader(MasterCacheId masterCacheId, Class<V> cachedType, Strategy0<Map<K, V>> actor) {
        switch (getReadAccess()) {
            case NONE:
                //noinspection unchecked
                return RepositoryReaderNoOp.instance;
            case CACHE:
                return createRepositoryReaderOfCachedData(rxConfig.domainCacheId, masterCacheId, cachedType, rxConfig.actPolicy.getCachePolicy());
            case ALL:
        }
        return super.createLazySingletonRepositoryReader(masterCacheId, cachedType, actor);
    }

    @Override
    public Map<K, RxRepositoryReader<K, V>> computeIfAbsent(MasterCacheId masterCacheId, Class<V> cachedType, ActPolicy actPolicy, CommandPolicy commandPolicy, Iterable<K> keys, Strategy1<V, K> valueComputer) {
        switch (getReadAccess()) {
            case NONE:
                return Collections.emptyMap();
            case CACHE:
                return createRepositoryReaderOfCachedDataMap(rxConfig.domainCacheId, masterCacheId, cachedType, keys, rxConfig.actPolicy.getCachePolicy());
            case ALL:
        }
        return super.computeIfAbsent(masterCacheId, cachedType, actPolicy, commandPolicy, keys, valueComputer);
    }

    // --------------------------------------------
    // private factory functions
    // --------------------------------------------

    private static <K, V> RxRepositoryReaderCollection<K, V> createRepositoryReaderCollectionOfCachedData(DomainCacheId domainCacheId, MasterCacheId masterCacheId, Class<V> cachedType, Map<K, Strategy0<RxStreamer>> creators, CachePolicy cachePolicy) {
        return new RepositoryStreamReaderCollection<>(createRepositoryReaderMap(domainCacheId, masterCacheId, cachedType, creators, cachePolicy));
    }

    private static <K, V> Map<K, RxRepositoryReader<K, V>> createRepositoryReaderMap(DomainCacheId domainCacheId, MasterCacheId masterCacheId, Class<V> cachedType, Map<K, Strategy0<RxStreamer>> creators, CachePolicy cachePolicy) {
        Map<K, RxRepositoryReader<K, V>> readers = new HashMap<>();
        creators.forEach((requestKey, factory) -> readers.put(requestKey, createRepositoryReaderOfCachedData(domainCacheId, masterCacheId, cachedType, cachePolicy)));
        return readers;
    }

    private static <K, V> Map<K, RxRepositoryReader<K, V>> createRepositoryReaderOfCachedDataMap(DomainCacheId domainCacheId, MasterCacheId masterCacheId, Class<V> cachedType, Iterable<K> keys, CachePolicy cachePolicy) {
        Map<K, RxRepositoryReader<K, V>> readers = new HashMap<>();
        keys.forEach(key -> readers.put(key, createRepositoryReaderOfCachedData(domainCacheId, masterCacheId, cachedType, cachePolicy)));
        return readers;
    }

    private static <K, V> RxLoanedValueReader<V> createLoanedValueReaderOfCachedData(DomainCacheId domainCacheId, MasterCacheId masterCacheId, Class<V> cachedType, K key, CachePolicy cachePolicy, LoanPolicy loanPolicy) {
        return new LoanedValueReaderOfCachedData<>(CacheHandle.create(domainCacheId, masterCacheId, cachedType), cachePolicy, key, loanPolicy);
    }

    private static <K, V> RxRepositoryReader<K, V> createRepositoryReaderOfCachedData(DomainCacheId domainCacheId, MasterCacheId masterCacheId, Class<V> cachedType, CachePolicy cachePolicy) {
        return new RepositoryReaderOfCachedData<>(CacheHandle.create(domainCacheId, masterCacheId, cachedType), cachePolicy);
    }

    private static <K, V> RxValueReader<V> createValueReaderOfCachedData(DomainCacheId domainCacheId, MasterCacheId masterCacheId, Class<V> cachedType, K key, CachePolicy cachePolicy) {
        return new ValueReaderOfCachedData<>(CacheHandle.create(domainCacheId, masterCacheId, cachedType), cachePolicy, key);
    }

    private static <K, V> RxRepositoryWriter<K, V> createRepositoryWriterToCache(DomainCacheId domainCacheId, MasterCacheId masterCacheId, Class<V> cachedType, CachePolicy cachePolicy) {
        return new RepositoryWriterToCache<>(cachePolicy, CacheHandle.create(domainCacheId, masterCacheId, cachedType));
    }

    // --------------------------------------------
    // access checkers
    // --------------------------------------------

    private boolean isCachedDataAccessible() {
        Access access = Optional.ofNullable(repositoryConfig.getReadAccess().perform()).orElse(Access.ALL);
        return access.equals(Access.ALL) || access.equals(Access.CACHE);
    }

    private Access getReadAccess() {
        return Optional.ofNullable(repositoryConfig.getReadAccess().perform()).orElse(Access.ALL);
    }

    private Access getWriteAccess() {
        return Optional.ofNullable(repositoryConfig.getWriteAccess().perform()).orElse(Access.ALL);
    }
}
