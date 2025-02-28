package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;

@NullMarked
abstract sealed class AbstractUniversalTermination<Solution_>
        extends AbstractTermination<Solution_>
        implements UniversalTermination<Solution_>
        permits AbstractCompositeTermination, BestScoreFeasibleTermination, BestScoreTermination, MoveCountTermination,
        ScoreCalculationCountTermination, SolverToUniversalBridgeTermination, TimeMillisSpentTermination,
        UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination, UnimprovedTimeMillisSpentTermination {

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Override if needed.
    }

    protected static <Solution_> void solvingStarted(Termination<Solution_> termination, SolverScope<Solution_> scope) {
        if (termination instanceof SolverTermination<Solution_> solverTermination) {
            solverTermination.solvingStarted(scope);
        }
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Override if needed.
    }

    protected static <Solution_> void solvingEnded(Termination<Solution_> termination, SolverScope<Solution_> scope) {
        if (termination instanceof SolverTermination<Solution_> solverTermination) {
            solverTermination.solvingEnded(scope);
        }
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // Override if needed.
    }

    protected static <Solution_> void phaseStarted(Termination<Solution_> termination, AbstractPhaseScope<Solution_> scope) {
        if (termination instanceof PhaseTermination<Solution_> phaseTermination) {
            phaseTermination.phaseStarted(scope);
        }
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // Override if needed.
    }

    protected static <Solution_> void stepStarted(Termination<Solution_> termination, AbstractStepScope<Solution_> scope) {
        if (termination instanceof PhaseTermination<Solution_> phaseTermination) {
            phaseTermination.stepStarted(scope);
        }
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        // Override if needed.
    }

    protected static <Solution_> void stepEnded(Termination<Solution_> termination, AbstractStepScope<Solution_> scope) {
        if (termination instanceof PhaseTermination<Solution_> phaseTermination) {
            phaseTermination.stepEnded(scope);
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Override if needed.
    }

    protected static <Solution_> void phaseEnded(Termination<Solution_> termination, AbstractPhaseScope<Solution_> scope) {
        if (termination instanceof PhaseTermination<Solution_> phaseTermination) {
            phaseTermination.phaseEnded(scope);
        }
    }

}
