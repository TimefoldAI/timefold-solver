package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

abstract sealed class AbstractSolverTermination<Solution_>
        extends AbstractTermination<Solution_>
        implements SolverTermination<Solution_>
        permits AbstractCompositeTermination, BasicPlumbingTermination, BestScoreFeasibleTermination, BestScoreTermination,
        ChildThreadPlumbingTermination, MoveCountTermination, ScoreCalculationCountTermination, TimeMillisSpentTermination,
        UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination, UnimprovedTimeMillisSpentTermination {

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {

    }

    protected static <Solution_> void solvingStarted(Termination<Solution_> termination, SolverScope<Solution_> scope) {
        if (termination instanceof SolverTermination<Solution_> solverTermination) {
            solverTermination.solvingStarted(scope);
        }
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {

    }

    protected static <Solution_> void solvingEnded(Termination<Solution_> termination, SolverScope<Solution_> scope) {
        if (termination instanceof SolverTermination<Solution_> solverTermination) {
            solverTermination.solvingEnded(scope);
        }
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {

    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {

    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {

    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {

    }

}
