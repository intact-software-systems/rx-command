package com.intact.rx.core.rxrepo;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.*;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.Strategy2;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxrepo.*;
import com.intact.rx.api.subject.RxKeyValueSubject;
import com.intact.rx.api.subject.RxKeyValueSubjectCombi;
import com.intact.rx.api.subject.RxSubject;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.command.CommandControllerPolicy;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.core.command.factory.CommandFactory;
import com.intact.rx.core.rxcache.controller.ActsSingleExecutionPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreaker;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerCache;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.rate.RateLimiter;
import com.intact.rx.core.rxcircuit.rate.RateLimiterCache;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.core.rxrepo.factory.RxRepositoryFactory;
import com.intact.rx.core.rxrepo.factory.RxRepositoryFactoryWithAccessControl;
import com.intact.rx.core.rxrepo.reader.RepositoryStreamReaderCollection;
import com.intact.rx.policy.Access;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.templates.RxContexts;
import com.intact.rx.templates.Validate;

/**
 * CachedRepository creates Streamer objects to execute functions and store the result in a cache.
 * <p>
 * Any Streamer should only be cached as long as the "shortest lifespan of any objects in its lambda functions"
 */
public class CachedRepository<K, V> implements RxRepository<K, V> {
    private final CommandController<Void> tasksController;
    private final MasterCacheId masterCacheId;
    private final Class<V> cachedType;
    private final RequestCache<RxStreamer> requestCache;
    private final RepositoryConfig repositoryConfig;
    private final RxConfig rxConfig;
    private final RxSubjectCombi<Map<K, V>> subject;
    private final RxRepositoryFactory<K, V> factory;

    private CachedRepository(Class<V> cachedType, RequestCache<RxStreamer> requestCache) {
        this(MasterCacheId.uuid(),
                cachedType,
                RepositoryConfig.fromRxDefault(),
                requestCache,
                RxConfig.fromRxDefault());
    }

    private CachedRepository(
            MasterCacheId masterCacheId,
            Class<V> cachedType,
            RepositoryConfig repositoryConfig,
            RequestCache<RxStreamer> requestCache,
            RxConfig rxConfig) {
        this.tasksController = CommandFactory.createController(CommandControllerPolicy.parallel());
        this.masterCacheId = requireNonNull(masterCacheId);
        this.cachedType = requireNonNull(cachedType);
        this.requestCache = requireNonNull(requestCache);
        this.repositoryConfig = requireNonNull(repositoryConfig);
        this.rxConfig = requireNonNull(rxConfig);
        this.subject = new RxSubjectCombi<>();
        this.factory =
                repositoryConfig.isAllowAll()
                        ? new RxRepositoryFactory<>(rxConfig, repositoryConfig, requestCache, subject)
                        : new RxRepositoryFactoryWithAccessControl<>(rxConfig, repositoryConfig, requestCache, subject);

        Validate.assertTrue(masterCacheId.isValid(), "MasterCache id must be valid");
    }

    // --------------------------------------------
    // Factories
    // --------------------------------------------

    public static <RequestScope, K, V> CachedRepository<K, V> withSharedRequestsByScope(Class<V> cachedType, RequestScope requestScope, CachePolicy requestCachePolicy) {
        return new CachedRepository<>(cachedType, RequestCache.withSharedRequestsByScope(RxStreamer.class, requestScope, requestCachePolicy));
    }

    public static <RequestScope, K, V> CachedRepository<K, V> withSharedRequestsByScope(Class<V> cachedType, RequestScope requestScope) {
        return new CachedRepository<>(cachedType, RequestCache.withSharedRequestsByScope(RxStreamer.class, requestScope, RxDefault.getDefaultRequestCachePolicy()));
    }

    public static <K, V> CachedRepository<K, V> withSharedRequestsByType(Class<V> cachedType, CachePolicy requestCachePolicy) {
        return new CachedRepository<>(cachedType, RequestCache.withSharedRequestsByScope(RxStreamer.class, cachedType, requestCachePolicy));
    }

    public static <K, V> CachedRepository<K, V> withIsolatedRequests(Class<V> cachedType, CachePolicy requestCachePolicy) {
        return new CachedRepository<>(cachedType, RequestCache.withIsolatedRequests(RxStreamer.class, requestCachePolicy));
    }

    public static <K, V> CachedRepository<K, V> withIsolatedRequests(Class<V> cachedType) {
        return new CachedRepository<>(cachedType, RequestCache.withIsolatedRequests(RxStreamer.class, RxDefault.getDefaultRequestCachePolicy()));
    }

    // --------------------------------------------
    // Builder with fluent API
    // --------------------------------------------

    public static <K, V> Builder<K, V> forCache(Object cacheId, Class<V> cachedType) {
        return new Builder<>(MasterCacheId.create(cacheId), cachedType);
    }

    public static <K, V> Builder<K, V> forCache(MasterCacheId masterCacheId, Class<V> cachedType) {
        return new Builder<>(masterCacheId, cachedType);
    }

    public static <K, V> Builder<K, V> forType(Class<V> cachedType) {
        return new Builder<>(MasterCacheId.uuid(), cachedType);
    }

    public static class Builder<K, V> extends RxConfigBuilder<Builder<K, V>> {
        private final MasterCacheId masterCacheId;
        private final Class<V> cachedType;

        private final RxKeyValueSubject<ValueHandle<?>, RxStreamer> requestObservers = new RxKeyValueSubject<>();
        private final RxSubject<Map<K, V>> dataObservers = new RxSubject<>();

        private RepositoryConfig.Builder repositoryConfigBuilder = RepositoryConfig.buildFrom(RepositoryConfig.fromRxDefault());
        private RequestCache<RxStreamer> requestCache = RequestCache.withIsolatedRequests(RxStreamer.class, RxDefault.getDefaultRequestCachePolicy());

        private final State state;

        private Builder(MasterCacheId masterCacheId, Class<V> cachedType) {
            this.masterCacheId = requireNonNull(masterCacheId);
            this.cachedType = requireNonNull(cachedType);
            //noinspection ThisEscapedInObjectConstruction
            this.state = new State(this);
        }

        @Override
        protected State state() {
            return state;
        }

        public Builder<K, V> withControllerPolicy(ActsSingleExecutionPolicy actsControllerPolicy) {
            requireNonNull(actsControllerPolicy);
            state().actsControllerConfigBuilder.withActsControllerPolicy(actsControllerPolicy.toActsControllerPolicy());
            return this;
        }

        public <RequestScope> Builder<K, V> withSharedRequestsByScope(RequestScope requestScope, CachePolicy requestCachePolicy) {
            requestCache = RequestCache.withSharedRequestsByScope(RxStreamer.class, requestScope, requestCachePolicy);
            return this;
        }

        public <RequestScope> Builder<K, V> withSharedRequestsByScope(RequestScope requestScope) {
            requestCache = RequestCache.withSharedRequestsByScope(RxStreamer.class, requestScope, RxDefault.getDefaultRequestCachePolicy());
            return this;
        }

        public Builder<K, V> withIsolatedRequests(CachePolicy requestCachePolicy) {
            requestCache = RequestCache.withIsolatedRequests(RxStreamer.class, requestCachePolicy);
            return this;
        }

        public Builder<K, V> withIsolatedRequests() {
            requestCache = RequestCache.withIsolatedRequests(RxStreamer.class, RxDefault.getDefaultRequestCachePolicy());
            return this;
        }

        public Builder<K, V> withRepositoryConfig(RepositoryConfig repositoryConfig) {
            this.repositoryConfigBuilder = RepositoryConfig.buildFrom(repositoryConfig);
            return this;
        }

        public Builder<K, V> withRepositoryReadAccessControl(Strategy0<Access> accessControl) {
            repositoryConfigBuilder.withReadAccessControl(accessControl);
            return this;
        }

        public Builder<K, V> withRepositoryWriteAccessControl(Strategy0<Access> accessControl) {
            repositoryConfigBuilder.withWriteAccessControl(accessControl);
            return this;
        }

        public Builder<K, V> withRepositoryCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) {
            this.repositoryConfigBuilder.withCircuitBreakerPolicy(circuitBreakerPolicy);
            return this;
        }

        public Builder<K, V> withRepositoryCircuitBreakerIsolatedById(Object circuitBreakerId) {
            this.repositoryConfigBuilder.withCircuitBreakerIsolatedById(circuitBreakerId);
            return this;
        }

        public Builder<K, V> withRepositoryCircuitBreakerSharedById(Object circuitBreakerId) {
            this.repositoryConfigBuilder.withCircuitBreakerSharedById(circuitBreakerId);
            return this;
        }

        public Builder<K, V> withRepositoryCircuitBreakerIdAsUUID() {
            this.repositoryConfigBuilder.withCircuitBreakerIsolatedById(UUID.randomUUID());
            return this;
        }

        public Builder<K, V> withRepositoryRateLimiterPolicy(RateLimiterPolicy rateLimiterPolicy) {
            this.repositoryConfigBuilder.withRateLimiterPolicy(rateLimiterPolicy);
            return this;
        }

        public Builder<K, V> withRepositoryRateLimiterIsolatedById(Object rateLimiterId) {
            this.repositoryConfigBuilder.withRateLimiterIsolatedById(rateLimiterId);
            return this;
        }

        public Builder<K, V> withRepositoryRateLimiterSharedById(Object rateLimiterId) {
            this.repositoryConfigBuilder.withRateLimiterSharedById(rateLimiterId);
            return this;
        }

        public Builder<K, V> withRepositoryRateLimiterIdAsUUID() {
            this.repositoryConfigBuilder.withCircuitBreakerIsolatedById(UUID.randomUUID());
            return this;
        }

        public Builder<K, V> withRequestObserver(RxKeyValueObserver<ValueHandle<?>, RxStreamer> observer) {
            this.requestObservers.connect(observer);
            return this;
        }

        public Builder<K, V> withDataObserver(RxObserver<Map<K, V>> observer) {
            this.dataObservers.connect(observer);
            return this;
        }

        public CachedRepository<K, V> build() {
            final CachedRepository<K, V> repository = new CachedRepository<>(
                    masterCacheId,
                    cachedType,
                    repositoryConfigBuilder.build(),
                    requestCache,
                    buildRxConfig());

            requestObservers.observers().forEach(observer -> repository.observeRequests().connect(observer));
            dataObservers.observers().forEach(observer -> repository.observeData().connect(observer));
            return repository;
        }
    }

    // --------------------------------------------
    // Getters
    // --------------------------------------------

    @Override
    public RequestCache<RxStreamer> getRequestCache() {
        return requestCache;
    }

    @Override
    public MasterCacheId getMasterCacheId() {
        return masterCacheId;
    }

    @Override
    public Class<V> getCachedType() {
        return cachedType;
    }

    @Override
    public RxConfig getRxConfig() {
        return rxConfig;
    }

    @Override
    public RepositoryConfig getRepositoryConfig() {
        return repositoryConfig;
    }

    @Override
    public RxSubjectCombi<Map<K, V>> observeData() {
        return subject;
    }

    @Override
    public CircuitBreaker circuit() {
        return CircuitBreakerCache.circuit(repositoryConfig.getCircuitBreakerId(), repositoryConfig.getCircuitBreakerPolicy());
    }

    @Override
    public RateLimiter rateLimiter() {
        return RateLimiterCache.rateLimiter(repositoryConfig.getRateLimiterId(), repositoryConfig.getRateLimiterPolicy());
    }

    @Override
    public RxKeyValueSubjectCombi<ValueHandle<?>, RxStreamer> observeRequests() {
        return requestCache.observe();
    }

    // --------------------------------------------
    // Repository tasks
    // --------------------------------------------

    @Override
    public Optional<Command<Void>> addTask(CommandPolicy policy, VoidStrategy0 strategy) {
        Command<Void> command = CommandFactory.createCommand(policy, strategy);
        return tasksController.addCommand(command)
                ? Optional.of(command)
                : Optional.empty();
    }

    @Override
    public Optional<Command<Void>> addTask(CommandPolicy policy, Strategy0<Boolean> executeCondition, VoidStrategy0 strategy) {
        Command<Void> command = CommandFactory.createCommand(policy, executeCondition, strategy);
        return tasksController.addCommand(command)
                ? Optional.of(command)
                : Optional.empty();
    }

    @Override
    public void startTasks() {
        tasksController.subscribe();
    }

    // --------------------------------------------
    // Lifecycle API
    // --------------------------------------------

    @Override
    public void clearCache() {
        factory.clearCache(masterCacheId, cachedType);
    }

    @Override
    public boolean clearCachedTypeFrom(MasterCacheId masterCacheId) {
        return factory.clearCachedTypeFrom(masterCacheId, cachedType);
    }

    @Override
    public <RequestKey> void clear(RequestKey requestKey, MasterCacheId masterCacheId) {
        factory.clear(requestKey, masterCacheId);
    }

    @Override
    public <RequestKey> void clear(RequestKey requestKey, CacheHandle cacheHandle) {
        factory.clear(requestKey, cacheHandle);
    }

    @Override
    public void clearSingleton(Class<V> cachedType) {
        factory.clearSingleton(cachedType);
    }

    // --------------------------------------------
    // Convenience methods
    // --------------------------------------------

    @Override
    public <RequestKey> Optional<RxRepositoryReader<K, V>> find(RequestKey requestKey) {
        RxStreamer streamer = requestCache.find(requestKey).orElse(null);
        return streamer != null
                ? Optional.of(factory.createRepositoryReaderWithStreamer(masterCacheId, streamer, cachedType))
                : Optional.empty();
    }

    @Override
    public <RequestKey> Optional<RxRepositoryReader<K, V>> remove(RequestKey requestKey) {
        RxStreamer streamer = requestCache.remove(requestKey).orElse(null);
        return streamer != null
                ? Optional.of(factory.createRepositoryReaderWithStreamer(masterCacheId, streamer, cachedType))
                : Optional.empty();
    }

    @Override
    public <RequestKey> Optional<RxRepositoryReader<K, V>> replace(RequestKey requestKey, Strategy0<RxStreamer> creator) {
        RxStreamer streamer = requestCache.replace(requestKey, creator).orElse(null);
        return streamer != null
                ? Optional.of(factory.createRepositoryReaderWithStreamer(masterCacheId, streamer, cachedType))
                : Optional.empty();
    }

    @Override
    public Optional<RxRepositoryReader<K, V>> replaceSingleton(Strategy0<RxStreamer> factory) {
        RxStreamer streamer = requestCache.replaceSingleton(factory).orElse(null);
        return streamer != null
                ? Optional.of(this.factory.createRepositoryReaderWithStreamer(masterCacheId, streamer, cachedType))
                : Optional.empty();
    }

    @Override
    public Optional<RxCache<K, V>> findDefaultCache() {
        return factory.findCache(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType));
    }

    @Override
    public Optional<RxCache<K, V>> findCache(MasterCacheId masterCacheId) {
        return factory.findCache(CacheHandle.create(rxConfig.domainCacheId, masterCacheId, cachedType));
    }

    @Override
    public Optional<RxCache<K, V>> findCache(CacheHandle cacheHandle) {
        return factory.findCache(cacheHandle);
    }

    @Override
    public <K1, T> RxCache<K1, T> cache(Class<T> cachedType) {
        return factory.cache(masterCacheId, cachedType);
    }

    @Override
    public RxCache<K, V> cache() {
        return factory.cache(masterCacheId, cachedType);
    }

    // --------------------------------------------
    // Interface RepositoryAccess - one lambda
    // --------------------------------------------

    @Override
    public Optional<V> computeIfAbsent(K key, Strategy1<V, K> getter) {
        return requestValueFor(key, getter).computeIfAbsent(rxConfig.clientTimeout.toMillis());
    }

    @Override
    public Optional<V> computeIf(K key, Strategy1<Boolean, V> computeResolver, Strategy1<V, K> getter) {
        return requestValueFor(key, getter).computeIf(computeResolver, rxConfig.clientTimeout.toMillis());
    }

    @Override
    public V computeValueIfAbsent(K key, Strategy1<V, K> getter) {
        return requestValueFor(key, getter).computeValueIfAbsent(rxConfig.clientTimeout.toMillis());
    }

    @Override
    public V computeValueIf(K key, Strategy1<Boolean, V> computeResolver, Strategy1<V, K> getter) {
        return requestValueFor(key, getter).computeValueIf(computeResolver, rxConfig.clientTimeout.toMillis());
    }

    @Override
    public Map<K, V> computeIfAbsent(Iterable<K> keys, Strategy1<V, K> getter) {
        return requestAllFor(keys, getter).computeIfEmpty(rxConfig.clientTimeout.toMillis());
    }

    @Override
    public Optional<V> compute(K key, Strategy1<V, K> getter) {
        return requestValueFor(key, getter).compute(rxConfig.clientTimeout.toMillis());
    }

    @Override
    public V computeValue(K key, Strategy1<V, K> getter) {
        return requestValueFor(key, getter).computeValue(rxConfig.clientTimeout.toMillis());
    }

    @Override
    public Map<K, V> compute(Iterable<K> keys, Strategy1<V, K> getter) {
        return requestAllFor(keys, getter).computeIfEmpty(rxConfig.clientTimeout.toMillis());
    }

    @Override
    public boolean put(K key, V value, Strategy2<Boolean, K, V> putter) {
        return writeRequestFor(putter).put(key, value, rxConfig.clientTimeout.toMillis());
    }

    @Override
    public boolean putAndRemoveLocal(K key, V value, Strategy2<Boolean, K, V> putter) {
        return writeRequestFor(putter).putAndRemoveLocal(key, value, rxConfig.clientTimeout.toMillis());
    }

    @Override
    public Optional<V> putAndGet(K key, V value, Strategy2<Boolean, K, V> putter) {
        return writeRequestFor(putter).putAndGet(key, value, rxConfig.clientTimeout.toMillis());
    }

    @Override
    public boolean putIfAbsent(K key, V value, Strategy2<Boolean, K, V> putter) {
        return writeRequestFor(putter).putIfAbsent(key, value, rxConfig.clientTimeout.toMillis());
    }

    // --------------------------------------------
    // Repository request status
    // --------------------------------------------

    @Override
    public <RequestKey> Optional<RepositoryRequestStatus> findRequestStatus(RequestKey requestKey) {
        RxStreamer streamer = requestCache.find(requestKey).orElse(null);
        return streamer != null
                ? Optional.of(RepositoryRequestStatus.create(streamer.getStatus()))
                : Optional.empty();
    }

    // --------------------------------------------
    // Create requests and return repository reader
    // --------------------------------------------

    @Override
    public <RequestKey> RxRepositoryReader<K, V> requestFor(RequestKey requestKey, Strategy0<RxStreamer> creator) {
        return factory.createLazyRepositoryReader(masterCacheId, cachedType, requestKey, creator);
    }

    @Override
    public RxRepositoryReader<K, V> uuidRequestFor(Strategy0<RxStreamer> creator) {
        return factory.createLazyRepositoryReader(masterCacheId, cachedType, UUID.randomUUID(), creator);
    }

    @Override
    public RxRepositoryReaderCollection<K, V> requestAllFor(Map<K, Strategy0<RxStreamer>> creators) {
        return factory.createRepositoryReaderCollection(masterCacheId, cachedType, creators);
    }

    @Override
    public RxRepositoryReaderCollection<K, V> requestAllFor(Iterable<K> keys, Strategy1<V, K> computer) {
        return new RepositoryStreamReaderCollection<>(factory.computeIfAbsent(masterCacheId, cachedType, rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), keys, computer));
    }

    @Override
    public RxRepositoryReaderCollection<K, V> requestAllFor(MasterCacheId masterCacheId, Iterable<K> keys, Strategy1<V, K> computer) {
        return new RepositoryStreamReaderCollection<>(factory.computeIfAbsent(masterCacheId, cachedType, rxConfig.actPolicy, rxConfig.commandConfig.getCommandPolicy(), keys, computer));
    }

    @Override
    public <RequestKey> RxRepositoryReader<K, V> reusableRequestFor(RequestKey requestKey, Strategy0<RxStreamer> creator) {
        return factory.createReusableRequestFor(masterCacheId, requestKey, creator, cachedType);
    }

    @Override
    public RxRepositoryReaderCollection<K, V> reusableRequestAllFor(Map<K, Strategy0<RxStreamer>> creators) {
        return factory.createReusableRepositoryReaderCollection(masterCacheId, cachedType, creators);
    }

    // --------------------------------------------
    // Singletons
    // --------------------------------------------

    @Override
    public RxRepositoryReader<K, V> singletonRequestFor(Strategy0<RxStreamer> creator) {
        return factory.createSingletonLazyRepositoryReader(masterCacheId, cachedType, creator);
    }

    @Override
    public RxRepositoryReader<K, V> singletonRequestActorFor(Strategy0<Map<K, V>> actor) {
        return factory.createLazySingletonRepositoryReader(masterCacheId, cachedType, actor);
    }

    // --------------------------------------------
    // Create repository writer
    // --------------------------------------------

    @Override
    public RxRepositoryWriter<K, V> writeRequestFor(Strategy2<Boolean, K, V> putter) {
        return factory.createRepositoryWriter(masterCacheId, cachedType, rxConfig.rxContext, putter, rxConfig.actPolicy.getCachePolicy());
    }

    @Override
    public RxRepositoryWriter<K, V> writeRequestFor(MasterCacheId masterCacheId, Strategy2<Boolean, K, V> putter) {
        return factory.createRepositoryWriter(masterCacheId, cachedType, rxConfig.rxContext, putter, rxConfig.actPolicy.getCachePolicy());
    }

    @Override
    public RxRepositoryWriter<K, V> writeRequestFor(MasterCacheId masterCacheId, RxContext rxContext, Strategy2<Boolean, K, V> putter) {
        return factory.createRepositoryWriter(masterCacheId, cachedType, RxContexts.create().withRxContext(rxConfig.rxContext).withRxContext(rxContext).build(), putter, rxConfig.actPolicy.getCachePolicy());
    }

    // --------------------------------------------
    // Repository requests with one lambda
    // --------------------------------------------

    @Override
    public <RequestKey> RxRepositoryReader<K, V> requestActorFor(RequestKey requestKey, Strategy0<Map<K, V>> actor) {
        return factory.createLazyRepositoryReader(masterCacheId, cachedType, rxConfig.rxContext, requestKey, actor);
    }

    @Override
    public <RequestKey> RxRepositoryReader<K, V> requestActorFor(MasterCacheId masterCacheId, RequestKey requestKey, Strategy0<Map<K, V>> actor) {
        return factory.createLazyRepositoryReader(masterCacheId, cachedType, rxConfig.rxContext, requestKey, actor);
    }

    @Override
    public <RequestKey> RxRepositoryReader<K, V> requestActorFor(MasterCacheId masterCacheId, RxContext rxContext, RequestKey requestKey, Strategy0<Map<K, V>> actor) {
        return factory.createLazyRepositoryReader(masterCacheId, cachedType, RxContexts.create().withRxContext(rxConfig.rxContext).withRxContext(rxContext).build(), requestKey, actor);
    }

    @Override
    public RxRepositoryReader<K, V> uuidRequestActorFor(Strategy0<Map<K, V>> actor) {
        return factory.createLazyRepositoryReader(masterCacheId, cachedType, rxConfig.rxContext, UUID.randomUUID(), actor);
    }

    @Override
    public RxRepositoryReader<K, V> uuidRequestActorFor(MasterCacheId masterCacheId, Strategy0<Map<K, V>> actor) {
        return factory.createLazyRepositoryReader(masterCacheId, cachedType, rxConfig.rxContext, UUID.randomUUID(), actor);
    }

    @Override
    public RxRepositoryReader<K, V> uuidRequestActorFor(MasterCacheId masterCacheId, RxContext rxContext, Strategy0<Map<K, V>> actor) {
        return factory.createLazyRepositoryReader(masterCacheId, cachedType, RxContexts.create().withRxContext(rxConfig.rxContext).withRxContext(rxContext).build(), UUID.randomUUID(), actor);
    }

    // --------------------------------------------
    // Repository requests with one lambda
    // --------------------------------------------

    @Override
    public RxValueReader<V> requestValueFor(K key, Strategy1<V, K> actor) {
        return factory.createLazyValueReader(key, masterCacheId, cachedType, rxConfig.rxContext, key, actor);
    }

    @Override
    public RxValueReader<V> requestValueFor(MasterCacheId masterCacheId, K key, Strategy1<V, K> actor) {
        return factory.createLazyValueReader(key, masterCacheId, cachedType, rxConfig.rxContext, key, actor);
    }

    @Override
    public RxValueReader<V> requestValueFor(RxContext rxContext, K key, Strategy1<V, K> actor) {
        return factory.createLazyValueReader(key, masterCacheId, cachedType, RxContexts.create().withRxContext(rxConfig.rxContext).withRxContext(rxContext).build(), key, actor);
    }

    @Override
    public RxValueReader<V> requestValueFor(MasterCacheId masterCacheId, RxContext rxContext, K key, Strategy1<V, K> actor) {
        return factory.createLazyValueReader(key, masterCacheId, cachedType, RxContexts.create().withRxContext(rxConfig.rxContext).withRxContext(rxContext).build(), key, actor);
    }

    @Override
    public RxValueReader<V> uuidRequestValueFor(K key, Strategy1<V, K> actor) {
        return factory.createLazyValueReader(UUID.randomUUID(), masterCacheId, cachedType, rxConfig.rxContext, key, actor);
    }

    @Override
    public RxValueReader<V> uuidRequestValueFor(MasterCacheId masterCacheId, K key, Strategy1<V, K> actor) {
        return factory.createLazyValueReader(UUID.randomUUID(), masterCacheId, cachedType, rxConfig.rxContext, key, actor);
    }

    @Override
    public RxValueReader<V> uuidRequestValueFor(RxContext rxContext, K key, Strategy1<V, K> actor) {
        return factory.createLazyValueReader(UUID.randomUUID(), masterCacheId, cachedType, RxContexts.create().withRxContext(rxConfig.rxContext).withRxContext(rxContext).build(), key, actor);
    }

    @Override
    public RxValueReader<V> uuidRequestValueFor(MasterCacheId masterCacheId, RxContext rxContext, K key, Strategy1<V, K> actor) {
        return factory.createLazyValueReader(UUID.randomUUID(), masterCacheId, cachedType, RxContexts.create().withRxContext(rxConfig.rxContext).withRxContext(rxContext).build(), key, actor);
    }

    @Override
    public RxLoanedValueReader<V> requestLoanedValueFor(K key, Strategy1<V, K> actor, LoanPolicy loanPolicy) {
        return factory.createLazyLoanedValueReader(key, masterCacheId, cachedType, rxConfig.rxContext, key, actor, loanPolicy);
    }

    @Override
    public RxLoanedValueReader<V> uuidRequestLoanedValueFor(K key, Strategy1<V, K> actor, LoanPolicy loanPolicy) {
        return factory.createLazyLoanedValueReader(UUID.randomUUID(), masterCacheId, cachedType, rxConfig.rxContext, key, actor, loanPolicy);
    }

    @Override
    public String toString() {
        return "CachedRepository{" +
                "tasksController=" + tasksController +
                ", masterCacheId=" + masterCacheId +
                ", cachedType=" + cachedType +
                ", requestCache=" + requestCache +
                ", repositoryConfig=" + repositoryConfig +
                ", rxConfig=" + rxConfig +
                '}';
    }
}
