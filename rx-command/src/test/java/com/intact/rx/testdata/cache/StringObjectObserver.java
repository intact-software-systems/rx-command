package com.intact.rx.testdata.cache;

import com.intact.rx.api.cache.observer.ObjectObserver;

public class StringObjectObserver implements ObjectObserver<Object, String> {

    private String object;

    @Override
    public void onObjectCreated(Object o, String object) {
        this.object = object;
    }

    @Override
    public void onObjectRemoved(Object o, String object) {
        this.object = object;
    }

    @Override
    public void onObjectModified(Object o, String object) {
        this.object = object;
    }

    public String getObject() {
        return this.object;
    }
}
