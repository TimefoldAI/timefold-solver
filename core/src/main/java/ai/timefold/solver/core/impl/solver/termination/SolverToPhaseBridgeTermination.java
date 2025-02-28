package ai.timefold.solver.core.impl.solver.termination;

import java.util.Objects;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class SolverToPhaseBridgeTermination<Solution_>
        extends AbstractPhaseTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private final SolverTermination<Solution_> solverTermination;

    public SolverToPhaseBridgeTermination(SolverTermination<Solution_> solverTermination) {
        this.solverTermination = Objects.requireNonNull(solverTermination);
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return solverTermination.isSolverTerminated(phaseScope.getSolverScope());
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return solverTermination.calculateSolverTimeGradient(phaseScope.getSolverScope());
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> scope, ChildThreadType childThreadType) {
        return ChildThreadSupportingTermination.assertChildThreadSupport(solverTermination)
                .createChildThreadTermination(scope, childThreadType);
    }

    @Override
    public String toString() {
        return solverTermination.toString();
    }
}
