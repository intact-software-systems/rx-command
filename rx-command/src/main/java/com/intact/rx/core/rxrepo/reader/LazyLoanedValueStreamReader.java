package com.intact.rx.core.rxrepo.reader;

import java.time.Duration;
import java.util.Map;
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
import com.intact.rx.api.rxrepo.RxLoanedValueReader;
import com.intact.rx.api.rxrepo.RxRequestStatus;
import com.intact.rx.api.rxrepo.RxValueReader;
import com.intact.rx.api.subject.RxSubjectCombi;
import com.intact.rx.core.cache.nullobjects.LoanedValueNoOp;
import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.core.cache.strategy.CachePolicyChecker;
import com.intact.rx.core.rxcache.StreamerOnlyCacheAccess;
import com.intact.rx.core.rxcache.noop.StreamerNoOp;
import com.intact.rx.core.rxrepo.RepositoryRequestStatus;
import com.intact.rx.policy.Access;
import com.intact.rx.policy.FaultPolicy;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.templates.AtomicSupplier;

import static com.intact.rx.core.rxcache.StreamerAlgorithms.*;

public class LazyLoanedValueStreamReader<K, V> implements RxLoanedValueReader<V>, RxObserver<Map<K, V>> {
    private final CacheHandle cacheHandle;
    private final CachePolicy cachePolicy;
    private final FaultPolicy faultPolicy;
    private final AtomicSupplier<RxStreamer> streamerFactory;
    private final K key;
    private final LoanPolicy loanPolicy;
    private final Strategy0<Access> accessControl;
    private final AccessStatus accessStatus;
    private final RxSubjectCombi<V> subject;

    public LazyLoanedValueStreamReader(CacheHandle cacheHandle, CachePolicy cachePolicy, FaultPolicy faultPolicy, Strategy0<RxStreamer> getOrCreateStreamer, K key, LoanPolicy loanPolicy, Strategy0<Access> accessControl) {
        this.cacheHandle = requireNonNull(cacheHandle);
        this.cachePolicy = requireNonNull(cachePolicy);
        this.faultPolicy = requireNonNull(faultPolicy);
        this.streamerFactory = new AtomicSupplier<>(
                () -> getOrCreateStreamer.perform()
                        .onSubscribeDo(this::onSubscribe)
                        .onNextDo(this::onNext, CacheHandle.<V>findCachedType(cacheHandle).orElseThrow(() -> new IllegalStateException("CacheHandle without class information")))
                        .onErrorDo(this::onError)
                        .onCompleteDo(this::onComplete));

        this.loanPolicy = requireNonNull(loanPolicy);
        this.key = requireNonNull(key);
        this.accessControl = requireNonNull(accessControl);
        this.accessStatus = new AccessStatus();
        this.subject = new RxSubjectCombi<>();
    }

    @Override
    public Loaned<V> loanCached() {
        return isExpiredLoan()
                ? LoanedValueNoOp.instance
                : cache().loan(key, loanPolicy);
    }

    @Override
    public Loaned<V> computeIfAbsentAndLoan(long msecs) {
        computeIfAbsent(msecs);
        return isExpiredLoan()
                ? LoanedValueNoOp.instance
                : cache().loan(key, loanPolicy);
    }

    @Override
    public Optional<V> refresh(long msecs) {
        return computeIfAbsentAlgorithm(this::getStreamer, cache(), faultPolicy, v -> v.ifPresent(v1 -> accessStatus.read()), key, v -> false, msecs);
    }

    @Override
    public boolean isExpired() {
        return isExpiredLoan();
    }

    @Override
    public LoanPolicy getLoanPolicy() {
        return loanPolicy;
    }

    @Override
    public Duration getLoanStart() {
        return accessStatus.isRead()
                ? Duration.ofMillis(accessStatus.getTime().getTimeSinceRead())
                : Duration.ZERO;
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
        return computeIfAbsentAlgorithm(this::getStreamer, cache(), faultPolicy, v -> v.ifPresent(v1 -> accessStatus.read()), key, v -> v != null && !isExpiredLoan(), msecs);
    }

    @Override
    public Optional<V> computeIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        return computeIfAlgorithm(this::getStreamer, cache(), faultPolicy, v -> v.ifPresent(v1 -> accessStatus.read()), key, v -> computeResolver.perform(v) && isExpiredLoan(), msecs);
    }

    @Override
    public V computeValueIfAbsent(long msecs) {
        return computeValueIfAbsentAlgorithm(this::getStreamer, cache(), v -> v.ifPresent(v1 -> accessStatus.read()), key, v -> v != null && !isExpiredLoan(), msecs);
    }

    @Override
    public V computeValueIf(Strategy1<Boolean, V> computeResolver, long msecs) {
        return computeValueIfAlgorithm(this::getStreamer, cache(), v -> v.ifPresent(v1 -> accessStatus.read()), key, v -> computeResolver.perform(v) && isExpiredLoan(), msecs);
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

    private boolean isExpiredLoan() {
        return !(CachePolicyChecker.isInLifetime(accessStatus, loanPolicy.getLifetime()) && accessStatus.isRead());
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
