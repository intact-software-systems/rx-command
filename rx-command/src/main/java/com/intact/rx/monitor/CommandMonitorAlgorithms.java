package com.intact.rx.monitor;

import java.lang.ref.WeakReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.core.cache.data.ObjectRoot;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.core.command.factory.CommandFactory;

final class CommandMonitorAlgorithms {

    private static final Logger log = LoggerFactory.getLogger(CommandMonitorAlgorithms.class);

    static void monitorControllers() {
        int recentlyGarbageCollected = 0;
        for (ObjectRoot<Object, WeakReference<CommandController<Object>>> objectRoot : CommandFactory.monitorCache().getRoots()) {
            WeakReference<CommandController<Object>> reference = objectRoot.getValueNoStatusUpdate();

            if (reference == null) {
                log.warn("Weak reference to CommandController was null!");
                objectRoot.setExpired();
            } else {
                CommandController<Object> commandController = reference.get();
                if (commandController == null) {
                    objectRoot.setExpired();
                    ++recentlyGarbageCollected;
                } else if (commandController.isExecuting()) {
                    commandController
                            .getCommands()
                            .stream()
                            .filter(Command::isPolicyViolated)
                            .forEach(command -> log.warn("Command policy violated {} attached to controller {}", command, commandController));
                }
            }
        }

        if (CommandFactory.monitorCache().size() > 1000 || recentlyGarbageCollected > 50) {
            log.info("# of CommandControllers {}", CommandFactory.monitorCache().size());
            log.info("# of CommandControllers recently garbage collected {}", recentlyGarbageCollected);
        }
    }

    private CommandMonitorAlgorithms() {
    }
}
