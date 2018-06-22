package com.intact.rx.core.rxrepo;

import java.util.Optional;

import com.intact.rx.api.rxrepo.RxRequestResult;
import com.intact.rx.api.rxrepo.RxRequestStatus;

public class RepositoryRequestResult<K, V> implements RxRequestResult<K, V> {
    private final RepositoryRequestStatus status;
    private final V value;
    private final K key;

    private RepositoryRequestResult(K key, V value, RepositoryRequestStatus status) {
        this.status = status;
        this.value = value;
        this.key = key;
    }

    public static <K, V> RepositoryRequestResult<K, V> create(K key, V value, RepositoryRequestStatus status) {
        return new RepositoryRequestResult<>(key, value, status);
    }

    public static <K, V> RepositoryRequestResult<K, V> no(K key) {
        return new RepositoryRequestResult<>(key, null, RepositoryRequestStatus.no());
    }

    @Override
    public RxRequestStatus getStatus() {
        return status;
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @Override
    public boolean isNotNull() {
        return value != null;
    }

    @Override
    public Optional<V> value() {
        return Optional.ofNullable(value);
    }

    @Override
    public K key() {
        return key;
    }
}
