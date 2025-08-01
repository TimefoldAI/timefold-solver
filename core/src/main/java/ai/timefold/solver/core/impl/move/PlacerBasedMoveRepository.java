package ai.timefold.solver.core.impl.move;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.placer.Placement;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedValuePlacer;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PlacerBasedMoveRepository<Solution_>
        implements MoveRepository<Solution_> {

    private final EntityPlacer<Solution_> placer;
    private @Nullable Iterator<Placement<Solution_>> placementIterator;
    private @Nullable EnvironmentMode environmentMode = null;

    public PlacerBasedMoveRepository(EntityPlacer<Solution_> placer) {
        this.placer = Objects.requireNonNull(placer);
    }

    public EntityPlacer<Solution_> getPlacer() {
        return placer;
    }

    @Override
    public void enableAssertions(EnvironmentMode environmentMode) {
        // We only store the environment mode and use it later when generating moves
        this.environmentMode = environmentMode;
    }

    @Override
    public boolean isNeverEnding() {
        return false;
    }

    @Override
    public void initialize(SessionContext<Solution_> context) {
        placementIterator = placer.iterator();
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        placer.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        placer.phaseStarted(phaseScope);
        phaseScope.getScoreDirector().setMoveRepository(this);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        placer.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        placer.stepEnded(stepScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        phaseScope.getScoreDirector().setMoveRepository(null);
        placer.phaseEnded(phaseScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        placer.solvingEnded(solverScope);
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        var iterator = Objects.requireNonNull(placementIterator).next().iterator();
        return new PlacementAssertionIterator(iterator, environmentMode);
    }

    public boolean hasNext() {
        return Objects.requireNonNull(placementIterator).hasNext();
    }

    public boolean hasListVariable() {
        // When the placer depends on a list variable, the CH phase creates a ListChangeMoveSelector.
        // However, in certain cases, such as ALLOCATE_TO_VALUE_FROM_QUEUE,
        // a QueuedValuePlacer can be created for a basic planning variable,
        // and in these cases, the move selector does not rely on a list variable.
        return placer instanceof QueuedValuePlacer<Solution_> queuedValuePlacer
                && queuedValuePlacer.hasListChangeMoveSelector();
    }

    @NullMarked
    private class PlacementAssertionIterator implements Iterator<Move<Solution_>> {

        private final Iterator<Move<Solution_>> childIterator;
        private final EnvironmentMode environmentMode;

        private PlacementAssertionIterator(Iterator<Move<Solution_>> childIterator, EnvironmentMode environmentMode) {
            this.childIterator = Objects.requireNonNull(childIterator);
            this.environmentMode = Objects.requireNonNull(environmentMode);
        }

        @Override
        public boolean hasNext() {
            return childIterator.hasNext();
        }

        @Override
        public Move<Solution_> next() {
            var move = childIterator.next();
            move.enableAssertions(environmentMode);
            return move;
        }
    }

}
