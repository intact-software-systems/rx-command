package com.intact.rx.testdata.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.observer.SelectionObserver;

public class ResultSelectionObserver implements SelectionObserver<Result> {
    private static final Logger log = LoggerFactory.getLogger(ResultSelectionObserver.class);

    @Override
    public void onObjectIn(Result value) {
        log.debug("In result {}", value.getId());
    }

    @Override
    public void onObjectOut(Result value) {
        log.debug("Out result {}", value.getId());
    }

    @Override
    public void onObjectModified(Result value) {
        log.debug("Modified result {}", value.getId());
    }

    @Override
    public void onDetach() {
        log.debug("Detached from cache");
    }
}
