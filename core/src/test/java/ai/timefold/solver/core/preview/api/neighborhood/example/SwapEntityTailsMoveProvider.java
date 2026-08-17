package ai.timefold.solver.core.preview.api.neighborhood.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.jspecify.annotations.NullMarked;

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
        var entityPairs = factory.forEach(TestdataListEntity.class, false)
                .asCachedDataset()
                .join(factory.forEach(TestdataListEntity.class, false),
                        NeighborhoodsJoiners.lessThan(TestdataListEntity::getCode));

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

            var moveList = new ArrayList<Move<TestdataListSolution>>();
            var pairIterator = pairInstance.iterator();
            while (pairIterator.hasNext()) {
                pairIterator.next();
                var entityA = pairIterator.a();
                var entityB = pairIterator.b();
                var overlap = Math.min(solutionView.countValues(variableMetaModel, entityA),
                        solutionView.countValues(variableMetaModel, entityB));

                var cutIterator = destinationInstance.iterator(entityA);
                while (cutIterator.hasNext()) {
                    var cutIndex = cutIterator.next().index();
                    if (cutIndex < overlap) {
                        moveList.add(new SwapEntityTailsMove(variableMetaModel, entityA, entityB, cutIndex));
                    }
                    // Else: the cut leaves no shared tail to swap.
                    // Nothing to add for this cut.
                }
            }
            return moveList.iterator();
        });
    }

}
