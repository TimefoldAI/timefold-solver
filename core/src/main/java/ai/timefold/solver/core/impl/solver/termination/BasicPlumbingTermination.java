package ai.timefold.solver.core.impl.solver.termination;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.change.ProblemChangeAdapter;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

/**
 * Concurrency notes:
 * Condition predicate on ({@link #problemChangeQueue} is not empty or {@link #terminatedEarly} is true).
 */
@NullMarked
public final class BasicPlumbingTermination<Solution_>
        extends AbstractUniversalTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private final boolean daemon;
    private final BlockingQueue<ProblemChangeAdapter<Solution_>> problemChangeQueue = new LinkedBlockingQueue<>();

    private boolean terminatedEarly = false;
    private boolean problemChangesBeingProcessed = false;

    public BasicPlumbingTermination(boolean daemon) {
        this.daemon = daemon;
    }

    /**
     * This method is thread-safe.
     */
    public synchronized void resetTerminateEarly() {
        terminatedEarly = false;
    }

    /**
     * This method is thread-safe.
     * <p>
     * Concurrency note: unblocks {@link #waitForRestartSolverDecision()}.
     *
     * @return true if successful
     */
    public synchronized boolean terminateEarly() {
        boolean terminationEarlySuccessful = !terminatedEarly;
        terminatedEarly = true;
        notifyAll();
        return terminationEarlySuccessful;
    }

    /**
     * This method is thread-safe.
     */
    public synchronized boolean isTerminateEarly() {
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
            return !terminatedEarly;
        }
    }

    /**
     * Concurrency note: unblocks {@link #waitForRestartSolverDecision()}.
     *
     * @param problemChangeList never null
     * @return as specified by {@link Collection#add}
     */
    public synchronized boolean addProblemChanges(List<ProblemChangeAdapter<Solution_>> problemChangeList) {
        boolean added = problemChangeQueue.addAll(problemChangeList);
        notifyAll();
        return added;
    }

    public synchronized BlockingQueue<ProblemChangeAdapter<Solution_>> startProblemChangesProcessing() {
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
    public synchronized boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        // Destroying a thread pool with solver threads will only cause it to interrupt those solver threads,
        // it won't call Solver.terminateEarly()
        if (Thread.currentThread().isInterrupted() // Does not clear the interrupted flag
                // Avoid duplicate log message because this method is called twice:
                // - in the phase step loop (every phase termination bridges to the solver termination)
                // - in the solver's phase loop
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
