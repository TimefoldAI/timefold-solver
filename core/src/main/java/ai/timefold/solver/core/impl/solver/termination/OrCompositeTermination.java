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

    public OrCompositeTermination(List<Termination<Solution_>> terminationList) {
        super(terminationList);
    }

    @SafeVarargs
    public OrCompositeTermination(Termination<Solution_>... terminations) {
        super(terminations);
    }

    /**
     * @param solverScope never null
     * @return true if any of the Termination is terminated.
     */
    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        for (var termination : terminationList) {
            if (termination instanceof SolverTermination<Solution_> solverTermination
                    && solverTermination.isSolverTerminated(solverScope)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param phaseScope never null
     * @return true if any of the Termination is terminated.
     */
    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        for (var termination : terminationList) {
            if (termination instanceof PhaseTermination<Solution_> phaseTermination
                    && phaseTermination.isPhaseTerminated(phaseScope)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the maximum timeGradient of all Terminations.
     * Not supported timeGradients (-1.0) are ignored.
     *
     * @param solverScope never null
     * @return the maximum timeGradient of the Terminations.
     */
    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        var timeGradient = 0.0;
        for (var termination : terminationList) {
            if (!(termination instanceof SolverTermination<Solution_> solverTermination)) {
                continue;
            }
            var nextTimeGradient = solverTermination.calculateSolverTimeGradient(solverScope);
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
     * @param phaseScope never null
     * @return the maximum timeGradient of the Terminations.
     */
    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        var timeGradient = 0.0;
        for (var termination : terminationList) {
            if (!(termination instanceof PhaseTermination<Solution_> phaseTermination)) {
                continue;
            }
            var nextTimeGradient = phaseTermination.calculatePhaseTimeGradient(phaseScope);
            if (nextTimeGradient >= 0.0) {
                timeGradient = Math.max(timeGradient, nextTimeGradient);
            }
        }
        return timeGradient;
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new OrCompositeTermination<>(createChildThreadTerminationList(solverScope, childThreadType));
    }

    @Override
    public String toString() {
        return "Or(" + terminationList + ")";
    }

}
