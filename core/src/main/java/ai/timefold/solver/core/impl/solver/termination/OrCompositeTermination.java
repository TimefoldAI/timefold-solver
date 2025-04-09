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
     * @return true if any one of the terminations is terminated.
     */
    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        for (var termination : solverTerminationList) {
            if (termination.isSolverTerminated(solverScope)) {
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
                return true;
            }
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
        return new OrCompositeTermination<>(createChildThreadTerminationList(solverScope, childThreadType));
    }

    @Override
    public String toString() {
        return "Or(" + terminationList + ")";
    }

}
