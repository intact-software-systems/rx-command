package com.intact.rx.templates.api;

import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.VoidStrategy0;
import com.intact.rx.api.command.VoidStrategy1;

public interface RxObserverSupport<Subject, T> {

    Subject onCompleteDo(VoidStrategy0 completedFunction);

    Subject onErrorDo(VoidStrategy1<Throwable> errorFunction);

    Subject onNextDo(VoidStrategy1<T> nextFunction);

    Subject onSubscribeDo(VoidStrategy1<Subscription> subscribeFunction);
}
