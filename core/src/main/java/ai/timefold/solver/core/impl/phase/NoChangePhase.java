package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

/**
 * A {@link NoChangePhase} is a {@link Phase} which does nothing.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @deprecated Deprecated on account of having no use.
 */
@Deprecated(forRemoval = true, since = "1.20.0")
public class NoChangePhase<Solution_> extends AbstractPhase<Solution_> {

    private NoChangePhase(Builder<Solution_> builder) {
        super(builder);
    }

    @Override
    public String getPhaseTypeString() {
        return "No Change";
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        logger.info("{}No Change phase ({}) ended.",
                logIndentation,
                phaseIndex);
    }

    public static class Builder<Solution_> extends AbstractPhaseBuilder<Solution_> {

        public Builder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination) {
            super(phaseIndex, logIndentation, phaseTermination);
        }

        @Override
        public NoChangePhase<Solution_> build() {
            return new NoChangePhase<>(this);
        }
    }
}
