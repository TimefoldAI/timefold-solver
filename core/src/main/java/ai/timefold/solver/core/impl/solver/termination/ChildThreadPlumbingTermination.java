package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ChildThreadPlumbingTermination<Solution_>
        extends AbstractSolverTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private boolean terminateChildren = false;

    /**
     * This method is thread-safe.
     *
     * @return true if termination hasn't been requested previously
     */
    public synchronized boolean terminateChildren() {
        var terminationEarlySuccessful = !terminateChildren;
        terminateChildren = true;
        return terminationEarlySuccessful;
    }

    @Override
    public synchronized boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        // Destroying a thread pool with solver threads will only cause it to interrupt those child solver threads
        if (Thread.currentThread().isInterrupted()) { // Does not clear the interrupted flag
            logger.info("A child solver thread got interrupted, so these child solvers are terminating early.");
            terminateChildren = true;
        }
        return terminateChildren;
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        throw new IllegalStateException(ChildThreadPlumbingTermination.class.getSimpleName()
                + " configured only as solver termination."
                + " It is always bridged to phase termination.");
    }

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return -1.0; // Not supported
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        throw new IllegalStateException(ChildThreadPlumbingTermination.class.getSimpleName()
                + " configured only as solver termination."
                + " It is always bridged to phase termination.");
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return this;
    }

    @Override
    public String toString() {
        return "ChildThreadPlumbing()";
    }

}
