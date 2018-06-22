package com.intact.rx.core.command.observer;

import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.api.command.Action0;

public interface RxActionObserver<T> extends RxKeyValueObserver<Action0<T>, T> {
    void onFallback(Action0<T> action);
}
