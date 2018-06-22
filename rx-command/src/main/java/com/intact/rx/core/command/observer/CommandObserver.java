package com.intact.rx.core.command.observer;

import com.intact.rx.api.RxKeyValueObserver;
import com.intact.rx.core.command.api.Command;

public interface CommandObserver<T> extends RxKeyValueObserver<Command<T>, T> {
}
