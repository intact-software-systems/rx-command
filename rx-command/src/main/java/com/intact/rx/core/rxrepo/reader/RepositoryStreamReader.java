package com.intact.rx.core.rxrepo.reader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxrepo.RxRepositoryReader;
import com.intact.rx.api.rxrepo.RxRequestResult;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.rxrepo.RepositoryRequestResult;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;

public class RepositoryStreamReader<K, V> implements RxRepositoryReader<K, V>, RxObserver<Map<K, V>> {
    private final CacheHandle cacheHandle;
    private final RxSubjectCombi<Map<K, V>> subject;
    private final Strategy0<RxStreamer> streamer;

    public RepositoryStreamReader(Strategy0<RxStreamer> streamer, CacheHandle cacheHandle) {
        this.streamer = requireNonNull(streamer);
        this.cacheHandle = requireNonNull(cacheHandle);
        this.subject = new RxSubjectCombi<>();
        this.streamer.perform()
                .onSubscribeDo(this::onSubscribe)
                .onNextDo(this::onNext, getCachedType())
                .onErrorDo(this::onError)
                .onCompleteDo(this::onComplete);
    }

    @Override
    public RxCache<K, V> cache() {
        return streamer().cache(cacheHandle);
    }

    @Override
    public Optional<V> computeIfAbsent(K key, long msecs) {
        return streamer().computeIfAbsent(cacheHandle, key, msecs);
    }

    @Override
    public Optional<V> computeIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return streamer().computeIf(cacheHandle, key, computeResolver, msecs);
    }

    @Override
    public V computeValueIfAbsent(K key, long msecs) {
        return streamer().computeValueIfAbsent(cacheHandle, key, msecs);
    }

    @Override
    public V computeValueIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return streamer().computeValueIf(cacheHandle, key, computeResolver, msecs);
    }

    @Override
    public Map<K, V> computeIfAbsent(Iterable<K> keys, long msecs) {
        return streamer().computeIfAbsent(cacheHandle, keys, msecs);
    }

    @Override
    public Map<K, V> computeIf(Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        return streamer().computeIf(cacheHandle, keys, computeResolver, msecs);
    }

    @Override
    public RxRequestResult<K, V> computeResultIfAbsent(K key, long msecs) {
        return RepositoryRequestResult.create(
                key,
                streamer().<K, V>computeIfAbsent(cacheHandle, key, msecs).orElse(null),
                RepositoryRequestStatus.create(streamer().getStatus())
        );
    }

    @Override
    public RxRequestResult<K, V> computeResultIf(K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        return RepositoryRequestResult.create(
                key,
                streamer().computeIf(cacheHandle, key, computeResolver, msecs).orElse(null),
                RepositoryRequestStatus.create(streamer().getStatus())
        );
    }

    @Override
    public List<V> computeListIfEmpty(long msecs) {
        return streamer().computeListIfEmpty(cacheHandle, msecs);
    }

    @Override
    public Map<K, V> computeIfEmpty(long msecs) {
        return streamer().computeIfEmpty(cacheHandle, msecs);
    }

    @Override
    public Optional<V> compute(K key, long msecs) {
        return streamer().compute(cacheHandle, key, msecs);
    }

    @Override
    public V computeValue(K key, long msecs) {
        return streamer().computeValue(cacheHandle, key, msecs);
    }

    @Override
    public Map<K, V> compute(Iterable<K> keys, long msecs) {
        return streamer().compute(cacheHandle, keys, msecs);
    }

    @Override
    public RxRequestResult<K, V> computeResult(K key, long msecs) {
        return RepositoryRequestResult.create(
                key,
                streamer().<K, V>compute(cacheHandle, key, msecs).orElse(null),
                RepositoryRequestStatus.create(streamer().getStatus())
        );
    }

    @Override
    public List<V> computeList(long msecs) {
        return streamer().computeList(cacheHandle, msecs);
    }

    @Override
    public Map<K, V> compute(long msecs) {
        return streamer().compute(cacheHandle, msecs);
    }

    @Override
    public RxRepositoryReader<K, V> subscribe() {
        streamer().subscribe();
        return this;
    }

    @Override
    public RxRepositoryReader<K, V> waitFor(long msecs) {
        streamer().waitFor(msecs);
        return this;
    }

    @Override
    public RxRequestStatus getStatus() {
        return RepositoryRequestStatus.create(streamer().getStatus());
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

    private RxStreamer streamer() {
        return streamer.perform();
    }

    private Class<V> getCachedType() {
        return this.cacheHandle.getDataCacheId().getId().<V>getTypedClazz().orElseThrow(() -> new IllegalStateException("Cannot attach rx observer without class information"));
    }
}
