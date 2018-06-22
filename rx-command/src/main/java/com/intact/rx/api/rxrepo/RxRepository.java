package com.intact.rx.api.rxrepo;

import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxContext;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.cache.ValueHandle;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.Strategy2;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.subject.RxKeyValueSubjectCombi;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreaker;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerCache;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxcircuit.rate.RateLimiter;
import com.intact.rx.core.rxcircuit.rate.RateLimiterCache;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxrepo.RepositoryConfig;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;
import com.intact.rx.core.rxrepo.RequestCache;
import com.intact.rx.policy.LoanPolicy;

public interface RxRepository<K, V> extends RxRepositoryAccessor<K, V> {
    static void clear(CacheHandle cacheHandle) {
        RxCacheAccess.clear(cacheHandle);
    }

    RequestCache<RxStreamer> getRequestCache();

    MasterCacheId getMasterCacheId();

    Class<V> getCachedType();

    RxConfig getRxConfig();

    RepositoryConfig getRepositoryConfig();

    RxSubjectCombi<Map<K, V>> observeData();

    CircuitBreaker circuit();

    static Optional<CircuitBreaker> find(CircuitId circuitId) {
        return CircuitBreakerCache.find(circuitId);
    }

    RateLimiter rateLimiter();

    static Optional<RateLimiter> find(RateLimiterId rateLimiterId) {
        return RateLimiterCache.find(rateLimiterId);
    }

    RxKeyValueSubjectCombi<ValueHandle<?>, RxStreamer> observeRequests();

    Optional<Command<Void>> addTask(CommandPolicy policy, VoidStrategy0 strategy);

    Optional<Command<Void>> addTask(CommandPolicy policy, Strategy0<Boolean> executeCondition, VoidStrategy0 strategy);

    void startTasks();

    void clearCache();

    boolean clearCachedTypeFrom(MasterCacheId masterCacheId);

    <RequestKey> void clear(RequestKey requestKey, MasterCacheId masterCacheId);

    <RequestKey> void clear(RequestKey requestKey, CacheHandle cacheHandle);

    void clearSingleton(Class<V> cachedType);

    <RequestKey> Optional<RxRepositoryReader<K, V>> find(RequestKey requestKey);

    <RequestKey> Optional<RxRepositoryReader<K, V>> remove(RequestKey requestKey);

    <RequestKey> Optional<RxRepositoryReader<K, V>> replace(RequestKey requestKey, Strategy0<RxStreamer> factory);

    Optional<RxRepositoryReader<K, V>> replaceSingleton(Strategy0<RxStreamer> factory);

    Optional<RxCache<K, V>> findDefaultCache();

    Optional<RxCache<K, V>> findCache(MasterCacheId masterCacheId);

    Optional<RxCache<K, V>> findCache(CacheHandle cacheHandle);

    <K1, T> RxCache<K1, T> cache(Class<T> cachedType);

    RxCache<K, V> cache();

    <RequestKey> Optional<RepositoryRequestStatus> findRequestStatus(RequestKey requestKey);

    /**
     * Create scoped and keyed request and return repository reader
     * Scoped: Streamer lifetime is [start, finish], where finish includes success and failure.
     */
    <RequestKey> RxRepositoryReader<K, V> requestFor(RequestKey requestKey, Strategy0<RxStreamer> factory);

    /**
     * Create a uuid (no-key) request and return a repository reader for the request.
     * Scoped: Streamer lifetime is [start, finish], where finish includes success and failure.
     */
    RxRepositoryReader<K, V> uuidRequestFor(Strategy0<RxStreamer> factory);

    RxRepositoryReaderCollection<K, V> requestAllFor(Map<K, Strategy0<RxStreamer>> requestFactories);

    RxRepositoryReaderCollection<K, V> requestAllFor(Iterable<K> keys, Strategy1<V, K> valueComputer);

    RxRepositoryReaderCollection<K, V> requestAllFor(MasterCacheId masterCacheId, Iterable<K> keys, Strategy1<V, K> valueComputer);

    /**
     * Create a keyed request with a Streamer that will be reused until it its lifetime expires, where lifetime
     * is defined by requestCachePolicy.
     */
    <RequestKey> RxRepositoryReader<K, V> reusableRequestFor(RequestKey requestKey, Strategy0<RxStreamer> factory);

    RxRepositoryReaderCollection<K, V> reusableRequestAllFor(Map<K, Strategy0<RxStreamer>> requestFactories);

    RxRepositoryReader<K, V> singletonRequestFor(Strategy0<RxStreamer> factory);

    RxRepositoryReader<K, V> singletonRequestActorFor(Strategy0<Map<K, V>> actor);

    RxRepositoryWriter<K, V> writeRequestFor(Strategy2<Boolean, K, V> pusher);

    RxRepositoryWriter<K, V> writeRequestFor(MasterCacheId masterCacheId, Strategy2<Boolean, K, V> pusher);

    RxRepositoryWriter<K, V> writeRequestFor(MasterCacheId masterCacheId, RxContext rxContext, Strategy2<Boolean, K, V> pusher);

//    RxRepositoryWriter<K, V> writeRequestFor(MasterCacheId masterCacheId, RxPolicy rxPolicy, Strategy2<Boolean, K, V> pusher);
//
//    RxRepositoryWriter<K, V> writeRequestFor(MasterCacheId masterCacheId, RxContext rxContextsBuilder, RxPolicy rxPolicy, Strategy2<Boolean, K, V> pusher);

    <RequestKey> RxRepositoryReader<K, V> requestActorFor(RequestKey requestKey, Strategy0<Map<K, V>> actor);

    <RequestKey> RxRepositoryReader<K, V> requestActorFor(MasterCacheId masterCacheId, RequestKey requestKey, Strategy0<Map<K, V>> actor);

    <RequestKey> RxRepositoryReader<K, V> requestActorFor(MasterCacheId masterCacheId, RxContext rxContext, RequestKey requestKey, Strategy0<Map<K, V>> actor);

    RxRepositoryReader<K, V> uuidRequestActorFor(Strategy0<Map<K, V>> actor);

    RxRepositoryReader<K, V> uuidRequestActorFor(MasterCacheId masterCacheId, Strategy0<Map<K, V>> actor);

    RxRepositoryReader<K, V> uuidRequestActorFor(MasterCacheId masterCacheId, RxContext rxContext, Strategy0<Map<K, V>> actor);

    /**
     * Retrieve value with actor using key
     *
     * @param key   used as input to actor, also used as request key
     * @param actor to be executed
     * @return value reader
     */
    RxValueReader<V> requestValueFor(K key, Strategy1<V, K> actor);

    /**
     * Retrieve value with actor using key
     *
     * @param masterCacheId to identify in which cache to store value
     * @param key           used as input to actor, also used as request key
     * @param actor         to be executed
     * @return value reader
     */
    RxValueReader<V> requestValueFor(MasterCacheId masterCacheId, K key, Strategy1<V, K> actor);

    /**
     * Retrieve value with actor using key
     *
     * @param rxContext context invoke before and after execution of actor
     * @param key       used as input to actor, also used as request key
     * @param actor     to be executed
     * @return value reader
     */
    RxValueReader<V> requestValueFor(RxContext rxContext, K key, Strategy1<V, K> actor);

    /**
     * Retrieve value with actor using key
     *
     * @param masterCacheId to identify in which cache to store value
     * @param rxContext     context invoke before and after execution of actor
     * @param key           used as input to actor, also used as request key
     * @param actor         to be executed
     * @return value reader
     */
    RxValueReader<V> requestValueFor(MasterCacheId masterCacheId, RxContext rxContext, K key, Strategy1<V, K> actor);

    /**
     * Retrieve value with actor using key
     *
     * @param key   used as input to actor, request key is uuid
     * @param actor to be executed
     * @return value reader
     */
    RxValueReader<V> uuidRequestValueFor(K key, Strategy1<V, K> actor);

    /**
     * Retrieve value with actor using key
     *
     * @param masterCacheId to identify in which cache to store value
     * @param key           used as input to actor, request key is uuid
     * @param actor         to be executed
     * @return value reader
     */
    RxValueReader<V> uuidRequestValueFor(MasterCacheId masterCacheId, K key, Strategy1<V, K> actor);

    /**
     * Retrieve value with actor using key
     *
     * @param rxContext context invoke before and after execution of actor
     * @param key       used as input to actor, request key is uuid
     * @param actor     to be executed
     * @return value reader
     */
    RxValueReader<V> uuidRequestValueFor(RxContext rxContext, K key, Strategy1<V, K> actor);

    /**
     * Retrieve value with actor using key
     *
     * @param masterCacheId to identify in which cache to store value
     * @param rxContext     context invoke before and after execution of actor
     * @param key           used as input to actor, request key is uuid
     * @param actor         to be executed
     * @return value reader
     */
    RxValueReader<V> uuidRequestValueFor(MasterCacheId masterCacheId, RxContext rxContext, K key, Strategy1<V, K> actor);

    /**
     * Retrieve value with actor using key
     *
     * @param key        used as input to actor, also used as request key
     * @param actor      to be executed
     * @param loanPolicy policy on loan
     * @return value reader
     */
    RxLoanedValueReader<V> requestLoanedValueFor(K key, Strategy1<V, K> actor, LoanPolicy loanPolicy);

    /**
     * Retrieve value with actor using key
     *
     * @param key        used as input to actor, also used as request key
     * @param actor      to be executed
     * @param loanPolicy policy on loan
     * @return value reader
     */
    RxLoanedValueReader<V> uuidRequestLoanedValueFor(K key, Strategy1<V, K> actor, LoanPolicy loanPolicy);
}
