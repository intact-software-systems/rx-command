package com.intact.rx.core.command.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.intact.rx.core.command.Commands;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.policy.MaxLimit;

public final class ComputationStrategies {

    public static <T> List<Command<T>> sequentialComputation(Commands<T> commands, MaxLimit maxLimit) {
        return findOneReadyCommand(commands);
    }

    public static <T> List<Command<T>> parallelComputation(Commands<T> commands, MaxLimit maxLimit) {
        int numCommandsToExecute = Math.max(0, maxLimit.getLimit() - commands.numCommandsExecuting());

        return findNReadyCommands(numCommandsToExecute, commands);
    }


    /**
     * Finds one ready command in FIFO order, but only if no other command is executing.
     */
    private static <T> List<Command<T>> findOneReadyCommand(Commands<T> commands) {
        if (commands.areCommandsExecuting()) {
            return Collections.emptyList();
        }

        return commands.stream()
                .filter(command -> !command.isExecuting() && command.isReady())
                .findFirst()
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }


    /**
     * Finds n ready commands in FIFO order, but only if no other command is executing.
     * <p>
     * TODO: Starvation
     */
    private static <T> List<Command<T>> findNReadyCommands(int nReadyCommands, Commands<T> commands) {
        List<Command<T>> readyCommands = new ArrayList<>();

        if (nReadyCommands > 0) {
            for (Command<T> command : commands) {
                if (!command.isExecuting() && command.isReady()) {
                    readyCommands.add(command);

                    //noinspection AssignmentToMethodParameter
                    --nReadyCommands;

                    if (nReadyCommands <= 0) {
                        break;
                    }
                }
            }
        }

        return readyCommands;
    }

    private ComputationStrategies() {
    }
}
