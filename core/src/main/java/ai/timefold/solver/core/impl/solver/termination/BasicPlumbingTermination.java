package ai.timefold.solver.core.impl.solver.termination;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

/**
 * Concurrency notes:
 * Condition predicate on ({@link #problemChangeQueue} is not empty or {@link #terminatedEarly} is true).
 * The lock is required by {@link #waitForRestartSolverDecision()} (it waits),
 * by {@link #terminateEarly()} and {@link #addProblemChanges(List)} (they notify the waiting thread),
 * and by the problem-change processing methods: {@link #isEveryProblemChangeProcessed()} must read the queue
 * and {@link #problemChangesBeingProcessed} together, while another thread adds a change.
 * The other methods use only volatile fields, so they do not take the lock;
 * {@link #isSolverTerminated(SolverScope)} runs on every step, also in child threads.
 */
@NullMarked
public final class BasicPlumbingTermination<Solution_>
        extends AbstractUniversalTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private final boolean daemon;
    private final BlockingQueue<ProblemChange<Solution_>> problemChangeQueue = new LinkedBlockingQueue<>();

    /**
     * The answer for the run summary.
     * {@link #solvingEnded(SolverScope)} takes it; {@link #waitForRestartSolverDecision()} can only set it to true.
     * The wait reads the queue under the lock, so a problem change added later by another thread does not change it.
     */
    private volatile boolean runTerminatedEarly = false;
    private volatile boolean terminatedEarly = false;
    private boolean problemChangesBeingProcessed = false;

    public BasicPlumbingTermination(boolean daemon) {
        this.daemon = daemon;
    }

    /**
     * This method is thread-safe.
     */
    public void solvingStarted() {
        terminatedEarly = false;
        runTerminatedEarly = false;
    }

    /**
     * Called before any solver listener, so a {@link #terminateEarly()} after the phases ended does not count,
     * unless it prevents a restart.
     */
    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        runTerminatedEarly = terminatedEarly;
    }

    /**
     * This method is thread-safe.
     * <p>
     * Concurrency note: unblocks {@link #waitForRestartSolverDecision()}.
     *
     * @return true if successful
     */
    public synchronized boolean terminateEarly() {
        var terminationEarlySuccessful = !terminatedEarly;
        terminatedEarly = true;
        notifyAll();
        return terminationEarlySuccessful;
    }

    /**
     * This method is thread-safe.
     *
     * @return true once a stop was requested, also after the run ended;
     *         {@link #isRunEndedByTerminateEarly()} tells if that request ended the run
     */
    public boolean isTerminateEarlyRequested() {
        return terminatedEarly;
    }

    /**
     * If this returns true, then the problemFactChangeQueue is definitely not empty.
     * <p>
     * Concurrency note: Blocks until {@link #problemChangeQueue} is not empty or {@link #terminatedEarly} is true.
     *
     * @return true if the solver needs to be restarted
     */
    public synchronized boolean waitForRestartSolverDecision() {
        if (!daemon) {
            runTerminatedEarly |= terminatedEarly && !problemChangeQueue.isEmpty();
            return !problemChangeQueue.isEmpty() && !terminatedEarly;
        } else {
            while (problemChangeQueue.isEmpty() && !terminatedEarly) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Solver thread was interrupted during Object.wait().", e);
                }
            }
            // A daemon leaves the wait without a restart only because of terminateEarly().
            runTerminatedEarly |= terminatedEarly;
            return !terminatedEarly;
        }
    }

    /**
     * @return true if {@link #terminateEarly()} ended the run: before its phases ended, or by preventing a restart
     */
    public boolean isRunEndedByTerminateEarly() {
        return runTerminatedEarly;
    }

    /**
     * Concurrency note: unblocks {@link #waitForRestartSolverDecision()}.
     *
     * @param problemChangeList never null
     * @return as specified by {@link Collection#add}
     */
    public synchronized boolean addProblemChanges(List<ProblemChange<Solution_>> problemChangeList) {
        var added = problemChangeQueue.addAll(problemChangeList);
        notifyAll();
        return added;
    }

    public synchronized BlockingQueue<ProblemChange<Solution_>> startProblemChangesProcessing() {
        problemChangesBeingProcessed = true;
        return problemChangeQueue;
    }

    public synchronized void endProblemChangesProcessing() {
        problemChangesBeingProcessed = false;
    }

    public synchronized boolean isEveryProblemChangeProcessed() {
        return problemChangeQueue.isEmpty() && !problemChangesBeingProcessed;
    }

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        // Destroying a thread pool with solver threads will only cause it to interrupt those solver threads,
        // it won't call Solver.terminateEarly()
        if (Thread.currentThread().isInterrupted() // Does not clear the interrupted flag
                // Avoid duplicate log message because this method is called twice:
                // - in the phase step loop (every phase termination bridges to the solver termination)
                // - in the solver's phase loop
                // Child threads interrupted at the same time can still both log; there is no lock here.
                && !terminatedEarly) {
            logger.info("The solver thread got interrupted, so this solver is terminating early.");
            terminatedEarly = true;
        }
        return terminatedEarly || !problemChangeQueue.isEmpty();
    }

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return -1.0; // Not supported
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return isSolverTerminated(phaseScope.getSolverScope());
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return calculateSolverTimeGradient(phaseScope.getSolverScope());
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return this;
    }

    @Override
    public String toString() {
        return "BasicPlumbing()";
    }

}
