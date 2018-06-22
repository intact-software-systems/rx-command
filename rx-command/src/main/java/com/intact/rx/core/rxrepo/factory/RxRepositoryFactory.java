package com.intact.rx.core.rxrepo.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxContext;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.Strategy2;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxrepo.*;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.cache.CacheReaderWriter;
import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.cache.factory.CacheFactory;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.rxcache.Streamer;
import com.intact.rx.core.rxcache.StreamerOnlyCacheAccess;
import com.intact.rx.core.rxcache.act.ActPolicy;
import com.intact.rx.core.rxcache.controller.ActsControllerConfig;
import com.intact.rx.core.rxcache.controller.ActsControllerPolicy;
import com.intact.rx.core.rxcache.noop.StreamerNoOp;
import com.intact.rx.core.rxrepo.RepositoryConfig;
import com.intact.rx.core.rxrepo.RequestCache;
import com.intact.rx.core.rxrepo.reader.*;
import com.intact.rx.core.rxrepo.writer.RepositoryStreamWriter;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.templates.Pair;

@SuppressWarnings({"WeakerAccess", "PackageVisibleField"})
public class RxRepositoryFactory<K, V> {
    final RxConfig rxConfig;
    final RepositoryConfig repositoryConfig;

    private final RequestCache<RxStreamer> requestCache;
    private final RxSubjectCombi<Map<K, V>> subject;

    public RxRepositoryFactory(RxConfig rxConfig, RepositoryConfig repositoryConfig, RequestCache<RxStreamer> requestCache, RxSubjectCombi<Map<K, V>> subject) {
        this.rxConfig = rxConfig;
        this.repositoryConfig = repositoryConfig;
        this.requestCache = requestCache;
        this.subject = subject;
    }

    // --------------------------------------------------------------
    // RxCache functions
    // --------------------------------------------------------------

    public void clearCache(MasterCacheId masterCacheId, Class<V> cachedType) {
        Pair<CacheMaster, DataCache<Object, Object>> cachePair = cacheDomain().findCache(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType));
        cachePair.first().ifPresent(CacheMaster::clearAll);
    }

    public boolean clearCachedTypeFrom(MasterCacheId masterCacheId, Class<V> cachedType) {
        CacheMaster cacheMaster = cacheDomain().findCacheMaster(masterCacheId);
        return cacheMaster != null
                && cacheMaster.removeDataCache(DataCacheId.create(cachedType, masterCacheId));
    }

    public <RequestKey> void clear(RequestKey requestKey, MasterCacheId masterCacheId) {
        cacheDomain().clear(masterCacheId);
        requestCache.remove(requestKey);
    }

    public <RequestKey> void clear(RequestKey requestKey, CacheHandle cacheHandle) {
        Pair<CacheMaster, DataCache<Object, Object>> cachePair = cacheDomain().findCache(cacheHandle);
        cachePair.first().ifPresent(CacheMaster::clearAll);
        requestCache.remove(requestKey);
    }

    public void clearSingleton(Class<V> cachedType) {
        requestCache.cache().readAsList().forEach(streamer -> streamer.clearCache(cachedType));
        requestCache.clearAll();
    }

    public <K1, T> RxCache<K1, T> cache(MasterCacheId masterCacheId, Class<T> cachedType) {
        return cacheDomain().computeCacheIfAbsent(masterCacheId, DataCacheId.create(cachedType, masterCacheId), rxConfig.actPolicy.getCachePolicy());
    }

    public Optional<RxCache<K, V>> findCache(CacheHandle cacheHandle) {
        Pair<CacheMaster, DataCache<K, V>> pair = cacheDomain().findCache(cacheHandle);
        DataCache<K, V> dataCache = pair.second().orElse(null);
        return dataCache != null
                ? Optional.of(new CacheReaderWriter<>(() -> dataCache))
                : Optional.empty();
    }

    private CacheFactory cacheDomain() {
        return RxCacheAccess.computeIfAbsent(rxConfig.domainCacheId);
    }

    // --------------------------------------------------------------
    // Create repository readers and writers
    // --------------------------------------------------------------


    public RxRepositoryReaderCollection<K, V> createRepositoryReaderCollection(MasterCacheId masterCacheId, Class<V> cachedType, Map<K, Strategy0<RxStreamer>> creators) {
        Map<K, RxRepositoryReader<K, V>> readers = new HashMap<>();

        creators.forEach(
                (K requestKey, Strategy0<RxStreamer> factory) -> {
                    RxStreamer streamer = requestCache.computeScopedIfAbsent(requestKey, factory).connect(subject, cachedType);
                    readers.put(
                            requestKey,
                            new RepositoryStreamReader<>(
                                    () -> {
                                        switch (this.repositoryConfig.getReadAccess().perform()) {
                                            case NONE:
                                                return StreamerNoOp.instance;
                                            case CACHE:
                                                return new StreamerOnlyCacheAccess(rxConfig.domainCacheId, streamer.getMasterCacheId(), streamer.getRxConfig().actPolicy.getCachePolicy());
                                            case ALL:
                                        }
                                        return streamer;
                                    },
                                    CacheHandle.create(rxConfig.domainCacheId, streamer.getMasterCacheId(), cachedType)
                            )
                    );
                }
        );
        return new RepositoryStreamReaderCollection<>(readers);
    }

    public RxRepositoryReaderCollection<K, V> createReusableRepositoryReaderCollection(MasterCacheId masterCacheId, Class<V> cachedType, Map<K, Strategy0<RxStreamer>> creators) {
        Map<K, RxRepositoryReader<K, V>> readers = new HashMap<>();

        creators.forEach(
                (K requestKey, Strategy0<RxStreamer> factory) -> {
                    RxStreamer streamer = requestCache.computeReusableIfAbsent(requestKey, factory).connect(subject, cachedType);
                    readers.put(
                            requestKey,
                            new RepositoryStreamReader<>(
                                    () -> {
                                        switch (this.repositoryConfig.getReadAccess().perform()) {
                                            case NONE:
                                                return StreamerNoOp.instance;
                                            case CACHE:
                                                return new StreamerOnlyCacheAccess(rxConfig.domainCacheId, streamer.getMasterCacheId(), streamer.getRxConfig().actPolicy.getCachePolicy());
                                            case ALL:
                                        }
                                        return streamer;
                                    },
                                    CacheHandle.create(rxConfig.domainCacheId, streamer.getMasterCacheId(), cachedType)
                            )
                    );
                }
        );

        return new RepositoryStreamReaderCollection<>(readers);
    }

    public <RequestKey> RxRepositoryReader<K, V> createReusableRequestFor(MasterCacheId masterCacheId, RequestKey requestKey, Strategy0<RxStreamer> creator, Class<V> cachedType) {
        RxStreamer streamer = requestCache.computeReusableIfAbsent(requestKey, creator).connect(subject, cachedType);
        return new RepositoryStreamReader<>(
                () -> {
                    switch (this.repositoryConfig.getReadAccess().perform()) {
                        case NONE:
                            return StreamerNoOp.instance;
                        case CACHE:
                            return new StreamerOnlyCacheAccess(rxConfig.domainCacheId, streamer.getMasterCacheId(), streamer.getRxConfig().actPolicy.getCachePolicy());
                        case ALL:
                    }
                    return streamer;
                },
                CacheHandle.create(rxConfig.domainCacheId, streamer.getMasterCacheId(), cachedType)
        );
    }

    public RxRepositoryReader<K, V> createRepositoryReaderWithStreamer(MasterCacheId masterCacheId, RxStreamer streamer, Class<V> cachedType) {
        return new RepositoryStreamReader<>(
                () -> {
                    switch (this.repositoryConfig.getReadAccess().perform()) {
                        case NONE:
                            return StreamerNoOp.instance;
                        case CACHE:
                            return new StreamerOnlyCacheAccess(rxConfig.domainCacheId, streamer.getMasterCacheId(), streamer.getRxConfig().actPolicy.getCachePolicy());
                        case ALL:
                    }
                    return streamer;
                },
                CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType)
        );
    }

    public RxRepositoryWriter<K, V> createRepositoryWriter(MasterCacheId masterCacheId, Class<V> cachedType, RxContext rxContext, Strategy2<Boolean, K, V> putter, CachePolicy cachePolicy) {
        return new RepositoryStreamWriter<>(
                CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType),
                rxContext,
                rxConfig,
                putter
        );
    }

    public <RequestKey> RxRepositoryReader<K, V> createLazyRepositoryReader(MasterCacheId masterCacheId, Class<V> cachedType, RequestKey requestKey, Strategy0<RxStreamer> factory) {
        requireNonNull(requestKey);
        requireNonNull(factory);

        return new LazyRepositoryStreamReader<>(
                CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType),
                rxConfig.actPolicy.getCachePolicy(),
                rxConfig.faultPolicy,
                () -> requestCache
                        .computeScopedIfAbsent(
                                requestKey,
                                () -> {
                                    try {
                                        return factory.perform();
                                    } catch (RuntimeException e) {
                                        throw new IllegalStateException("Streamer creation failed " + this, e);
                                    }
                                })
                        .connect(subject, cachedType),
                repositoryConfig.getReadAccess()
        );
    }

    public <RequestKey> RxRepositoryReader<K, V> createLazyRepositoryReader(MasterCacheId masterCacheId, Class<V> cachedType, RxContext rxContext, RequestKey requestKey, Strategy0<Map<K, V>> actor) {
        requireNonNull(requestKey);
        requireNonNull(actor);

        return new LazyRepositoryStreamReader<>(
                CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType),
                rxConfig.actPolicy.getCachePolicy(),
                rxConfig.faultPolicy,
                () -> requestCache
                        .computeScopedIfAbsent(
                                requestKey,
                                () -> {
                                    try {
                                        return Streamer
                                                .forCache(masterCacheId)
                                                .withExecuteAround(rxContext)
                                                .withThreadPoolConfig(rxConfig.rxThreadPoolConfig)
                                                .withControllerConfig(
                                                        ActsControllerConfig.builder()
                                                                .withActsControllerPolicy(ActsControllerPolicy.from(rxConfig.actsControllerConfig.getActsControllerPolicy(), repositoryConfig.getCircuitBreakerPolicy(), repositoryConfig.getRateLimiterPolicy()))
                                                                .withCircuitBreakerId(repositoryConfig.getCircuitBreakerId())
                                                                .withRateLimiterId(repositoryConfig.getRateLimiterId())
                                                                .build())
                                                .withActPolicy(rxConfig.actPolicy)
                                                .withCommandConfig(rxConfig.commandConfig)
                                                .withFaultPolicy(rxConfig.faultPolicy)
                                                .withDomainCacheId(rxConfig.domainCacheId)
                                                .build()
                                                .sequential()
                                                .get(cachedType, actor)
                                                .done();
                                    } catch (RuntimeException e) {
                                        throw new IllegalStateException("Streamer creation failed " + this, e);
                                    }
                                })
                        .connect(subject, cachedType),
                repositoryConfig.getReadAccess()
        );
    }

    public <RequestKey> RxValueReader<V> createLazyValueReader(RequestKey requestKey, MasterCacheId masterCacheId, Class<V> cachedType, RxContext rxContext, K key, Strategy1<V, K> actor) {
        requireNonNull(requestKey);
        requireNonNull(key);
        requireNonNull(actor);

        return new LazyValueStreamReader<>(
                CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType),
                rxConfig.actPolicy.getCachePolicy(),
                rxConfig.faultPolicy,
                () -> requestCache
                        .computeScopedIfAbsent(
                                requestKey,
                                () -> {
                                    try {
                                        return Streamer
                                                .forCache(masterCacheId)
                                                .withExecuteAround(rxContext)
                                                .withThreadPoolConfig(rxConfig.rxThreadPoolConfig)
                                                .withControllerConfig(
                                                        ActsControllerConfig.builder()
                                                                .withActsControllerPolicy(ActsControllerPolicy.from(rxConfig.actsControllerConfig.getActsControllerPolicy(), repositoryConfig.getCircuitBreakerPolicy(), repositoryConfig.getRateLimiterPolicy()))
                                                                .withCircuitBreakerId(repositoryConfig.getCircuitBreakerId())
                                                                .withRateLimiterId(repositoryConfig.getRateLimiterId())
                                                                .build())
                                                .withActPolicy(rxConfig.actPolicy)
                                                .withCommandConfig(rxConfig.commandConfig)
                                                .withFaultPolicy(rxConfig.faultPolicy)
                                                .withDomainCacheId(rxConfig.domainCacheId)
                                                .build()
                                                .sequential()
                                                .get(cachedType, () -> Optional.ofNullable(actor.perform(key)).map(value -> Map.of(key, value)).orElse(null))
                                                .done();
                                    } catch (RuntimeException e) {
                                        throw new IllegalStateException("Streamer creation failed " + this, e);
                                    }
                                })
                        .connect(subject, cachedType),
                key,
                repositoryConfig.getReadAccess()
        );
    }

    public <RequestKey> RxLoanedValueReader<V> createLazyLoanedValueReader(RequestKey requestKey, MasterCacheId masterCacheId, Class<V> cachedType, RxContext rxContext, K key, Strategy1<V, K> actor, LoanPolicy loanPolicy) {
        requireNonNull(requestKey);
        requireNonNull(key);
        requireNonNull(actor);
        requireNonNull(loanPolicy);

        return new LazyLoanedValueStreamReader<>(
                CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType),
                rxConfig.actPolicy.getCachePolicy(),
                rxConfig.faultPolicy,
                () -> requestCache
                        .computeScopedIfAbsent(
                                requestKey,
                                () -> {
                                    try {
                                        return Streamer
                                                .forCache(masterCacheId)
                                                .withExecuteAround(rxContext)
                                                .withThreadPoolConfig(rxConfig.rxThreadPoolConfig)
                                                .withControllerConfig(
                                                        ActsControllerConfig.builder()
                                                                .withActsControllerPolicy(ActsControllerPolicy.from(rxConfig.actsControllerConfig.getActsControllerPolicy(), repositoryConfig.getCircuitBreakerPolicy(), repositoryConfig.getRateLimiterPolicy()))
                                                                .withCircuitBreakerId(repositoryConfig.getCircuitBreakerId())
                                                                .withRateLimiterId(repositoryConfig.getRateLimiterId())
                                                                .build())
                                                .withActPolicy(rxConfig.actPolicy)
                                                .withCommandConfig(rxConfig.commandConfig)
                                                .withFaultPolicy(rxConfig.faultPolicy)
                                                .withDomainCacheId(rxConfig.domainCacheId)
                                                .build()
                                                .sequential()
                                                .get(cachedType, () -> Optional.ofNullable(actor.perform(key)).map(value -> Map.of(key, value)).orElse(null))
                                                .done();
                                    } catch (RuntimeException e) {
                                        throw new IllegalStateException("Streamer creation failed " + this, e);
                                    }
                                })
                        .connect(subject, cachedType),
                key,
                loanPolicy,
                repositoryConfig.getReadAccess()

        );
    }

    public RxRepositoryReader<K, V> createSingletonLazyRepositoryReader(MasterCacheId masterCacheId, Class<V> cachedType, Strategy0<RxStreamer> factory) {
        requireNonNull(factory);

        return new LazyRepositoryStreamReader<>(
                CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType),
                rxConfig.actPolicy.getCachePolicy(),
                rxConfig.faultPolicy,
                () -> requestCache
                        .computeSingletonIfAbsent(
                                () -> {
                                    try {
                                        return factory.perform();
                                    } catch (RuntimeException e) {
                                        throw new IllegalStateException("Singleton Streamer creation failed " + this, e);
                                    }
                                })
                        .connect(subject, cachedType),
                repositoryConfig.getReadAccess()
        );
    }

    public RxRepositoryReader<K, V> createLazySingletonRepositoryReader(MasterCacheId masterCacheId, Class<V> cachedType, Strategy0<Map<K, V>> actor) {
        requireNonNull(actor);

        return new LazyRepositoryStreamReader<>(
                CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType),
                rxConfig.actPolicy.getCachePolicy(),
                rxConfig.faultPolicy,
                () -> requestCache
                        .computeSingletonIfAbsent(
                                () -> {
                                    try {
                                        return Streamer
                                                .forCache(masterCacheId)
                                                .withExecuteAround(rxConfig.rxContext)
                                                .withThreadPoolConfig(rxConfig.rxThreadPoolConfig)
                                                .withControllerConfig(
                                                        ActsControllerConfig.builder()
                                                                .withActsControllerPolicy(ActsControllerPolicy.from(rxConfig.actsControllerConfig.getActsControllerPolicy(), repositoryConfig.getCircuitBreakerPolicy(), repositoryConfig.getRateLimiterPolicy()))
                                                                .withCircuitBreakerId(repositoryConfig.getCircuitBreakerId())
                                                                .withRateLimiterId(repositoryConfig.getRateLimiterId())
                                                                .build())
                                                .withActPolicy(rxConfig.actPolicy)
                                                .withCommandConfig(rxConfig.commandConfig)
                                                .withFaultPolicy(rxConfig.faultPolicy)
                                                .withDomainCacheId(rxConfig.domainCacheId)
                                                .build()
                                                .sequential()
                                                .get(cachedType, actor)
                                                .done();
                                    } catch (RuntimeException e) {
                                        throw new IllegalStateException("Singleton Streamer creation failed " + this, e);
                                    }
                                })
                        .connect(subject, cachedType),
                repositoryConfig.getReadAccess()
        );
    }

    public Map<K, RxRepositoryReader<K, V>> computeIfAbsent(MasterCacheId masterCacheId, Class<V> cachedType, ActPolicy actPolicy, CommandPolicy commandPolicy, Iterable<K> keys, Strategy1<V, K> valueComputer) {
        requireNonNull(keys);

        Map<K, RxRepositoryReader<K, V>> readers = new HashMap<>();
        keys.forEach(
                key -> readers.put(
                        key,
                        new LazyRepositoryStreamReader<>(
                                CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType),
                                rxConfig.actPolicy.getCachePolicy(),
                                rxConfig.faultPolicy,
                                () -> requestCache
                                        .computeScopedIfAbsent(
                                                key,
                                                () -> {
                                                    try {
                                                        return Streamer
                                                                .forCache(masterCacheId)
                                                                .withExecuteAround(rxConfig.rxContext)
                                                                .withThreadPoolConfig(rxConfig.rxThreadPoolConfig)
                                                                .withControllerConfig(
                                                                        ActsControllerConfig.builder()
                                                                                .withActsControllerPolicy(ActsControllerPolicy.from(rxConfig.actsControllerConfig.getActsControllerPolicy(), repositoryConfig.getCircuitBreakerPolicy(), repositoryConfig.getRateLimiterPolicy()))
                                                                                .withCircuitBreakerId(repositoryConfig.getCircuitBreakerId())
                                                                                .withRateLimiterId(repositoryConfig.getRateLimiterId())
                                                                                .build())
                                                                .withActPolicy(actPolicy)
                                                                .withCommandPolicy(commandPolicy)
                                                                .withFaultPolicy(rxConfig.faultPolicy)
                                                                .withDomainCacheId(rxConfig.domainCacheId)
                                                                .build()
                                                                .sequential()
                                                                .get(cachedType, () -> Optional.ofNullable(valueComputer.perform(key)).map(value -> Map.of(key, value)).orElse(null))
                                                                .done();
                                                    } catch (RuntimeException e) {
                                                        throw new IllegalStateException("Singleton Streamer creation failed " + this, e);
                                                    }
                                                })
                                        .connect(subject, cachedType),
                                repositoryConfig.getReadAccess()
                        )
                )
        );
        return readers;
    }
}
