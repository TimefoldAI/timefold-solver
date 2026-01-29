package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveRunContext;
import ai.timefold.solver.core.preview.api.neighborhood.EvaluatedNeighborhood;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class DefaultEvaluatedNeighborhood<Solution_>
        implements EvaluatedNeighborhood<Solution_> {

    private final NeighborhoodsBasedMoveRepository<Solution_> moveRepository;
    private final MoveRunContext<Solution_> moveRunContext;
    private final LocalSearchPhaseScope<Solution_> phaseScope;

    DefaultEvaluatedNeighborhood(NeighborhoodsBasedMoveRepository<Solution_> moveRepository,
            MoveRunContext<Solution_> moveRunContext,
            LocalSearchPhaseScope<Solution_> phaseScope) {
        this.moveRepository = Objects.requireNonNull(moveRepository, "moveRepository");
        this.moveRunContext = Objects.requireNonNull(moveRunContext, "moveRunContext");
        this.phaseScope = Objects.requireNonNull(phaseScope, "phaseScope");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Move_ extends Move<Solution_>> Iterator<Move_> getMoveIterator(Class<Move_> moveClass) {
        var stepScope = new LocalSearchStepScope<>(phaseScope);
        moveRepository.stepStarted(stepScope);
        var iterator = (Iterator<Move_>) moveRepository.iterator();
        moveRepository.stepEnded(stepScope);
        return iterator;
    }

    @Override
    public MoveRunContext<Solution_> getMoveRunContext() {
        return moveRunContext;
    }

}
