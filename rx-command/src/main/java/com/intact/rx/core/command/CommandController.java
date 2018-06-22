package com.intact.rx.core.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.RxObserver;
import com.intact.rx.api.Subscription;
import com.intact.rx.api.command.CommandResult;
import com.intact.rx.core.command.api.Command;
import com.intact.rx.core.command.observer.CommandObserver;
import com.intact.rx.core.command.result.CommandControllerResult;
import com.intact.rx.core.command.result.CommandFutureResult;
import com.intact.rx.core.command.strategy.ExecutionPolicyChecker;
import com.intact.rx.core.machine.RxThreadPool;
import com.intact.rx.exception.ExecutionEndedOnErrorException;
import com.intact.rx.exception.FatalException;
import com.intact.rx.templates.ContextObject;
import com.intact.rx.templates.api.Context;

/**
 * Algorithm design:
 * - ThreadPool implementation only allows a command controller to trigger itself.
 * - If commands trigger this, then schedule queue will grow infinitely.
 * - Only when CommandController is not running is someone else allowed to trigger it.
 */
@SuppressWarnings({"SynchronizedMethod", "SynchronizeOnThis", "AccessToStaticFieldLockedOnInstance"})
public class CommandController<T> implements
        Runnable,
        com.intact.rx.core.command.api.CommandController<T>,
        CommandObserver<T>,
        Subscription {
    private static final long ACQUIRE_LOCK_TIMEOUT_IN_MS = 5000L;
    private static final Logger log = LoggerFactory.getLogger(CommandController.class);

    private final Context<CommandControllerPolicy, CommandControllerState<T>> context;

    public CommandController(CommandControllerPolicy commandControllerPolicy, RxThreadPool threadPool) {
        this.context = new ContextObject<>(commandControllerPolicy, new CommandControllerState<>(threadPool));
    }

    // -----------------------------------------------------------
    // Interface Runnable
    // -----------------------------------------------------------

    /**
     * Execute attached commands until they are done.
     * A command is done when its Attempt policy is met.
     */
    @Override
    public void run() {
        try {
            if (state().getControllerThreadPool() == null) {
                //noinspection ThrowCaughtLocally
                throw new IllegalStateException("RxThreadPool is null for CommandController:\n" + this);
            }

            scheduleFromRun();
        } catch (IllegalStateException exception) {
            log.error("Error while scheduling commands in CommandController:\n{}\nException:", this, exception);
            processFinishedWithFatalError(exception);
        } catch (Throwable throwable) {
            // TODO: There is a risk that an infinite loop occurs here ... implement a loop breaker. UPDATE: One loop breaker is the timeout policy.
            // AlarmChecker.isInInfiniteLoop(state().getExecutionStatus(), state().getCommands());
            log.error("Error while scheduling commands in CommandController:\n{}\nException:", this, throwable);

            state().getControllerThreadPool().schedule(this, this.computeMaxWaitTimeMs());
        }
    }

    // -----------------------------------------------------------
    // Interface Subscription
    // -----------------------------------------------------------

    @Override
    public void request(long n) {
        subscribe();
    }

    @Override
    public void cancel() {
        unsubscribe();
    }

    // -----------------------------------------------------------
    // Interface CommandController
    // -----------------------------------------------------------

    @Override
    public synchronized boolean setCommands(Commands<T> commands) {
        return !state().getExecutionStatus().isExecuting() && state().setCommands(commands);
    }

    @Override
    public synchronized boolean addCommand(com.intact.rx.core.command.api.Command<T> command) {
        return !state().getExecutionStatus().isExecuting() && state().addCommand(command);
    }

    @Override
    public synchronized Commands<T> getCommands() {
        return new Commands<>(state().getCommands());
    }

    @Override
    public synchronized CommandResult<T> subscribe() {
        if (state().getCommands().isEmpty()) {
            throw new IllegalStateException("No commands to execute in CommandController");
        }
        if (state().getExecutionStatus().isExecuting()) {
            //noinspection AccessToStaticFieldLockedOnInstance
            log.debug("Controller already executing.");
            return state().getCommandResult();
        }

        if (!lockSchedulingMutex(ACQUIRE_LOCK_TIMEOUT_IN_MS)) {
            log.warn("Could not acquire subscribe lock within " + ACQUIRE_LOCK_TIMEOUT_IN_MS + " ms");
            return state().getCommandResult();
        }

        try {
            if (state().getExecutionStatus().isExecuting()) {
                log.info("Already executing. Returning with subscribe lock");
                return state().getCommandResult();
            }

            return resetAndInitSubscription();
        } finally {
            state().schedulingMutex().unlock();
        }
    }

    @Override
    public boolean unsubscribe() {
        if (!state().getExecutionStatus().isExecuting()) {
            return true;
        }

        synchronized (this) {
            cancelPrivate();
            return !state().getCommands().isEmpty();
        }
    }

    @Override
    public synchronized CommandResult<T> trigger() {
        if (state().getCommands().isEmpty()) {
            throw new IllegalStateException("No commands to execute in CommandController");
        }
        if (state().getCommands().getExecutionStatus().isExecuting()) {
            return state().getCommandResult();
        }

        final CommandFutureResult<T> commandResult = new CommandFutureResult<>();
        CommandResult<T> controllerResult = new CommandControllerResult<>(commandResult, this);
        state().getRxSubject().connect(commandResult);

        // Potential cyclic dependency if rx subject is never cleaned up until end of commandResult lifecycle
        if (!state().getTriggered().get()) {
            state().getTriggered().set(true);
            state().getExecutionStatus().start();
            state().getControllerThreadPool().schedule(this, 0L);
        }

        return controllerResult;
    }

    @Override
    public boolean isDone() {
        return isDonePrivate();
    }

    @Override
    public boolean isSuccess() {
        return isSuccessPrivate();
    }

    @Override
    public boolean isExecuting() {
        return state().getExecutionStatus().isExecuting();
    }

    @Override
    public boolean areCommandsExecuting() {
        return state().getCommands().areCommandsExecuting();
    }

    @Override
    public int numCommandsExecuting() {
        return state().getCommands().numCommandsExecuting();
    }

    @Override
    public long timeoutInMs() {
        return ExecutionPolicyChecker.computeTimeUntilTimeoutMs(state().getExecutionStatus(), config().getTimeout());
    }

    @Override
    public Collection<Throwable> getExceptions() {
        return state().getCommands().getExceptions();
    }

    /**
     * Set thread pool for commands to avoid CommandController starvation, which
     * occurs when commands use all threads in thread-pool.
     */
    @Override
    public synchronized boolean setCommandThreadPool(RxThreadPool commandThreadPool) {
        if (state().getExecutionStatus().isExecuting()) {
            return false;
        }

        state().setCommandThreadPool(commandThreadPool);
        return true;
    }

    // -----------------------------------------------------------
    // Interface CommandObserver <T>
    // -----------------------------------------------------------

    @Override
    public void onNext(com.intact.rx.core.command.api.Command<T> command, T values) {
        // Precondition: if (!state().getExecutionStatus().isCancelled()) Validate.assertTrue(state().getExecutionStatus().isExecuting());
        // Precondition: Validate.assertTrue(!state().getCommands().isDone());

        state().onNext(values);
    }

    @Override
    public void onSubscribe(com.intact.rx.core.command.api.Command<T> command, Subscription subscription) {
    }

    @Override
    public void onComplete(com.intact.rx.core.command.api.Command<T> command) {
        // Precondition: if (!state().getExecutionStatus().isCancelled()) Validate.assertTrue(state().getExecutionStatus().isExecuting());

        if (isInLastExecution()) {
            state().getControllerThreadPool().cleanupScheduleQueueAndSchedule(this, 0);
        }
    }

    @Override
    public void onError(com.intact.rx.core.command.api.Command<T> command, Throwable throwable) {
        // Precondition: if (!state().getExecutionStatus().isCancelled()) Validate.assertTrue(state().getExecutionStatus().isExecuting());
        this.state().getCommands().addException(command, throwable);

        if (throwable instanceof FatalException) {
            log.debug("Stopping command controller! Fatal exception thrown from command [{}]", command, throwable);
            processFinishedWithFatalError(throwable);
        } else {
            log.debug("Error encountered while executing command [{}]", command, throwable);

            if (isInLastExecution()) {
                state().getControllerThreadPool().cleanupScheduleQueueAndSchedule(this, 0);
            }
        }
    }

    private boolean isInLastExecution() {
        return ExecutionPolicyChecker.isNExecutionsLeft(state().getCommands().getExecutionStatus(), config().getAttempt(), 1);
    }

    // -----------------------------------------------------------
    // Attach and detach observers
    // -----------------------------------------------------------

    @Override
    public void disconnectAll() {
        state().getRxSubject().disconnectAll();
    }

    @Override
    public boolean connect(RxObserver<T> observer) {
        return state().getRxSubject().connect(observer);
    }

    @Override
    public boolean disconnect(RxObserver<T> observer) {
        return state().getRxSubject().disconnect(observer);
    }

    // -----------------------------------------------------------
    // schedule and execute commands according to policies
    // -----------------------------------------------------------

    /**
     * Tries to acquire a scheduling mutex, gives up within a give threshold.
     */
    private boolean scheduleFromRun() {
        if (!lockSchedulingMutex(ACQUIRE_LOCK_TIMEOUT_IN_MS)) {
            log.warn("Could not acquire scheduling lock within " + ACQUIRE_LOCK_TIMEOUT_IN_MS + " ms");
            return false;
        }

        try {
            List<com.intact.rx.core.command.api.Command<T>> scheduled = schedule();
            return !scheduled.isEmpty();
        } finally {
            state().schedulingMutex().unlock();
        }
    }

    /**
     * Execute attached commands until they are done. A command is done when its Attempt policy is met.
     */
    private List<com.intact.rx.core.command.api.Command<T>> schedule() {
        if (isDonePrivate()) {
            stopScheduling();
            return Collections.emptyList();
        } else {
            if (isCommandGroupDone()) {
                boolean hasNext = nextExecution();
                if (!hasNext) {
                    log.debug("Ingoring prepare next execution.");
                    // How is this possible? Should this ever hit?
                }

                return Collections.emptyList();
            } else {
                List<com.intact.rx.core.command.api.Command<T>> executed = executeCommands();
                scheduleFollowup();
                return executed;
            }
        }
    }

    /**
     * Next schedule time. NB! waitTime == 0 should not be filtered out.
     */
    private void scheduleFollowup() {
        long waitTimeMs = this.computeMaxWaitTimeMs();
        if (waitTimeMs < Long.MAX_VALUE) {
            state().getControllerThreadPool().schedule(this, waitTimeMs);
        }
    }

    private boolean nextExecution() {
        // ------------------------------------------
        // Update commands execution status
        // ------------------------------------------
        if (!state().getTriggered().get()) {
            if (isSuccessPrivate()) {
                log.debug("Controller ended in success.");
                state().getCommands().getExecutionStatus().success();
            } else {
                log.debug("Controller ended in failure!");
                state().getCommands().getExecutionStatus().failure();
            }
        }

        // ------------------------------------------
        // Is there a next execution?
        // ------------------------------------------
        if (state().getTriggered().get() || ExecutionPolicyChecker.isInAttempt(state().getCommands().getExecutionStatus(), config().getAttempt())) {

            state().getCommands().reset();
            state().getTriggered().set(false);
            state().getCommands().getExecutionStatus().start();

            log.debug("Time spent to execute current command group: {}", state().getExecutionStatus().getTime().getTimeSinceLastExecutionTimeMs());
            //log.info("Controller status: " + state().getExecutionStatus());

            // ---------------------------------
            // schedule next using controller interval policy -- Use commands status?
            // ---------------------------------
            long waitTimeMs = ExecutionPolicyChecker.computeTimeUntilNextExecutionTimeMs(state().getExecutionStatus(), config().getInterval());
            if (waitTimeMs < Long.MAX_VALUE) {
                log.debug("New execution of command group in {} interval {} status {}", waitTimeMs, config().getInterval().getPeriodMs(), state().getExecutionStatus().getTime().getTimeSinceLastExecutionTimeMs());
                state().getControllerThreadPool().schedule(this, waitTimeMs);
            } else {
                log.debug("Ignoring scheduling time {}", waitTimeMs);
            }

            return true;
        }

        return false;
    }

    private void stopScheduling() {
        if (state().getExecutionStatus().isExecuting()) {
            if (isSuccessPrivate()) {
                processFinished();
            } else {
                if (isTimeoutPrivate()) {
                    doTimeoutPrivate();
                }

                processFinishedWithError(new ExecutionEndedOnErrorException("CommandController finished with errors for commands {}" + state().getCommands()));
            }
        } else {
            log.debug("CommandController is already done {}", this);
        }
    }

    /**
     * Execute all commands that can be executed according to policies
     */
    private List<com.intact.rx.core.command.api.Command<T>> executeCommands() {
        // --------------------------------------
        // Algorithm: command.doTimeout() leads to callback to this.onError(command, ..)
        // ---------------------------------------
        state().getCommands().stopTimeoutCommands();

        return executeCommands(
                config().getComputationStrategy().perform(state().getCommands(), config().getMaxConcurrentCommands())
        );
    }

    /**
     * Execute the commands if they are not currently executing
     * Note! "Not executing" should be enforced by computation strategies.
     */
    private List<com.intact.rx.core.command.api.Command<T>> executeCommands(@SuppressWarnings("rawtypes") Iterable<com.intact.rx.core.command.api.Command> commands) {
        List<com.intact.rx.core.command.api.Command<T>> executed = new ArrayList<>();
        //noinspection rawtypes
        for (com.intact.rx.core.command.api.Command command : commands) {
            if (!command.isExecuting() && command.isReady()) {
                @SuppressWarnings("unchecked") com.intact.rx.core.command.api.Command<T> cmd = requireNonNull((com.intact.rx.core.command.api.Command<T>) command);
                cmd.subscribe(this, state().getCommandThreadPool());
                executed.add(cmd);
            }
        }

        return executed;
    }

    /**
     * Compute max wait time based on timeout and interval policy: "The time when a command should be done with the current execution and is ready for another execution".
     */
    private long computeMaxWaitTimeMs() {
        return Math.min(
                ExecutionPolicyChecker.computeEarliestReadyInMs(state().getCommands().getExecutionStatus(), config().getAttempt(), config().getInterval(), config().getInterval(), config().getTimeout()),
                state().getCommands().earliestReadyInMs()
        );
    }

    /**
     * Check if controller has timed out according to policy, i.e., no more time left to finish all commands.
     */
    private boolean isTimeoutPrivate() {
        return ExecutionPolicyChecker.isTimeout(state().getExecutionStatus(), config().getTimeout());
    }

    /**
     * Check if controller is cancelled, timed out, or if there are any more commands left executing.
     */
    private boolean isDonePrivate() {
        if (state().getTriggered().get()) {
            log.debug("Controller was triggered externally");
            return false;
        }

        if (state().getExecutionStatus().isCancelled()) {
            return true;
        }

        if (isTimeoutPrivate()) {
            log.warn("CommandController timed out with commands: {}", state().getCommands());
            return true;
        }

        boolean inAttempt = ExecutionPolicyChecker.isInAttempt(state().getCommands().getExecutionStatus(), config().getAttempt());
        boolean isDone = state().getCommands().isDone();

        return isDone && !inAttempt;
    }

    private boolean isCommandGroupDone() {
        if (state().getExecutionStatus().isCancelled()) {
            return true;
        }

        if (isTimeoutPrivate()) {
            log.warn("CommandController timed out with commands: {}", state().getCommands());
            return true;
        }

        return state().getCommands().isDone();
    }

    /**
     * Check if commands are successfully executed.
     */
    private boolean isSuccessPrivate() {
        return state().getCommands().isSuccess();
    }

    /**
     * Cancel all attached commands and set execution state.
     */
    private void cancelPrivate() {
        state().getCommands().unsubscribe();
        state().getExecutionStatus().cancel();
    }

    /**
     * Controller times out, stop all commands.
     */
    private void doTimeoutPrivate() {
        log.warn("CommandController timed out: {}", this);
        state().getCommands().doTimeout();
        state().getExecutionStatus().cancel();
    }

    /**
     * Initialize commandResult and add as observer.
     * NB! Only when CommandController is not running is an outsider allowed to trigger it.
     */
    private CommandResult<T> resetAndInitSubscription() {
        state().resetSubscription();

        final CommandFutureResult<T> commandResult = new CommandFutureResult<>();
        state().setCommandResult(commandResult);

        state().getExecutionStatus().start();
        state().getCommands().getExecutionStatus().start();

        state().onSubscribe(this);
        state().getControllerThreadPool().schedule(this, 0L);

        return new CommandControllerResult<>(commandResult, this);
    }

    // -----------------------------------------------------------
    // private functions to set state, perform callbacks, etc
    // -----------------------------------------------------------

    private void processFinished() {
        // Precondition: Validate.assertTrue(state().getExecutionStatus().isExecuting());
        // Precondition: Validate.assertTrue(!state().getCommands().areCommandsExecuting());

        state().getCommands().getExecutionStatus().success();
        state().getExecutionStatus().success();

        state().getControllerThreadPool().cleanupScheduleQueueForRunnable(this);
        state().onComplete();
    }

    private void processFinishedWithError(Throwable throwable) {
        log.debug("CommandController finished with errors for commands {}", state().getCommands());
        state().getCommands().getExecutionStatus().failure();
        state().getExecutionStatus().failure();

        state().getControllerThreadPool().cleanupScheduleQueueForRunnable(this);
        state().onError(throwable);
    }

    private void processFinishedWithFatalError(Throwable throwable) {
        for (Command<T> command : state().getCommands()) {
            command.unsubscribe();
            state().getCommandThreadPool().errorRunnableCleanupQueueAndStopRunning(command, throwable);
        }

        processFinishedWithError(throwable);
    }

    // -----------------------------------------------------------
    // private functions
    // -----------------------------------------------------------

    private CommandControllerPolicy config() {
        return context.config();
    }

    private CommandControllerState<T> state() {
        return context.state();
    }

    private boolean lockSchedulingMutex(long lockTimeoutInMs) {
        try {
            if (!state().schedulingMutex().tryLock(lockTimeoutInMs, TimeUnit.MILLISECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for scheduling lock", e);
            return false;
        }
        return true;
    }

    // -----------------------------------------------------------
    // Overridden from Object
    // -----------------------------------------------------------

    @Override
    public String toString() {
        return "\n  CommandController@" + hashCode() + " {\n" +
                "       context=" + context +
                "\n }";
    }
}
