package com.intact.rx.monitor;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.core.command.CommandControllerPolicy;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.core.command.factory.CommandFactory;
import com.intact.rx.core.machine.factory.RxThreadPoolFactory;
import com.intact.rx.policy.Interval;
import com.intact.rx.templates.DoubleCheckedLocking;
import com.intact.rx.templates.api.InitializeMethods;

public class CacheMonitor implements InitializeMethods {
    private static final Logger log = LoggerFactory.getLogger(CacheMonitor.class);
    private static final CacheMonitor instance = new CacheMonitor();

    private final AtomicBoolean isInitialized;
    private final CommandPolicy commandPolicy;
    private final CommandController<Void> controller;

    private CacheMonitor() {
        this.isInitialized = new AtomicBoolean(false);
        this.commandPolicy = CommandPolicy.runForever(Interval.delayOfMillis(10000, 5000));
        this.controller = CommandFactory.createController(CommandControllerPolicy.sequential(), RxThreadPoolFactory.monitorPool());
    }

    public static CacheMonitor instance() {
        DoubleCheckedLocking.initialize(instance);
        return instance;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized.get();
    }

    @Override
    public boolean initialize() {
        controller.addCommand(CommandFactory.createCommand(commandPolicy, CacheMonitorAlgorithms::monitorAllCaches));
        controller.subscribe();

        log.info("rx-command cache monitor initialized");

        isInitialized.set(true);
        return true;
    }
}
