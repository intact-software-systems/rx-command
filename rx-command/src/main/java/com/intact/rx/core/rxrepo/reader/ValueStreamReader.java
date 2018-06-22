package com.intact.rx.core.rxrepo.reader;

import java.util.Map;
import java.util.Optional;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.cache.RxValueAccess;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.api.rxrepo.RxValueReader;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;

public class ValueStreamReader<K, V> implements RxValueReader<V>, RxObserver<Map<K, V>> {
    private final CacheHandle cacheHandle;
    private final K key;
    private final Strategy0<RxStreamer> streamer;
    private final RxSubjectCombi<V> subject;

    public ValueStreamReader(Strategy0<RxStreamer> streamer, CacheHandle cacheHandle, K key) {
        this.cacheHandle = cacheHandle;
        this.key = key;
        this.streamer = streamer;
        this.subject = new RxSubjectCombi<>();

        this.streamer.perform()
                .onSubscribeDo(this::onSubscribe)
                .onNextDo(this::onNext, getCachedType())
                .onErrorDo(this::onError)
                .onCompleteDo(this::onComplete);
    }

    @Override
    public RxValueAccess<V> accessCached() {
        return RxCacheAccess.<K, V>cache(cacheHandle, streamer.perform().getRxConfig().actPolicy.getCachePolicy()).access(key);
    }

    @Override
    public RxRequestStatus getStatus() {
        return RepositoryRequestStatus.create(getStreamer().getStatus());
    }

    @Override
    public Optional<V> computeIfAbsent(long msecs) {
        return getStreamer().computeIfAbsent(cacheHandle, key, msecs);
    }

    @Override
    public Optional<V> computeIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        return getStreamer().computeIf(cacheHandle, key, computeResolver, msecs);
    }

    @Override
    public V computeValueIfAbsent(long msecs) {
        return getStreamer().computeValueIfAbsent(cacheHandle, key, msecs);
    }

    @Override
    public V computeValueIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        return getStreamer().computeValueIf(cacheHandle, key, computeResolver, msecs);
    }

    @Override
    public Optional<V> compute(long msecs) {
        return getStreamer().compute(cacheHandle, key, msecs);
    }

    @Override
    public V computeValue(long msecs) {
        return getStreamer().computeValue(cacheHandle, key, msecs);
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

    private RxStreamer getStreamer() {
        return streamer.perform();
    }

    private Class<V> getCachedType() {
        return this.cacheHandle.getDataCacheId().getId().<V>getTypedClazz().orElseThrow(() -> new IllegalStateException("Cannot attach rx observer without class information"));
    }
}
