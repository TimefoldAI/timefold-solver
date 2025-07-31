package ai.timefold.solver.core.impl.move;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class MoveSelectorBasedMoveRepository<Solution_>
        implements MoveRepository<Solution_>, PhaseLifecycleListener<Solution_> {

    private final MoveSelector<Solution_> moveSelector;
    private @Nullable EnvironmentMode environmentMode;

    public MoveSelectorBasedMoveRepository(MoveSelector<Solution_> moveSelector) {
        this.moveSelector = Objects.requireNonNull(moveSelector);
    }

    @Override
    public void enableAssertions(EnvironmentMode environmentMode) {
        // We only store the environment mode and use it later when generating moves
        this.environmentMode = Objects.requireNonNull(environmentMode);
    }

    @Override
    public boolean isNeverEnding() {
        return moveSelector.isNeverEnding();
    }

    @Override
    public void initialize(SessionContext<Solution_> context) {
        // No need to do anything.
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        moveSelector.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        moveSelector.phaseStarted(phaseScope);
        phaseScope.getScoreDirector().setMoveRepository(this);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        moveSelector.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        moveSelector.stepEnded(stepScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        phaseScope.getScoreDirector().setMoveRepository(null);
        moveSelector.phaseEnded(phaseScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        moveSelector.solvingEnded(solverScope);
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new Iterator<>() {

            private final Iterator<ai.timefold.solver.core.impl.heuristic.move.Move<Solution_>> delegate =
                    moveSelector.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Move<Solution_> next() {
                var move = delegate.next();
                move.enableAssertions(environmentMode);
                return new LegacyMoveAdapter<>(move);
            }
        };
    }

}
