package com.intact.rx.core.rxcache;

import java.util.Map;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.rxcache.acts.ActGroup;

import static com.intact.rx.core.rxcache.StreamerAlgorithms.*;

@SuppressWarnings("WeakerAccess")
public class StreamerBuilder {
    private final Streamer streamer;

    public StreamerBuilder(Streamer streamer) {
        this.streamer = streamer;
    }

    public Streamer streamer() {
        return streamer;
    }

    public StreamerGroup sequential() {
        return new StreamerGroup(streamer.getMasterCacheId(), streamer.getRxConfig(), addSequentialGroup(streamer), this);
    }

    public StreamerGroup sequential(MasterCacheId masterCacheId) {
        return new StreamerGroup(masterCacheId, streamer.getRxConfig(), addSequentialGroup(streamer), this);
    }

    public StreamerGroup parallel() {
        return new StreamerGroup(streamer.getMasterCacheId(), streamer.getRxConfig(), addParallelGroup(streamer), this);
    }

    public StreamerGroup parallel(MasterCacheId masterCacheId) {
        return new StreamerGroup(masterCacheId, streamer.getRxConfig(), addParallelGroup(streamer), this);
    }

    /**
     * Async group - execute in parallel to any other group
     */
    public StreamerGroup async() {
        return streamer.state().async.computeIfInitial().asyncGroup();
    }

    public StreamerBuilder syncpoint() {
        return syncpoint(streamer.getRxConfig().commandConfig.getCommandPolicy().getTimeout().toMillis());
    }

    public StreamerBuilder syncpoint(long msecs) {
        computeSyncPoint(this, msecs);
        return this;
    }

    // --------------------------------------------
    // Setup callbacks when execution finishes
    // --------------------------------------------

    public <K, V> StreamerBuilder onNextDo(VoidStrategy1<Map<K, V>> observer, Class<V> cachedType) {
        streamer.onNextDo(observer, cachedType);
        return this;
    }

    public <K, V> StreamerBuilder connect(RxObserver<Map<K, V>> subject, Class<V> cachedType) {
        streamer.connect(subject, cachedType);
        return this;
    }

    // --------------------------------------------
    // Setup callbacks when execution finishes
    // --------------------------------------------

    public StreamerBuilder onCompleteDo(VoidStrategy0 completedFunction) {
        streamer.onCompleteDo(completedFunction);
        return this;
    }

    public StreamerBuilder onErrorDo(VoidStrategy1<Throwable> errorFunction) {
        streamer.onErrorDo(errorFunction);
        return this;
    }

    public StreamerBuilder onNextDo(VoidStrategy1<ActGroup> nextFunction) {
        streamer.onNextDo(nextFunction);
        return this;
    }

    public StreamerBuilder onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction) {
        streamer.onSubscribeDo(subscribeFunction);
        return this;
    }
}
