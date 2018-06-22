package com.intact.rx.core.rxcache.act;

import java.util.Map;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.cache.CacheHandle;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.api.cache.observer.DataCacheObserver;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.subject.RxKeyValueSubject;
import com.intact.rx.core.cache.strategy.CachePolicyChecker;
import com.intact.rx.core.command.Commands;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.core.command.factory.CommandFactory;
import com.intact.rx.core.command.status.ExecutionStatus;
import com.intact.rx.core.command.strategy.ExecutionPolicyChecker;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.core.rxcache.api.Act;
import com.intact.rx.templates.ContextObject;

/**
 * Act is an observer to a contained CommandController, which upon completion calls
 * onData with the result. The data is then written to cache using provided cache ids.
 * <p>
 * Usage:
 * - Attach commands to controller
 * - Start commands using subscribe
 * - Stop commands using unsubscribe
 * - Access results using CommandResult (future, pull-based)
 * - Access results through cache using this.reader() and this.writer()
 * - Provide observer to retrieve data from callbacks (push-based).
 */
@SuppressWarnings({"SynchronizedMethod", "SynchronizeOnThis"})
public class ActCommands<K, V>
        implements
        Act<K, V>,
        RxObserver<Map<K, V>>,
        DataCacheObserver,
        Subscription {
    private static final Logger log = LoggerFactory.getLogger(ActCommands.class);

    private final ContextObject<ActPolicy, ActState<K, V>> context;

    public ActCommands(CacheHandle cacheHandle, ActPolicy policy, RxThreadPool commandThreadPool, Commands<Map<K, V>> commands) {
        context = new ContextObject<>(policy, new ActState<>(cacheHandle, commandThreadPool, commands));
    }

    // -----------------------------------------------------------
    // Interface Subscription
    // -----------------------------------------------------------

    @Override
    public void request(long n) {
        subscribe();
    }

    @Override
    public void cancel() {
        unsubscribe();
    }

    // -----------------------------------------------------------
    // Interface Act
    // -----------------------------------------------------------

    @Override
    public CommandResult<Map<K, V>> subscribe() {
        return subscribePrivate();
    }

    @Override
    public boolean unsubscribe() {
        return unsubscribePrivate();
    }

    @Override
    public CommandResult<Map<K, V>> refresh() {
        return state().getController().trigger();
    }

    @Override
    public synchronized boolean isSubscribed() {
        return state().getExecutionStatus().isExecuting();
    }

    @Override
    public synchronized Commands<Map<K, V>> getCommands() {
        return state().getCommandsWithState();
    }

    @Override
    public synchronized CommandResult<Map<K, V>> result() {
        return state().getResult();
    }

    @Override
    public synchronized boolean isDone() {
        return isDonePrivate();
    }

    @Override
    public ExecutionStatus getExecutionStatus() {
        return state().getExecutionStatus().copy();
    }

    @Override
    public CacheHandle getCacheHandle() {
        return state().getCacheHandle();
    }

    @Override
    public RxCache<K, V> cache() {
        return cacheData();
    }

    // -----------------------------------------------------------
    // Interface RxObserver
    // -----------------------------------------------------------

    @Override
    public void onComplete() {
        // Precondition: Validate.assertTrue(state().getExecutionStatus().isExecuting());
        RxKeyValueSubject<Act<?, ?>, Map<?, ?>> subject = state().getActSubject().makeCopy();
        processFinished();
        state().getExecutionStatus().success();
        state().onComplete();
        subject.onComplete(this);
    }

    @Override
    public void onError(Throwable throwable) {
        // Precondition: Validate.assertTrue(state().getExecutionStatus().isExecuting());
        RxKeyValueSubject<Act<?, ?>, Map<?, ?>> subject = state().getActSubject().makeCopy();
        processFinished();
        state().getExecutionStatus().failure();
        state().onError(throwable);
        subject.onError(this, throwable);
    }

    @Override
    public void onNext(Map<K, V> values) {
        // Precondition: Validate.assertTrue(state().getExecutionStatus().isExecuting(), "Status: " + state().getExecutionStatus());
        requireNonNull(values);

        cacheData().writeAll(values);

        state().onNext(values);
        state().getActSubject().onNext(this, values);

        if (config().getExtension().isRenewOnWrite() || config().getExtension().isRenewOnAccess()) {
            state().getAccessStatus().renewLoan();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
    }

    // -----------------------------------------------------------
    // Interface CacheObserver
    // -----------------------------------------------------------

    @Override
    public void onClearedCache(CacheHandle id) {
        reloadSubscribe(config().getReload().isOnInvalidate());
    }

    @Override
    public void onCreatedCache(CacheHandle id) {
        reloadSubscribe(config().getReload().isOnCreate());
    }

    @Override
    public void onModifiedCache(CacheHandle id) {
        reloadSubscribe(config().getReload().isOnModify());
    }

    @Override
    public void onRemovedCache(CacheHandle id) {
        reloadSubscribe(config().getReload().isOnDelete());
    }

    // -----------------------------------------------------------
    // Observer interfaces
    // -----------------------------------------------------------

    @Override
    public boolean connect(RxKeyValueObserver<Act<?, ?>, Map<?, ?>> actObserver) {
        return state().getActSubject().connect(actObserver);
    }

    @Override
    public boolean disconnect(RxKeyValueObserver<Act<?, ?>, Map<?, ?>> actObserver) {
        return state().getActSubject().disconnect(actObserver);
    }

    @Override
    public void disconnectAll() {
        state().getActSubject().disconnectAll();
        state().getRxSubject().disconnectAll();
        state().getLambdaSubject().disconnectAll();
    }

    @Override
    public boolean connect(RxObserver<Map<K, V>> observer) {
        return state().getRxSubject().connect(observer);
    }

    @Override
    public boolean disconnect(RxObserver<Map<K, V>> observer) {
        return state().getRxSubject().disconnect(observer);
    }

    // -----------------------------------------------------------
    // Fluent observer connect
    // -----------------------------------------------------------

    @Override
    public Act<K, V> onCompleteDo(VoidStrategy0 completedFunction) {
        state().getLambdaSubject().onCompleteDo(completedFunction);
        return this;
    }

    @Override
    public Act<K, V> onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        state().getLambdaSubject().onErrorDo(errorFunction);
        return this;
    }

    @Override
    public Act<K, V> onNextDo(VoidStrategy1<Map<K, V>> nextFunction) {
        state().getLambdaSubject().onNextDo(nextFunction);
        return this;
    }

    @Override
    public Act<K, V> onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        state().getLambdaSubject().onSubscribeDo(subscribeFunction);
        return this;
    }

    // -----------------------------------------------------------
    // protected/private functions
    // -----------------------------------------------------------

    private RxCache<K, V> cacheData() {
        return RxCacheAccess.cache(state().getCacheHandle(), config().getCachePolicy());
    }

    private CommandResult<Map<K, V>> subscribePrivate() {
        RxCache<K, V> cache = cacheData();
        if (!state().getExecutionStatus().isExecuting()) {
            boolean doSubscribe;
            synchronized (this) {
                doSubscribe = !state().getExecutionStatus().isExecuting();
                if (doSubscribe) {
                    // Precondition: Validate.assertTrue(!state().getController().isExecuting());
                    // Precondition: Validate.assertTrue(state().getController().isDone());

                    state().getController().disconnectAll();

                    if (!config().getReload().isNo()) {
                        cache.addCacheObserver(this);
                    }

                    CommandController<Map<K, V>> controller = CommandFactory.createController(config().getCommandControllerPolicy(), state().getCommandThreadPool());
                    controller.setCommands(state().getCommands().copy());
                    controller.connect(this);

                    state().getExecutionStatus().start();
                    state().setController(controller);

                    CommandResult<Map<K, V>> result = controller.subscribe();
                    state().setResult(result);
                }
            }

            if (doSubscribe) {
                state().onSubscribe(this);
                state().getActSubject().onSubscribe(this, this);
            }
        }
        return state().getResult();
    }

    private boolean unsubscribePrivate() {
        RxCache<K, V> cache = cacheData();

        synchronized (this) {
            CommandController<Map<K, V>> controller = state().getController();
            boolean unsubscribed = controller.unsubscribe();
            controller.disconnectAll();

            if (unsubscribed) {
                cache.removeCacheObserver(this);
                state().getActSubject().disconnectAll();
                state().getRxSubject().disconnectAll();

                state().getResult().cancel();
                state().resetController();
                //noinspection AccessToStaticFieldLockedOnInstance
                log.debug("Unsubscribing Act@{} dataCacheId: {}", this.hashCode(), state().getCacheHandle().getDataCacheId());
            }
            return unsubscribed;
        }
    }

    private boolean reloadSubscribe(boolean reload) {
        if (!reload) {
            return false;
        }

        if (isDonePrivate()) {
            log.debug("Act is done. Ignoring reload.");
            processFinished();
            return false;
        }
        subscribePrivate();
        return true;
    }

    private boolean isDonePrivate() {
        if (state().getExecutionStatus().isExecuting()) {
            return false;
        }
        if (state().getController().isExecuting()) {
            return false;
        }

        boolean attemptedMaxTimes = !ExecutionPolicyChecker.isInAttempt(state().getExecutionStatus(), config().getAttempt());
        boolean exceededLifespan = !CachePolicyChecker.isInLifetime(state().getAccessStatus(), config().getLifetime());

        return attemptedMaxTimes || exceededLifespan;
    }

    private void processFinished() {
        boolean attemptedMaxTimes = !ExecutionPolicyChecker.isInAttempt(state().getExecutionStatus(), config().getAttempt());
        boolean exceededLifespan = !CachePolicyChecker.isInLifetime(state().getAccessStatus(), config().getLifetime());

        if (config().getReload().isNo() || attemptedMaxTimes || exceededLifespan) {
            cacheData().removeCacheObserver(this);
        }

        state().setCommandsWithState(state().getController().getCommands());
    }

    private ActPolicy config() {
        return context.config();
    }

    private ActState<K, V> state() {
        return context.state();
    }

    @Override
    public String toString() {
        return "Act@" + hashCode() + " {" +
                "context=" + context +
                '}';
    }
}
