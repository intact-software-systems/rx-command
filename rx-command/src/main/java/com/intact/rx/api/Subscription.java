package com.intact.rx.api;

public interface Subscription {
    void request(long n);

    void cancel();
}
