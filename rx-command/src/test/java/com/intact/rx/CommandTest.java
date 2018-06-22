package com.intact.rx;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.command.CommandResult;
import com.intact.rx.core.command.CommandControllerPolicy;
import com.intact.rx.core.command.CommandPolicy;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.api.CommandController;
import com.intact.rx.core.command.factory.CommandFactory;
import com.intact.rx.policy.Attempt;
import com.intact.rx.policy.Interval;
import com.intact.rx.policy.Timeout;
import com.intact.rx.templates.Utility;
import com.intact.rx.testdata.command.*;

class CommandTest {

    private static final Logger log = LoggerFactory.getLogger(CommandTest.class);
    private static final int defaultWaitTimeMsecs = 50000;

    @Test
    void testSingleCommand() {
        final ServiceImpl service = new ServiceImpl();
        final CommandResult<Result> commandResult = CommandFactory.executeActions(Utility.toArray(new SimpleAction0(service)), null, CommandControllerPolicy.parallel(), CommandPolicy.runOnceNow());

        boolean isDone = commandResult.waitFor(defaultWaitTimeMsecs);
        assertTrue(isDone);
        assertTrue(!commandResult.result().isEmpty());
        assertEquals("result", commandResult.result().get(0).getResult());
    }

    @Test
    void testTimeOutCommand() {
        final ServiceImpl service = new ServiceImpl();
        final CommandResult<Result> commandResult = CommandFactory.executeActions(Utility.toArray(new TimeOutAction(service)), null, CommandControllerPolicy.parallel(), CommandPolicy.runOnceNow());

        boolean isDone = commandResult.waitFor(10);
        assertFalse(isDone);
        assertFalse(commandResult.isSuccess());
        assertTrue(commandResult.result().isEmpty());
    }

    @Test
    void testFailingCommand() {
        CommandPolicy commandPolicy = CommandPolicy.runOnceNow();
        long minWaitTimeMsecs = commandPolicy.getInterval().getInitialDelayMs() + (commandPolicy.getAttempt().getMaxNumAttempts() - 1) * commandPolicy.getRetryInterval().getPeriodMs();
        long overheadMarginMsecs = 1000;

        final CommandResult<Result> commandResult = CommandFactory.executeActions(Utility.toArray(new ExceptionThrowingAction()), null, CommandControllerPolicy.parallel(), commandPolicy);
        boolean isDone = commandResult.waitFor(minWaitTimeMsecs + overheadMarginMsecs);

        assertTrue(isDone);
        assertFalse(commandResult.isSuccess());
        assertTrue(commandResult.result().isEmpty());
    }

    @Test
    void cancelCommand() {
        final ServiceInterface service = new ServiceImpl();
        final CommandResult<Result> commandResult = CommandFactory.executeActions(Utility.toArray(new TimeOutAction(service)), null, CommandControllerPolicy.parallel(), CommandPolicy.runOnceNow());
        commandResult.cancel();
        assertTrue(commandResult.isDone(), "Command not done: " + commandResult);
        assertFalse(commandResult.isSuccess(), "Command not failed: " + commandResult);
    }

    @Test
    void testBeforeExecute() {
        final CommandResult<List<String>> commandResult = CommandFactory.executeActions(Utility.toArray(new BeforeExecuteAction()), null, CommandControllerPolicy.parallel(), CommandPolicy.runOnceNow());
        assertTrue(commandResult.waitFor(defaultWaitTimeMsecs));
        // first value added in beforeExecute hook
        assertTrue(commandResult.result().get(0).size() == 2 && Objects.equals(commandResult.result().get(0).get(0), "beforeExecute"));
    }

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    @Test
    void testControllerInterval() {
        final int[] i = {0};

        int controllerRounds = 3;
        int commandRounds = 10;

        CommandController<Void> controller = CommandFactory.createController(CommandControllerPolicy.parallelAnd(Attempt.numSuccessfulTimes(controllerRounds, controllerRounds), Interval.nowThenOfMillis(100), Timeout.ofSixtySeconds()));
        Command<Void> command = CommandFactory.createCommand(CommandPolicy.runNTimes(Interval.nowThenOfMillis(10), commandRounds), () -> log.info("Executed {}", ++i[0]));
        controller.addCommand(command);

        CommandResult<Void> result = controller.subscribe();

        boolean success = result.waitFor(defaultWaitTimeMsecs);
        assertTrue(success);
        assertEquals(i[0], controllerRounds * commandRounds);
    }
}
