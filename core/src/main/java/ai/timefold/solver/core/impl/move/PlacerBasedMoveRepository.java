package ai.timefold.solver.core.impl.move;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.placer.Placement;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedValuePlacer;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PlacerBasedMoveRepository<Solution_>
        implements MoveRepository<Solution_> {

    private final EntityPlacer<Solution_> placer;
    private @Nullable Iterator<Placement<Solution_>> placementIterator;

    public PlacerBasedMoveRepository(EntityPlacer<Solution_> placer) {
        this.placer = Objects.requireNonNull(placer);
    }

    public EntityPlacer<Solution_> getPlacer() {
        return placer;
    }

    @Override
    public boolean isNeverEnding() {
        return false;
    }

    @Override
    public void initialize(Solution_ workingSolution, SupplyManager supplyManager) {
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
        return Objects.requireNonNull(placementIterator).next().iterator();
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

}
