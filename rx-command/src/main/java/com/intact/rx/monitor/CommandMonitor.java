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

public class CommandMonitor implements InitializeMethods {
    private static final Logger log = LoggerFactory.getLogger(CommandMonitor.class);
    private static final CommandMonitor instance = new CommandMonitor();

    private final AtomicBoolean initialized;
    private final CommandPolicy policy;
    private final CommandController<Void> controller;

    private CommandMonitor() {
        this.initialized = new AtomicBoolean(false);
        this.policy = CommandPolicy.runForever(Interval.ofMinutes(1), Timeout.ofSixtySeconds());
        this.controller = CommandFactory.createController(CommandControllerPolicy.parallel(), RxThreadPoolFactory.monitorPool());
    }

    public static CommandMonitor instance() {
        DoubleCheckedLocking.initialize(instance);
        return instance;
    }

    @Override
    public boolean isInitialized() {
        return initialized.get();
    }

    @Override
    public boolean initialize() {
        controller.addCommand(CommandFactory.createCommand(policy, CommandMonitorAlgorithms::monitorControllers));
        controller.subscribe();

        log.info("rx-command command monitor initialized");

        initialized.set(true);
        return true;
    }
}
