package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveTestContext;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodTestContext;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class DefaultNeighborhoodTestContext<Solution_>
        implements NeighborhoodTestContext<Solution_> {

    private final NeighborhoodsBasedMoveRepository<Solution_> moveRepository;
    private final MoveTestContext<Solution_> moveTestContext;
    private final LocalSearchPhaseScope<Solution_> phaseScope;

    DefaultNeighborhoodTestContext(NeighborhoodsBasedMoveRepository<Solution_> moveRepository,
            MoveTestContext<Solution_> moveTestContext,
            LocalSearchPhaseScope<Solution_> phaseScope) {
        this.moveRepository = Objects.requireNonNull(moveRepository, "moveRepository");
        this.moveTestContext = Objects.requireNonNull(moveTestContext, "moveTestContext");
        this.phaseScope = Objects.requireNonNull(phaseScope, "phaseScope");
    }

    @Override
    public <Move_ extends Move<Solution_>> Iterator<Move_> getMovesAsIterator(Function<Move<Solution_>, Move_> moveCaster) {
        var stepScope = new LocalSearchStepScope<>(phaseScope);
        moveRepository.stepStarted(stepScope);
        var iterator = new CastingIterator<>(moveRepository.iterator(), Objects.requireNonNull(moveCaster, "moveCaster"));
        moveRepository.stepEnded(stepScope);
        return iterator;
    }

    @Override
    public MoveTestContext<Solution_> getMoveTestContext() {
        return moveTestContext;
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
