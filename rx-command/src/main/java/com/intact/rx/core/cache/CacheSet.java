package com.intact.rx.core.cache;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.ReaderWriter;

public class CacheSet<T> extends AbstractSet<T> {
    private final ReaderWriter<T, T> cache;

    public CacheSet(ReaderWriter<T, T> cache) {
        this.cache = requireNonNull(cache);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return cache.containsKey((T) o);
    }

    @Override
    public Iterator<T> iterator() {
        return cache.readAsList().iterator();
    }

    @Override
    public Object[] toArray() {
        return cache.readAsList().toArray();
    }

    @Override
    public boolean add(T t) {
        cache.write(t, t);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return cache.take((T) o).isPresent();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(o -> cache.containsKey((T) o));
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        Map<T, T> all = new HashMap<>(c.size());
        c.forEach(o -> all.put(o, o));
        cache.writeAll(all);
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        List<T> remove = c.stream().map(o -> (T) o).filter(Objects::nonNull).filter(t -> !cache.containsKey(t)).collect(Collectors.toList());
        return !cache.take(remove).isEmpty();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        List<T> keys = c.stream().map(o -> (T) o).filter(Objects::nonNull).collect(Collectors.toList());
        return cache.take(keys).size() == c.size();
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
