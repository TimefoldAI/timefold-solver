package ai.timefold.solver.core.impl.solver.termination;

import java.util.Objects;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class SolverBridgePhaseTermination<Solution_>
        extends AbstractPhaseTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private final SolverTermination<Solution_> solverTermination;

    public SolverBridgePhaseTermination(SolverTermination<Solution_> solverTermination) {
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
        if (childThreadType == ChildThreadType.PART_THREAD) {
            // This strips the bridge, but the partitioned phase factory will add it back.
            return ChildThreadSupportingTermination.assertChildThreadSupport(solverTermination)
                    .createChildThreadTermination(scope, childThreadType);
        } else {
            throw new UnsupportedOperationException("The childThreadType (%s) is not implemented."
                    .formatted(childThreadType));
        }
    }

    @Override
    public String toString() {
        return "Bridge(" + solverTermination + ")";
    }
}
