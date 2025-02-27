package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class PhaseToSolverTerminationBridge<Solution_>
        extends AbstractTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private final SolverTermination<Solution_> solverTermination;

    public PhaseToSolverTerminationBridge(SolverTermination<Solution_> solverTermination) {
        this.solverTermination = solverTermination;
    }

    // Do not propagate any of the lifecycle events up to the solverTermination,
    // because it already gets the solver events from the DefaultSolver
    // and the phase/step events - if ever needed - should also come through the DefaultSolver

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return solverTermination.isSolverTerminated(phaseScope.getSolverScope());
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return solverTermination.calculateSolverTimeGradient(phaseScope.getSolverScope());
    }

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        if (childThreadType == ChildThreadType.PART_THREAD) {
            // Remove of the bridge (which is nested if there's a phase termination), PhaseConfig will add it again
            var childThreadSupportingTermination = ChildThreadSupportingTermination.assertChildThreadSupport(solverTermination);
            return childThreadSupportingTermination.createChildThreadTermination(solverScope, childThreadType);
        } else {
            throw new IllegalStateException("The childThreadType (" + childThreadType + ") is not implemented.");
        }
    }

    @Override
    public String toString() {
        return "Bridge(" + solverTermination + ")";
    }

}
