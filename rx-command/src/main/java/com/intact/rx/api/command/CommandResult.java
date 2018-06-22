package com.intact.rx.api.command;

import java.util.List;
import java.util.concurrent.Future;

import com.intact.rx.api.FutureStatus;
import com.intact.rx.templates.Tuple2;

public interface CommandResult<T> extends Future<List<T>> {

    List<T> result();

    boolean isDone();

    boolean isSuccess();

    boolean isSubscribed();

    boolean isValid();

    default boolean cancel() {
        return cancel(true);
    }

    /**
     * @param msecs max wait time
     * @return true if done
     */
    boolean waitFor(long msecs);

    Tuple2<FutureStatus, List<T>> getResult(long msecs);
}
