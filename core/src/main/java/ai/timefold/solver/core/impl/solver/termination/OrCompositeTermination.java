package ai.timefold.solver.core.impl.solver.termination;

import java.util.List;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class OrCompositeTermination<Solution_>
        extends AbstractCompositeTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    /**
     * If true, the id of the first child that is true goes to {@link SolverScope#setLastTerminationId}.
     */
    private boolean registering = true;

    /**
     * If true, a false result clears {@link SolverScope#setLastTerminationId},
     * because a termination that was true in the middle of a step did not end the phase.
     */
    private boolean phaseRoot = false;

    public OrCompositeTermination(List<Termination<Solution_>> terminationList) {
        super(terminationList);
    }

    @SafeVarargs
    public OrCompositeTermination(Termination<Solution_>... terminations) {
        super(terminations);
    }

    /**
     * Called by a built {@link AndCompositeTermination} above this one,
     * because its children can be true while it is false.
     */
    void stopRecording() {
        registering = false;
    }

    void markPhaseRoot() {
        phaseRoot = true;
    }

    private void recordTermination(SolverScope<Solution_> solverScope, Termination<Solution_> termination) {
        if (!registering) {
            return;
        }
        var terminationId = termination.getTerminationId();
        if (terminationId != null) {
            solverScope.setLastTerminationId(terminationId);
        }
    }

    /**
     * @param solverScope never null
     * @return true if any one of the terminations is terminated.
     */
    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        for (var termination : solverTerminationList) {
            if (termination.isSolverTerminated(solverScope)) {
                recordTermination(solverScope, termination);
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if any one of the supported terminations is terminated.
     */
    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        for (var termination : phaseTerminationList) {
            if (!termination.isApplicableTo(phaseScope.getClass())) {
                continue;
            }
            if (termination.isPhaseTerminated(phaseScope)) {
                recordTermination(phaseScope.getSolverScope(), termination);
                return true;
            }
        }
        if (phaseRoot) {
            phaseScope.getSolverScope().setLastTerminationId(null);
        }
        return false;
    }

    /**
     * Calculates the maximum timeGradient of all Terminations.
     * Not supported timeGradients (-1.0) are ignored.
     *
     * @return the maximum timeGradient of the terminations.
     */
    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        var timeGradient = 0.0;
        for (var termination : solverTerminationList) {
            var nextTimeGradient = termination.calculateSolverTimeGradient(solverScope);
            if (nextTimeGradient >= 0.0) {
                timeGradient = Math.max(timeGradient, nextTimeGradient);
            }
        }
        return timeGradient;
    }

    /**
     * Calculates the maximum timeGradient of all Terminations.
     * Not supported timeGradients (-1.0) are ignored.
     *
     * @return the maximum timeGradient of the supported terminations.
     */
    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        var timeGradient = 0.0;
        for (var termination : phaseTerminationList) {
            if (!termination.isApplicableTo(phaseScope.getClass())) {
                continue;
            }
            var nextTimeGradient = termination.calculatePhaseTimeGradient(phaseScope);
            if (nextTimeGradient >= 0.0) {
                timeGradient = Math.max(timeGradient, nextTimeGradient);
            }
        }
        return timeGradient;
    }

    @Override
    public OrCompositeTermination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        // The copy has default flags; it records into the child thread's scope, which the run summary does not read.
        return new OrCompositeTermination<>(createChildThreadTerminationList(solverScope, childThreadType));
    }

    @Override
    public String toString() {
        return "Or(" + terminationList + ")";
    }

}
