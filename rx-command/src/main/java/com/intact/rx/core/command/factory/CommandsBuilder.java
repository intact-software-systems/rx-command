package com.intact.rx.core.command.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.RxObserver;
import com.intact.rx.core.command.CommandControllerPolicy;
import com.intact.rx.core.command.Commands;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.core.machine.context.RxThreadPoolConfig;
import com.intact.rx.core.machine.factory.RxThreadPoolFactory;

public class CommandsBuilder<T> {
    private final CommandControllerPolicy commandControllerPolicy;

    private final AtomicReference<RxThreadPoolConfig> rxThreadPoolConfig = new AtomicReference<>(RxDefault.getDefaultCommandPoolConfig());
    private final Collection<RxObserver<T>> observers = new ArrayList<>();

    private final Collection<Command<T>> commands = new ArrayList<>();

    private CommandsBuilder(CommandControllerPolicy commandPolicy) {
        this.commandControllerPolicy = commandPolicy;
    }

    // --------------------------------------------
    // Builder with fluent API
    // --------------------------------------------

    public static <K, V> CommandsBuilder<V> withPolicy(CommandControllerPolicy commandPolicy) {
        return new CommandsBuilder<>(commandPolicy);
    }

    public CommandsBuilder<T> add(Command<T> command) {
        commands.add(command);
        return this;
    }

    public CommandsBuilder<T> withRxThreadPoolConfig(RxThreadPoolConfig rxThreadPoolConfig) {
        this.rxThreadPoolConfig.set(rxThreadPoolConfig);
        return this;
    }

    public CommandsBuilder<T> connect(RxObserver<T> observer) {
        this.observers.add(observer);
        return this;
    }

    public Commands<T> buildCommands() {
        return new Commands<>(commands);
    }

    public CommandController<T> build() {
        CommandController<T> commandController = CommandFactory.createController(commandControllerPolicy, RxThreadPoolFactory.computeIfAbsent(rxThreadPoolConfig.get()));
        if (!commands.isEmpty()) {
            commandController.setCommands(new Commands<>(commands));
        }
        this.observers.forEach(commandController::connect);
        return commandController;
    }
}
