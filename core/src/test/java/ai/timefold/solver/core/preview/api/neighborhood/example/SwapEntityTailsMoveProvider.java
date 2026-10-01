package ai.timefold.solver.core.preview.api.neighborhood.example;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * For a randomly picked pair of distinct entities and a randomly picked cut index valid for both,
 * swaps every value from that index to the end of the list between the two entities,
 * as a single {@link SwapEntityTailsMove}.
 */
@NullMarked
final class SwapEntityTailsMoveProvider implements MoveProvider<TestdataListSolution> {

    private final PlanningListVariableMetaModel<TestdataListSolution, TestdataListEntity, TestdataListValue> variableMetaModel;

    SwapEntityTailsMoveProvider(
            PlanningListVariableMetaModel<TestdataListSolution, TestdataListEntity, TestdataListValue> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<TestdataListSolution> build(MoveStreamFactory<TestdataListSolution> factory) {
        // Just-in-time: many entity pairs, few ever drawn.
        // Ordered, so no mirrored duplicate.
        // Filtered to pairs with a shared tail at all, so draw() only ever rejects the cut index.
        var entityPairs = factory.forEach(TestdataListEntity.class, false)
                .asCachedDataset()
                .join(factory.forEach(TestdataListEntity.class, false),
                        NeighborhoodsJoiners.lessThan(TestdataListEntity::getCode),
                        NeighborhoodsJoiners.filtering(this::bothHaveValues));

        // Cached, entity on the left:
        // queried per-A for its own destinations on every candidate pair,
        // and the exact per-A size is what bounds the cut.
        var entityDestinations = factory.forEach(TestdataListEntity.class, false)
                .join(factory.forEachDestination(variableMetaModel),
                        NeighborhoodsJoiners.equal(entity -> entity, PositionInList::entity))
                .asCachedDataset();

        return factory.buildMoveStream((session, random) -> {
            var pairInstance = session.getInstance(entityPairs);
            if (pairInstance.size() == 0) {
                return Collections.emptyIterator(); // Fewer than 2 entities.
            }
            var destinationInstance = session.getInstance(entityDestinations);
            var solutionView = session.getSolutionView();
            var pairIterator = pairInstance.iterator(random); // Draws with replacement; never ends.
            return new Iterator<>() {

                private @Nullable Move<TestdataListSolution> nextMove;

                @Override
                public boolean hasNext() {
                    while (nextMove == null && pairIterator.hasNext()) {
                        nextMove = draw();
                    }
                    return nextMove != null;
                }

                @Override
                public Move<TestdataListSolution> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    var move = Objects.requireNonNull(nextMove);
                    nextMove = null;
                    return move;
                }

                /**
                 * @return null if the drawn combination is not a valid move; the caller draws again
                 */
                private @Nullable Move<TestdataListSolution> draw() {
                    pairIterator.next();
                    var entityA = Objects.requireNonNull(pairIterator.a());
                    var entityB = Objects.requireNonNull(pairIterator.b());
                    var overlap = Math.min(solutionView.countValues(variableMetaModel, entityA),
                            solutionView.countValues(variableMetaModel, entityB));
                    var destinationIterator = destinationInstance.iterator(entityA, random);
                    if (!destinationIterator.hasNext()) {
                        return null; // No destination for entityA (should not happen once filtered non-empty).
                    }
                    var cutIndex = Objects.requireNonNull(destinationIterator.next()).index();
                    // A cut at or past the shared length leaves no shared tail to swap.
                    return cutIndex < overlap
                            ? new SwapEntityTailsMove(variableMetaModel, entityA, entityB, cutIndex)
                            : null;
                }
            };
        });
    }

    private boolean bothHaveValues(SolutionView<TestdataListSolution> solutionView, TestdataListEntity entityA,
            TestdataListEntity entityB) {
        return solutionView.countValues(variableMetaModel, entityA) > 0
                && solutionView.countValues(variableMetaModel, entityB) > 0;
    }

}
