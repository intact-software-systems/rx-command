package com.intact.rx.core.machine.api;

import com.intact.rx.api.RxObserver;

public interface Scheduler extends RxObserver<Long>, Runnable {
}
