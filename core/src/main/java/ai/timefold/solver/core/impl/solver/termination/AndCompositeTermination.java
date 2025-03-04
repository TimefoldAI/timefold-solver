package ai.timefold.solver.core.impl.solver.termination;

import java.util.List;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class AndCompositeTermination<Solution_>
        extends AbstractCompositeTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    public AndCompositeTermination(List<Termination<Solution_>> terminationList) {
        super(terminationList);
    }

    @SafeVarargs
    public AndCompositeTermination(Termination<Solution_>... terminations) {
        super(terminations);
    }

    /**
     * @return true if all terminations are terminated.
     */
    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        for (var termination : solverTerminationList) {
            if (!termination.isSolverTerminated(solverScope)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if all supported terminations are terminated.
     */
    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        for (var termination : phaseTerminationList) {
            if (termination.isApplicableTo(phaseScope.getClass()) && !termination.isPhaseTerminated(phaseScope)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the minimum timeGradient of all Terminations.
     * Not supported timeGradients (-1.0) are ignored.
     *
     * @return the minimum timeGradient of the Terminations.
     */
    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        var timeGradient = 1.0;
        for (var termination : solverTerminationList) {
            var nextTimeGradient = termination.calculateSolverTimeGradient(solverScope);
            if (nextTimeGradient >= 0.0) {
                timeGradient = Math.min(timeGradient, nextTimeGradient);
            }
        }
        return timeGradient;
    }

    /**
     * Calculates the minimum timeGradient of all Terminations.
     * Not supported timeGradients (-1.0) are ignored.
     *
     * @return the minimum timeGradient of the Terminations.
     */
    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        var timeGradient = 1.0;
        for (var termination : phaseTerminationList) {
            if (!termination.isApplicableTo(phaseScope.getClass())) {
                continue;
            }
            var nextTimeGradient = termination.calculatePhaseTimeGradient(phaseScope);
            if (nextTimeGradient >= 0.0) {
                timeGradient = Math.min(timeGradient, nextTimeGradient);
            }
        }
        return timeGradient;
    }

    @Override
    public AndCompositeTermination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new AndCompositeTermination<>(createChildThreadTerminationList(solverScope, childThreadType));
    }

    @Override
    public String toString() {
        return "And(" + terminationList + ")";
    }

}
