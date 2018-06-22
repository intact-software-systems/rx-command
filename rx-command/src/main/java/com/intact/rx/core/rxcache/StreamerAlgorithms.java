package com.intact.rx.core.rxcache;

import java.util.*;
import java.util.function.Supplier;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.api.cache.Reader;
import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.command.Strategy1;
import com.intact.rx.api.command.VoidStrategy1;
import com.intact.rx.api.rxcache.RxStreamer;
import com.intact.rx.core.rxcache.acts.ActGroup;
import com.intact.rx.exception.ExceptionMessageFactory;
import com.intact.rx.policy.FaultPolicy;
import com.intact.rx.templates.Tuple2;

@SuppressWarnings("WeakerAccess")
public final class StreamerAlgorithms {

    public static <K, V> Optional<V> computeIfAbsentAlgorithm(Supplier<RxStreamer> streamer, RxCache<K, V> reader, FaultPolicy faultPolicy, VoidStrategy1<Optional<V>> postComputer, K key, Strategy1<Boolean, V> cacheAcceptor, long msecs) {
        Optional<V> value = reader.read(key);

        FutureStatus status = FutureStatus.NotStarted;
        if (!value.isPresent() || !cacheAcceptor.perform(value.get())) {
            Tuple2<FutureStatus, Optional<V>> tuple = streamer.get().controller().computeIfAbsent(reader.getCacheHandle(), key, msecs);
            status = tuple.first;
            value = tuple.second;

            postComputer.perform(value);
        }

        return value.isPresent() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? value
                : ExceptionMessageFactory.throwFutureException(status, key, streamer.get().getStatus().getException());
    }

    public static <K, V> Map<K, V> computeIfAbsentAlgorithm(Supplier<RxStreamer> streamer, RxCache<K, V> reader, FaultPolicy faultPolicy, VoidStrategy1<Map<K, V>> postComputer, Iterable<K> keys, long msecs) {
        Map<K, V> values = reader.read(keys);

        FutureStatus status = FutureStatus.NotStarted;
        if (values.size() != keys.spliterator().getExactSizeIfKnown()) {
            Tuple2<FutureStatus, Map<K, V>> tuple = streamer.get().controller().computeIfAbsent(reader.getCacheHandle(), keys, msecs);
            status = tuple.first;
            values = tuple.second;

            postComputer.perform(values);
        }

        return !values.isEmpty() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? values
                : ExceptionMessageFactory.throwFutureException(status, keys, streamer.get().getStatus().getException());
    }

    public static <K, V> V computeValueIfAbsentAlgorithm(Supplier<RxStreamer> streamer, RxCache<K, V> reader, VoidStrategy1<Optional<V>> postComputer, K key, Strategy1<Boolean, V> cacheAcceptor, long msecs) {
        Optional<V> value = reader.read(key);

        FutureStatus status = FutureStatus.NotStarted;
        if (!value.isPresent() || !cacheAcceptor.perform(value.get())) {
            Tuple2<FutureStatus, Optional<V>> tuple = streamer.get().controller().computeIfAbsent(reader.getCacheHandle(), key, msecs);
            status = tuple.first;
            value = tuple.second;

            postComputer.perform(value);
        }

        V raw = value.orElse(null);
        return raw != null
                ? raw
                : ExceptionMessageFactory.throwFutureException(status, key, streamer.get().getStatus().getException());
    }

    public static <K, V> Optional<V> computeIfAlgorithm(Supplier<RxStreamer> streamer, RxCache<K, V> reader, FaultPolicy faultPolicy, VoidStrategy1<Optional<V>> postComputer, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        Optional<V> value = Optional.empty();

        FutureStatus status = FutureStatus.NotStarted;
        if (computeResolver.perform(reader.read(key).orElse(null))) {
            Tuple2<FutureStatus, Optional<V>> tuple = streamer.get().controller().computeIf(reader.getCacheHandle(), key, computeResolver, msecs);
            status = tuple.first;
            value = tuple.second;

            postComputer.perform(value);
        }

        return value.isPresent() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? value
                : ExceptionMessageFactory.throwFutureException(status, key, streamer.get().getStatus().getException());
    }

    public static <K, V> Map<K, V> computeIfAlgorithm(Supplier<RxStreamer> streamer, RxCache<K, V> reader, FaultPolicy faultPolicy, VoidStrategy1<Map<K, V>> postComputer, Iterable<K> keys, Strategy1<Boolean, Map<K, V>> computeResolver, long msecs) {
        Map<K, V> values = Collections.emptyMap();

        FutureStatus status = FutureStatus.NotStarted;
        if (computeResolver.perform(reader.read(keys))) {
            Tuple2<FutureStatus, Map<K, V>> tuple = streamer.get().controller().computeIf(reader.getCacheHandle(), keys, computeResolver, msecs);
            status = tuple.first;
            values = tuple.second;

            postComputer.perform(values);
        }

        return !values.isEmpty() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? values
                : ExceptionMessageFactory.throwFutureException(status, keys, streamer.get().getStatus().getException());
    }

    public static <K, V> V computeValueIfAlgorithm(Supplier<RxStreamer> streamer, RxCache<K, V> reader, VoidStrategy1<Optional<V>> postComputer, K key, Strategy1<Boolean, V> computeResolver, long msecs) {
        Optional<V> value = Optional.empty();

        FutureStatus status = FutureStatus.NotStarted;
        if (computeResolver.perform(reader.read(key).orElse(null))) {
            Tuple2<FutureStatus, Optional<V>> tuple = streamer.get().controller().computeIf(reader.getCacheHandle(), key, computeResolver, msecs);
            status = tuple.first;
            value = tuple.second;

            postComputer.perform(value);
        }

        V raw = value.orElse(null);
        return raw != null
                ? raw
                : ExceptionMessageFactory.throwFutureException(status, key, streamer.get().getStatus().getException());
    }

    public static <K, V> List<V> computeListIfEmptyAlgorithm(Supplier<RxStreamer> streamer, Reader<K, V> reader, FaultPolicy faultPolicy, long msecs) {
        List<V> values = reader.readAsList();

        FutureStatus status = FutureStatus.NotStarted;
        if (values == null || values.isEmpty()) {
            status = streamer.get().subscribe().waitForResult(msecs);
            values = reader.readAsList();
        }

        return values != null && !values.isEmpty() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? values
                : ExceptionMessageFactory.throwFutureException(status, "no-key", streamer.get().getStatus().getException());
    }

    public static <K, V> Map<K, V> computeIfEmptyAlgorithm(Supplier<RxStreamer> streamer, Reader<K, V> reader, FaultPolicy faultPolicy, long msecs) {
        Map<K, V> values = reader.readAll();

        FutureStatus status = FutureStatus.NotStarted;
        if (values == null || values.isEmpty()) {
            status = streamer.get().subscribe().waitForResult(msecs);
            values = reader.readAll();
        }

        return values != null && !values.isEmpty() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? values
                : ExceptionMessageFactory.throwFutureException(status, "no-key", streamer.get().getStatus().getException());
    }

    public static <K, V> Optional<V> computeAlgorithm(Supplier<RxStreamer> streamer, RxCache<K, V> reader, FaultPolicy faultPolicy, VoidStrategy1<Optional<V>> postComputer, K key, long msecs) {
        FutureStatus status = streamer.get().subscribe().waitForResult(msecs);
        Optional<V> value = reader.read(key);

        postComputer.perform(value);

        return value.isPresent() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? value
                : ExceptionMessageFactory.throwFutureException(status, key, streamer.get().getStatus().getException());
    }

    public static <K, V> Map<K, V> computeAlgorithm(Supplier<RxStreamer> streamer, RxCache<K, V> reader, FaultPolicy faultPolicy, VoidStrategy1<Map<K, V>> postComputer, Iterable<K> keys, long msecs) {
        FutureStatus status = streamer.get().subscribe().waitForResult(msecs);
        Map<K, V> values = reader.read(keys);

        postComputer.perform(values);

        return !values.isEmpty() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? values
                : ExceptionMessageFactory.throwFutureException(status, keys, streamer.get().getStatus().getException());
    }

    public static <K, V> V computeValueAlgorithm(Supplier<RxStreamer> streamer, RxCache<K, V> reader, VoidStrategy1<Optional<V>> postComputer, K key, long msecs) {
        FutureStatus status = streamer.get().subscribe().waitForResult(msecs);
        Optional<V> value = reader.read(key);

        postComputer.perform(value);

        return value.orElseGet(() -> ExceptionMessageFactory.throwFutureException(status, key, streamer.get().getStatus().getException()));
    }

    public static <K, V> List<V> computeListAlgorithm(Supplier<RxStreamer> streamer, Reader<K, V> reader, FaultPolicy faultPolicy, long msecs) {
        FutureStatus status = streamer.get().subscribe().waitForResult(msecs);
        List<V> values = reader.readAsList();

        return values != null && !values.isEmpty() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? values
                : ExceptionMessageFactory.throwFutureException(status, "no-key", streamer.get().getStatus().getException());
    }

    public static <K, V> Map<K, V> computeAlgorithm(Supplier<RxStreamer> streamer, Reader<K, V> reader, FaultPolicy faultPolicy, long msecs) {
        FutureStatus status = streamer.get().subscribe().waitForResult(msecs);
        Map<K, V> values = reader.readAll();

        return values != null && !values.isEmpty() || Objects.equals(status, FutureStatus.Success) || faultPolicy.isIgnored(status)
                ? values
                : ExceptionMessageFactory.throwFutureException(status, "no-key", streamer.get().getStatus().getException());
    }

    public static void computeSyncPoint(StreamerBuilder streamerBuilder, long msecs) {
        Streamer streamer = streamerBuilder.streamer();

        if (!streamer.state().async.isInitial() && streamer.state().async.computeIfInitial().currentGroup.get() != null) {
            streamer.state().async.computeIfInitial().currentGroup.set(null);
            int currentGroupIndex = streamer.state().async.computeIfInitial().asyncChain.size();

            StreamerGroup sync = new StreamerGroup(streamer.getMasterCacheId(), streamer.getRxConfig(), addSequentialGroup(streamer), streamerBuilder);
            sync.run(
                    () -> {
                        FutureStatus status = streamer.asyncController().waitForGroupN(currentGroupIndex, msecs);
                        if (status != FutureStatus.Success) {
                            throw new IllegalStateException("Sync point wait for async group failed. Execution status: " + status);
                        }
                    }
            );
        }
    }

    public static ActGroup addSequentialGroup(Streamer streamer) {
        ActGroup group = ActGroup.sequential();
        streamer.state().chain.add(group);
        return group;
    }

    public static ActGroup addParallelGroup(Streamer streamer) {
        ActGroup group = ActGroup.parallel();
        streamer.state().chain.add(group);
        return group;
    }

    private StreamerAlgorithms() {
    }
}
