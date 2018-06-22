package com.intact.rx.core.rxrepo.reader;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxrepo.RxRepositoryReader;
import com.intact.rx.api.rxrepo.RxRequestResult;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.rxcache.StreamerOnlyCacheAccess;
import com.intact.rx.core.rxcache.noop.StreamerNoOp;
import com.intact.rx.core.rxrepo.RepositoryRequestResult;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;
import com.intact.rx.policy.Access;
import com.intact.rx.policy.FaultPolicy;
import com.intact.rx.templates.AtomicSupplier;

import static com.intact.rx.core.rxcache.StreamerAlgorithms.*;

/**
 * Create streamer only when objects not found in cache
 */
public class LazyRepositoryStreamReader<K, V> implements RxRepositoryReader<K, V>, RxObserver<Map<K, V>> {
    private final CacheHandle cacheHandle;
    private final CachePolicy dataCachePolicy;
    private final FaultPolicy faultPolicy;
    private final AtomicSupplier<RxStreamer> streamerFactory;
    private final Strategy0<Access> accessControl;
    private final RxSubjectCombi<Map<K, V>> subject;

    public LazyRepositoryStreamReader(
            CacheHandle cacheHandle,
            CachePolicy dataCachePolicy,
            FaultPolicy faultPolicy,
            Strategy0<RxStreamer> getOrCreateStreamer,
            Strategy0<Access> accessControl) {
        this.cacheHandle = requireNonNull(cacheHandle);
        this.dataCachePolicy = requireNonNull(dataCachePolicy);
        this.faultPolicy = requireNonNull(faultPolicy);
        this.streamerFactory = new AtomicSupplier<>(
                () -> getOrCreateStreamer.perform()
                        .onSubscribeDo(this::onSubscribe)
                        .onNextDo(this::onNext, CacheHandle.<V>findCachedType(cacheHandle).orElseThrow(() -> new IllegalStateException("CacheHandle without class information")))
                        .onErrorDo(this::onError)
                        .onCompleteDo(this::onComplete));

        this.accessControl = requireNonNull(accessControl);
        this.subject = new RxSubjectCombi<>();
    }

    @Override
    public RxCache<K, V> cache() {
        return RxCacheAccess.cache(cacheHandle, dataCachePolicy);
    }

    @Override
    public Optional<V> computeIfAbsent(K key, long msecs) {
        return computeIfAbsentAlgorithm(this::getStreamer, cache(), faultPolicy, v -> {}, key, Objects::nonNull, msecs);
    }

    @Override
    public Optional<V> computeIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return computeIfAlgorithm(this::getStreamer, cache(), faultPolicy, v -> {}, key, computeResolver, msecs);
    }

    @Override
    public V computeValueIfAbsent(K key, long msecs) {
        return computeValueIfAbsentAlgorithm(this::getStreamer, cache(), v -> {}, key, Objects::nonNull, msecs);
    }

    @Override
    public V computeValueIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return computeValueIfAlgorithm(this::getStreamer, cache(), v -> {}, key, computeResolver, msecs);
    }

    @Override
    public Map<K, V> computeIfAbsent(Iterable<K> keys, long msecs) {
        return computeIfAbsentAlgorithm(this::getStreamer, cache(), faultPolicy, kvMap -> {}, keys, msecs);
    }

    @Override
    public Map<K, V> computeIf(Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return computeIfAlgorithm(this::getStreamer, cache(), faultPolicy, kvMap -> {}, keys, computeResolver, msecs);
    }

    @Override
    public RxRequestResult<K, V> computeResultIfAbsent(K key, long msecs) {
        return RepositoryRequestResult.create(
                key,
                computeIfAbsentAlgorithm(this::getStreamer, cache(), faultPolicy, v -> {}, key, Objects::nonNull, msecs).orElse(null),
                RepositoryRequestStatus.create(getStreamer().getStatus())
        );
    }

    @Override
    public RxRequestResult<K, V> computeResultIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return RepositoryRequestResult.create(
                key,
                computeIfAlgorithm(this::getStreamer, cache(), faultPolicy, v -> {}, key, computeResolver, msecs).orElse(null),
                RepositoryRequestStatus.create(getStreamer().getStatus())
        );
    }

    @Override
    public List<V> computeListIfEmpty(long msecs) {
        return computeListIfEmptyAlgorithm(this::getStreamer, cache(), faultPolicy, msecs);
    }

    @Override
    public Map<K, V> computeIfEmpty(long msecs) {
        return computeIfEmptyAlgorithm(this::getStreamer, cache(), faultPolicy, msecs);
    }

    @Override
    public Optional<V> compute(K key, long msecs) {
        return computeAlgorithm(this::getStreamer, cache(), faultPolicy, v -> {}, key, msecs);
    }

    @Override
    public V computeValue(K key, long msecs) {
        return computeValueAlgorithm(this::getStreamer, cache(), v -> {}, key, msecs);
    }

    @Override
    public Map<K, V> compute(Iterable<K> keys, long msecs) {
        return computeAlgorithm(this::getStreamer, cache(), faultPolicy, kvMap -> {}, keys, msecs);
    }

    @Override
    public RxRequestResult<K, V> computeResult(K key, long msecs) {
        return RepositoryRequestResult.create(
                key,
                computeAlgorithm(this::getStreamer, cache(), faultPolicy, v -> {}, key, msecs).orElse(null),
                RepositoryRequestStatus.create(getStreamer().getStatus())
        );
    }

    @Override
    public List<V> computeList(long msecs) {
        return computeListAlgorithm(this::getStreamer, cache(), faultPolicy, msecs);
    }

    @Override
    public Map<K, V> compute(long msecs) {
        return computeAlgorithm(this::getStreamer, cache(), faultPolicy, msecs);
    }

    @Override
    public LazyRepositoryStreamReader<K, V> subscribe() {
        getStreamer().subscribe();
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> waitFor(long msecs) {
        getStreamer().waitFor(msecs);
        return this;
    }

    @Override
    public RxRequestStatus getStatus() {
        return RepositoryRequestStatus.create(getStreamer().getStatus());
    }

    // -----------------------------------------------------------
    // Callback functions
    // -----------------------------------------------------------

    @Override
    public RxRepositoryReader<K, V> onCompleteDo(VoidStrategy0 completedFunction) {
        subject.onCompleteDo(completedFunction);
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        subject.onErrorDo(errorFunction);
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> onNextDo(VoidStrategy1<Map<K, V>> nextFunction) {
        subject.onNextDo(nextFunction);
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        subject.onSubscribeDo(subscribeFunction);
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> connect(RxObserver<Map<K, V>> observer) {
        subject.connect(observer);
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> disconnect(RxObserver<Map<K, V>> observer) {
        subject.disconnect(observer);
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> disconnectAll() {
        subject.disconnectAll();
        return this;
    }

    // -----------------------------------------------------------
    // RxObserver<Map<K, V>>
    // -----------------------------------------------------------

    @Override
    public void onComplete() {
        subject.onComplete();
    }

    @Override
    public void onError(Throwable throwable) {
        subject.onError(throwable);
    }

    @Override
    public void onNext(Map<K, V> value) {
        subject.onNext(value);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subject.onSubscribe(subscription);
    }

    // -----------------------------------------------------------
    // private implementation
    // -----------------------------------------------------------

    private RxStreamer getStreamer() {
        switch (accessControl.perform()) {
            case NONE:
                return StreamerNoOp.instance;
            case CACHE:
                return new StreamerOnlyCacheAccess(streamerFactory.get().getRxConfig().domainCacheId, streamerFactory.get().getMasterCacheId(), streamerFactory.get().getRxConfig().actPolicy.getCachePolicy());
            case ALL:
        }
        return streamerFactory.get();
    }
}
