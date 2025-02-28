package ai.timefold.solver.core.impl.solver.termination;

import java.util.Objects;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class PhaseToUniversalTerminationBridge<Solution_>
        extends AbstractUniversalTermination<Solution_> {

    private final PhaseTermination<Solution_> phaseTermination;

    public PhaseToUniversalTerminationBridge(PhaseTermination<Solution_> phaseTermination) {
        this.phaseTermination = Objects.requireNonNull(phaseTermination);
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return phaseTermination.isPhaseTerminated(phaseScope);
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return phaseTermination.calculatePhaseTimeGradient(phaseScope);
    }

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        return false;
    }

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return -1.0;
    }

    @Override
    public String toString() {
        return phaseTermination.toString();
    }
}
