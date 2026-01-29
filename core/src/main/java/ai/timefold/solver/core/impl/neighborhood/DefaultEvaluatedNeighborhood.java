package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

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

    @Override
    public <Move_ extends Move<Solution_>> Iterator<Move_> getMoveIterator(Function<Move<Solution_>, Move_> moveCaster) {
        var stepScope = new LocalSearchStepScope<>(phaseScope);
        moveRepository.stepStarted(stepScope);
        var iterator = new CastingIterator<>(moveRepository.iterator(), moveCaster);
        moveRepository.stepEnded(stepScope);
        return iterator;
    }

    @Override
    public MoveRunContext<Solution_> getMoveRunContext() {
        return moveRunContext;
    }

    private record CastingIterator<Solution_, Move_ extends Move<Solution_>>(Iterator<Move<Solution_>> childIterator,
            Function<Move<Solution_>, Move_> moveCaster)
            implements
                Iterator<Move_> {

        @Override
        public boolean hasNext() {
            return childIterator.hasNext();
        }

        @Override
        public Move_ next() {
            return moveCaster.apply(childIterator.next());
        }

    }

}
