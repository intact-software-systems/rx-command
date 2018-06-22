package com.intact.rx.core.rxrepo;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.*;
import com.intact.rx.api.command.Strategy0;
import com.intact.rx.api.subject.RxKeyValueSubjectCombi;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.templates.api.RxObserverSupport;

@SuppressWarnings("WeakerAccess")
public class RequestCache<Streamer extends RxObserverSupport<Streamer, ActGroup>> {
    private static final String singletonRequestKey = "singleton";

    private final ValueHandle<String> singletonRequestHandle;

    private final CachePolicy requestCachePolicy;
    private final CacheHandle requestCacheHandle;
    private final RxKeyValueSubjectCombi<ValueHandle<?>, Streamer> subject;

    private RequestCache(CacheHandle requestCacheHandle, CachePolicy requestCachePolicy) {
        this.requestCachePolicy = requireNonNull(requestCachePolicy);
        this.requestCacheHandle = requireNonNull(requestCacheHandle);
        this.singletonRequestHandle = ValueHandle.create(requestCacheHandle, singletonRequestKey);
        this.subject = new RxKeyValueSubjectCombi<>();
    }

    public static <RequestScope, Streamer extends RxObserverSupport<Streamer, ActGroup>> RequestCache<Streamer> withSharedRequestsByScope(Class<Streamer> requestorType, RequestScope requestScope, CachePolicy requestCachePolicy) {
        return new RequestCache<>(CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), new MasterCacheId(requestScope), requestorType), requestCachePolicy);
    }

    public static <Streamer extends RxObserverSupport<Streamer, ActGroup>> RequestCache<Streamer> withIsolatedRequests(Class<Streamer> requestorType, CachePolicy requestCachePolicy) {
        return new RequestCache<>(CacheHandle.create(RxDefault.getDefaultRxCommandDomainCacheId(), MasterCacheId.uuid(), requestorType), requestCachePolicy);
    }

    // --------------------------------------------
    // Getters
    // --------------------------------------------

    public CacheHandle getCacheHandle() {
        return requestCacheHandle;
    }

    public CachePolicy getCachePolicy() {
        return requestCachePolicy;
    }

    public RxKeyValueSubjectCombi<ValueHandle<?>, Streamer> observe() {
        return subject;
    }

    // --------------------------------------------
    // Request cache API
    // --------------------------------------------

    public void clearAll() {
        cache().clear();
    }

    public <RequestKey> Optional<Streamer> remove(RequestKey key) {
        return cache().take(requestHandle(key));
    }

    public <RequestKey> Optional<Streamer> replace(RequestKey requestKey, Strategy0<Streamer> factory) {
        return replacePrivate(requestHandle(requestKey), factory);
    }

    public Optional<Streamer> replaceSingleton(Strategy0<Streamer> factory) {
        return replacePrivate(singletonRequestHandle, factory);
    }

    public Streamer computeSingletonIfAbsent(Strategy0<Streamer> factory) {
        return computeScopedIfAbsentPrivate(singletonRequestHandle, factory);
    }

    public <RequestKey> Streamer computeReusableIfAbsent(RequestKey key, Strategy0<Streamer> factory) {
        ValueHandle<?> requestHandle = requestHandle(key);
        return cache().computeIfAbsent(requestHandle, createStreamerFactory(requestHandle, factory));
    }

    public <RequestKey> Streamer computeScopedIfAbsent(RequestKey key, Strategy0<Streamer> factory) {
        return computeScopedIfAbsentPrivate(requestHandle(key), factory);
    }

    public <RequestKey> Optional<Streamer> find(RequestKey key) {
        return cache().read(requestHandle(key));
    }

    public RxCache<ValueHandle<?>, Streamer> cache() {
        return RxCacheAccess.cache(requestCacheHandle, requestCachePolicy);
    }

    // --------------------------------------------
    // private methods
    // --------------------------------------------

    private Optional<Streamer> replacePrivate(ValueHandle<?> requestHandle, Strategy0<Streamer> factory) {
        Optional<Streamer> previous = cache().write(requestHandle, createStreamerFactory(requestHandle, factory).apply(requestHandle));
        setupReturnLoan(cache().loan(requestHandle, LoanPolicy.readWriteExpireOnNoLoan()));
        return previous;
    }

    private Streamer computeScopedIfAbsentPrivate(ValueHandle<?> requestHandle, Strategy0<Streamer> factory) {
        Function<ValueHandle<?>, Streamer> defaultFactory = createStreamerFactory(requestHandle, factory);
        return setupReturnLoan(cache().computeIfAbsentAndLoan(requestHandle, defaultFactory, LoanPolicy.readWriteExpireOnNoLoan()));
    }

    private Function<ValueHandle<?>, Streamer> createStreamerFactory(ValueHandle<?> requestHandle, Strategy0<Streamer> factory) {
        requireNonNull(requestHandle);
        requireNonNull(factory);

        return handle -> {
            Streamer streamer = factory.perform();
            return setupCallbacks(requestHandle, requireNonNull(streamer));
        };
    }

    private Streamer setupReturnLoan(Loaned<Streamer> loaned) {
        return loaned.read()
                .map(streamer -> streamer
                        .onCompleteDo(loaned::returnLoan)
                        .onErrorDo(throwable -> loaned.returnLoan()))
                .orElseThrow(() -> new IllegalStateException("No streamer present"));
    }

    private Streamer setupCallbacks(ValueHandle<?> requestHandle, Streamer streamer) {
        return streamer
                .onSubscribeDo(subscription -> subject.onSubscribe(requestHandle, subscription))
                .onCompleteDo(() -> {
                    subject.onNext(requestHandle, streamer);
                    subject.onComplete(requestHandle);
                })
                .onErrorDo(throwable -> subject.onError(requestHandle, throwable));
    }

    private <RequestKey> ValueHandle<?> requestHandle(RequestKey key) {
        return ValueHandle.create(requestCacheHandle, key);
    }
}
