package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;

@NullMarked
abstract sealed class AbstractUniversalTermination<Solution_>
        extends AbstractTermination<Solution_>
        implements UniversalTermination<Solution_>
        permits AbstractCompositeTermination, BasicPlumbingTermination, BestScoreFeasibleTermination, BestScoreTermination,
        ChildThreadPlumbingTermination, MoveCountTermination, ScoreCalculationCountTermination, TimeMillisSpentTermination,
        UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination, UnimprovedTimeMillisSpentTermination {

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Override if needed.
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Override if needed.
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // Override if needed.
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // Override if needed.
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        // Override if needed.
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Override if needed.
    }

}
