package ai.timefold.solver.core.impl.solver.termination;

import java.util.Objects;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class SolverToUniversalBridgeTermination<Solution_>
        extends AbstractUniversalTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private final SolverTermination<Solution_> solverTermination;

    public SolverToUniversalBridgeTermination(SolverTermination<Solution_> solverTermination) {
        this.solverTermination = Objects.requireNonNull(solverTermination);
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return false;
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return -1.0;
    }

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        return solverTermination.isSolverTerminated(solverScope);
    }

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return solverTermination.calculateSolverTimeGradient(solverScope);
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> scope, ChildThreadType childThreadType) {
        return ChildThreadSupportingTermination.assertChildThreadSupport(solverTermination)
                .createChildThreadTermination(scope, childThreadType);
    }

    @Override
    public String toString() {
        return "Bridge(" + solverTermination + ")";
    }
}
