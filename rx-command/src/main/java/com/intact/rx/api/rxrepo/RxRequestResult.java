package com.intact.rx.api.rxrepo;

import java.util.Optional;

public interface RxRequestResult<K, V> {
    RxRequestStatus getStatus();

    boolean isNull();

    boolean isNotNull();

    Optional<V> value();

    K key();
}
