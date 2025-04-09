package ai.timefold.solver.core.impl.solver.termination;

import java.util.Objects;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

/**
 * Delegates phase calls (such as {@link #isPhaseTerminated(AbstractPhaseScope)})
 * to solver calls (such as {@link SolverTermination#isSolverTerminated(SolverScope)})
 * on the bridged termination.
 * <p>
 * Had this not happened,
 * the solver-level termination running at phase-level
 * would call {@link #isPhaseTerminated(AbstractPhaseScope)}
 * instead of {@link SolverTermination#isSolverTerminated(SolverScope)},
 * and would therefore use the phase start timestamp as its reference,
 * and not the solver start timestamp.
 * The effect of this in practice would have been that,
 * if a solver-level {@link TimeMillisSpentTermination} were configured to terminate after 10 seconds,
 * each phase would effectively start a new 10-second counter.
 * 
 * @param <Solution_>
 */
@NullMarked
final class SolverBridgePhaseTermination<Solution_>
        extends AbstractPhaseTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    final SolverTermination<Solution_> solverTermination;

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
