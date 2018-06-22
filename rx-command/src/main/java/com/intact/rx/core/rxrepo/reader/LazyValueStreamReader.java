package com.intact.rx.core.rxrepo.reader;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.*;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.api.rxrepo.RxValueReader;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.rxcache.StreamerOnlyCacheAccess;
import com.intact.rx.core.rxcache.noop.StreamerNoOp;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;
import com.intact.rx.policy.Access;
import com.intact.rx.policy.FaultPolicy;
import com.intact.rx.templates.AtomicSupplier;

import static com.intact.rx.core.rxcache.StreamerAlgorithms.*;

public class LazyValueStreamReader<K, V> implements RxValueReader<V>, RxObserver<Map<K, V>> {
    private final CacheHandle cacheHandle;
    private final CachePolicy cachePolicy;
    private final FaultPolicy faultPolicy;
    private final AtomicSupplier<RxStreamer> streamerFactory;
    private final K key;
    private final Strategy0<Access> accessControl;
    private final RxSubjectCombi<V> subject;

    public LazyValueStreamReader(
            CacheHandle cacheHandle,
            CachePolicy cachePolicy,
            FaultPolicy faultPolicy,
            Strategy0<RxStreamer> streamerFactory,
            K key,
            Strategy0<Access> accessControl) {
        this.cacheHandle = requireNonNull(cacheHandle);
        this.cachePolicy = requireNonNull(cachePolicy);
        this.faultPolicy = requireNonNull(faultPolicy);
        this.streamerFactory = new AtomicSupplier<>(
                () -> streamerFactory.perform()
                        .onSubscribeDo(this::onSubscribe)
                        .onNextDo(this::onNext, CacheHandle.<V>findCachedType(cacheHandle).orElseThrow(() -> new IllegalStateException("CacheHandle without class information")))
                        .onErrorDo(this::onError)
                        .onCompleteDo(this::onComplete));

        this.key = requireNonNull(key);
        this.accessControl = requireNonNull(accessControl);
        this.subject = new RxSubjectCombi<>();
    }

    @Override
    public RxValueAccess<V> accessCached() {
        return cache().access(key);
    }

    @Override
    public RxRequestStatus getStatus() {
        return RepositoryRequestStatus.create(getStreamer().getStatus());
    }

    @Override
    public Optional<V> computeIfAbsent(long msecs) {
        return computeIfAbsentAlgorithm(this::getStreamer, cache(), faultPolicy, v -> {}, key, Objects::nonNull, msecs);
    }

    @Override
    public Optional<V> computeIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        return computeIfAlgorithm(this::getStreamer, cache(), faultPolicy, v -> {}, key, computeResolver, msecs);
    }

    @Override
    public V computeValueIfAbsent(long msecs) {
        return computeValueIfAbsentAlgorithm(this::getStreamer, cache(), v -> {}, key, Objects::nonNull, msecs);
    }

    @Override
    public V computeValueIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        return computeValueIfAlgorithm(this::getStreamer, cache(), v -> {}, key, computeResolver, msecs);
    }

    @Override
    public Optional<V> compute(long msecs) {
        return computeAlgorithm(this::getStreamer, cache(), faultPolicy, v -> {}, key, msecs);
    }

    @Override
    public V computeValue(long msecs) {
        return computeValueAlgorithm(this::getStreamer, cache(), v -> {}, key, msecs);
    }

    @Override
    public RxValueReader<V> subscribe() {
        getStreamer().subscribe();
        return this;
    }

    @Override
    public RxValueReader<V> waitFor(long msecs) {
        getStreamer().waitFor(msecs);
        return this;
    }

    // -----------------------------------------------------------
    // Callback functions
    // -----------------------------------------------------------

    @Override
    public RxValueReader<V> onCompleteDo(VoidStrategy0 completedFunction) {
        subject.onCompleteDo(completedFunction);
        return this;
    }

    @Override
    public RxValueReader<V> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        subject.onErrorDo(errorFunction);
        return this;
    }

    @Override
    public RxValueReader<V> onNextDo(VoidStrategy1<V> nextFunction) {
        subject.onNextDo(nextFunction);
        return this;
    }

    @Override
    public RxValueReader<V> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        subject.onSubscribeDo(subscribeFunction);
        return this;
    }

    @Override
    public RxValueReader<V> connect(RxObserver<V> observer) {
        subject.connect(observer);
        return this;
    }

    @Override
    public RxValueReader<V> disconnect(RxObserver<V> observer) {
        subject.disconnect(observer);
        return this;
    }

    @Override
    public RxValueReader<V> disconnectAll() {
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
        Optional.ofNullable(value.get(key)).ifPresent(subject::onNext);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subject.onSubscribe(subscription);
    }

    // -----------------------------------------------------------
    // private implementation
    // -----------------------------------------------------------

    private RxCache<K, V> cache() {
        return RxCacheAccess.cache(cacheHandle, cachePolicy);
    }

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
