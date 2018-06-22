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
import com.intact.rx.policy.Timeout;
import com.intact.rx.templates.DoubleCheckedLocking;
import com.intact.rx.templates.api.InitializeMethods;

public class MachineMonitor implements InitializeMethods {
    private static final Logger log = LoggerFactory.getLogger(MachineMonitor.class);
    private static final MachineMonitor instance = new MachineMonitor();

    private final AtomicBoolean initialized;
    private final CommandPolicy policy;
    private final CommandController<Void> controller;

    private MachineMonitor() {
        this.initialized = new AtomicBoolean(false);
        this.policy = CommandPolicy.runForever(Interval.delayOfMillis(13000, 60000), Interval.delayOfMillis(13000, 7000), Timeout.ofSixtySeconds());
        this.controller = CommandFactory.createController(CommandControllerPolicy.parallel(), RxThreadPoolFactory.monitorPool());
    }

    public static MachineMonitor instance() {
        DoubleCheckedLocking.initialize(instance);
        return instance;
    }

    @Override
    public boolean isInitialized() {
        return initialized.get();
    }

    @Override
    public boolean initialize() {
        controller.addCommand(CommandFactory.createCommand(policy, MachineMonitorAlgorithms::monitorThreadPools));
        controller.subscribe();

        log.info("rx-command machine monitor initialized");

        initialized.set(true);
        return true;
    }
}
