package com.intact.rx.api.cache;

import com.intact.rx.api.cache.observer.SelectionObserver;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;

@SuppressWarnings("UnusedReturnValue")
public interface RxSelection<K, V> extends ReaderWriter<K, V>, AutoCloseable {

    Object getId();
    
    CacheHandle getCacheId();

    boolean detach();

    boolean isAttached();

    boolean connect(SelectionObserver<V> observer);

    boolean disconnect(SelectionObserver<V> observer);

    void disconnectAll();

    RxSelection<K, V> onObjectInDo(VoidStrategy1<V> strategy);

    RxSelection<K, V> onObjectOutDo(VoidStrategy1<V> strategy);

    RxSelection<K, V> onObjectModifiedDo(VoidStrategy1<V> strategy);

    RxSelection<K, V> onDetachDo(VoidStrategy0 strategy);
}
