package com.intact.rx.core.cache;

import java.util.function.Supplier;

import com.intact.rx.api.cache.RxSet;
import com.intact.rx.core.cache.data.DataCache;

public class SetReaderWriter<T> extends CacheReaderWriter<T, T> implements RxSet<T> {
    public SetReaderWriter(Supplier<DataCache<T, T>> dataCacheSupplier) {
        super(dataCacheSupplier);
    }
}
