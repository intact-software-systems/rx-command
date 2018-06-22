package com.intact.rx.core.rxrepo.reader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.intact.rx.api.cache.RxCache;
import com.intact.rx.api.rxrepo.RxRepositoryReader;
import com.intact.rx.api.rxrepo.RxRepositoryReaderCollection;

public class RepositoryStreamReaderCollection<K, V> implements RxRepositoryReaderCollection<K, V> {
    private final Map<K, RxRepositoryReader<K, V>> readers = new ConcurrentHashMap<>();

    public RepositoryStreamReaderCollection(Map<K, RxRepositoryReader<K, V>> readers) {
        this.readers.putAll(readers);
    }

    @Override
    public Optional<RxCache<K, V>> cache(K requestKey) {
        RxRepositoryReader<K, V> reader = readers.get(requestKey);
        return reader != null ? Optional.ofNullable(reader.cache()) : Optional.empty();
    }

    @Override
    public Optional<V> computeIfAbsent(K key, long msecs) {
        RxRepositoryReader<K, V> reader = readers.get(key);
        return reader != null ? reader.computeIfAbsent(key, msecs) : Optional.empty();
    }

    @Override
    public Map<K, V> computeIfEmpty(long msecs) {
        Map<K, V> result = new HashMap<>();
        readers.forEach((key, reader) -> reader.computeIfAbsent(key, msecs).ifPresent(v -> result.put(key, v)));
        return result;
    }

    @Override
    public Optional<V> compute(K key, long msecs) {
        RxRepositoryReader<K, V> reader = readers.get(key);
        return reader != null ? reader.compute(key, msecs) : Optional.empty();
    }

    @Override
    public Map<K, V> compute(long msecs) {
        Map<K, V> result = new HashMap<>();
        readers.forEach((key, reader) -> reader.compute(key, msecs).ifPresent(v -> result.put(key, v)));
        return result;
    }

    @Override
    public void subscribe(K requestKey) {
        RxRepositoryReader<K, V> reader = readers.get(requestKey);
        if (reader != null) {
            reader.subscribe();
        }
    }

    @Override
    public void subscribe() {
        readers.values().forEach(RxRepositoryReader::subscribe);
    }
}
