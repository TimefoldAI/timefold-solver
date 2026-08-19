package ai.timefold.solver.core.impl.neighborhood;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.random.RandomSource;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.test.MoveTestContext;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodMoveAsserter;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTestContext;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class DefaultNeighborhoodTestContext<Solution_>
        implements NeighborhoodTestContext<Solution_> {

    private static final int DEFAULT_ITERATION_LIMIT = 1_000;

    private final NeighborhoodsBasedMoveRepository<Solution_> moveRepository;
    private final MoveTestContext<Solution_> moveTestContext;
    private final LocalSearchPhaseScope<Solution_> phaseScope;
    private final int iterationLimit;

    DefaultNeighborhoodTestContext(NeighborhoodsBasedMoveRepository<Solution_> moveRepository,
            MoveTestContext<Solution_> moveTestContext,
            LocalSearchPhaseScope<Solution_> phaseScope) {
        this(moveRepository, moveTestContext, phaseScope, DEFAULT_ITERATION_LIMIT);
    }

    private DefaultNeighborhoodTestContext(NeighborhoodsBasedMoveRepository<Solution_> moveRepository,
            MoveTestContext<Solution_> moveTestContext,
            LocalSearchPhaseScope<Solution_> phaseScope, int iterationLimit) {
        this.moveRepository = Objects.requireNonNull(moveRepository, "moveRepository");
        this.moveTestContext = Objects.requireNonNull(moveTestContext, "moveTestContext");
        this.phaseScope = Objects.requireNonNull(phaseScope, "phaseScope");
        this.iterationLimit = iterationLimit;
    }

    @Override
    public NeighborhoodMoveAsserter<Solution_> within(int iterationLimit) {
        return new DefaultNeighborhoodTestContext<>(moveRepository, moveTestContext, phaseScope, iterationLimit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void producesAllOf(Move<Solution_>... expectedMoves) {
        var unseenMoves = new HashSet<>(Arrays.asList(expectedMoves));
        draw(move -> {
            unseenMoves.remove(move);
            return unseenMoves.isEmpty();
        });
        if (!unseenMoves.isEmpty()) {
            throw new AssertionError(
                    "Within %d iterations, the neighborhood never produced the expected move(s) (%s)."
                            .formatted(iterationLimit, unseenMoves));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void producesNoneOf(Move<Solution_>... unexpectedMoves) {
        var forbiddenMoves = new HashSet<>(Arrays.asList(unexpectedMoves));
        var seenMove = new AtomicReference<Move<Solution_>>();
        var found = draw(move -> {
            if (forbiddenMoves.contains(move)) {
                seenMove.set(move);
                return true;
            }
            return false;
        });
        if (found) {
            throw new AssertionError(
                    "The neighborhood unexpectedly produced the move (%s), which was expected never to be produced."
                            .formatted(seenMove.get()));
        }
    }

    /**
     * Repeatedly draws moves from a fresh, seeded
     * {@link NeighborhoodsBasedMoveRepository#iterator(java.util.random.RandomGenerator)}
     * (restarting whenever it exhausts) until either {@code stopCondition} returns true, the iteration limit is
     * reached, or an entire pass draws nothing.
     *
     * @return whether {@code stopCondition} was met
     */
    private boolean draw(Predicate<Move<Solution_>> stopCondition) {
        var draws = 0;
        while (draws < iterationLimit) {
            var stepScope = new LocalSearchStepScope<>(phaseScope);
            moveRepository.stepStarted(stepScope);
            var iterator = moveRepository.iterator(RandomSource.seeded(0L).moveIteratorUsage());
            var drewAnythingThisPass = false;
            while (iterator.hasNext() && draws < iterationLimit) {
                var move = iterator.next();
                draws++;
                drewAnythingThisPass = true;
                if (stopCondition.test(move)) {
                    moveRepository.stepEnded(stepScope);
                    return true;
                }
            }
            moveRepository.stepEnded(stepScope);
            if (!drewAnythingThisPass) {
                return false;
            }
        }
        return false;
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
