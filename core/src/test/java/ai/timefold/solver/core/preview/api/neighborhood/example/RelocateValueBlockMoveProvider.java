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
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * For a randomly picked ordered pair of distinct entities
 * and a randomly picked run of consecutive values in one entity's list,
 * relocates that run into the other entity's list at a randomly picked destination,
 * as a single {@link RelocateValueBlockMove}.
 */
@NullMarked
final class RelocateValueBlockMoveProvider implements MoveProvider<TestdataListEntityProvidingSolution> {

    private final PlanningListVariableMetaModel<TestdataListEntityProvidingSolution, TestdataListEntityProvidingEntity, TestdataListEntityProvidingValue> variableMetaModel;

    RelocateValueBlockMoveProvider(
            PlanningListVariableMetaModel<TestdataListEntityProvidingSolution, TestdataListEntityProvidingEntity, TestdataListEntityProvidingValue> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<TestdataListEntityProvidingSolution> build(
            MoveStreamFactory<TestdataListEntityProvidingSolution> factory) {

        // Just-in-time: many entity pairs, few ever drawn.
        // Ordered, so no mirrored duplicate.
        var entityPairs =
                factory.forEach(TestdataListEntityProvidingEntity.class, false).asCachedDataset()
                        .join(factory.forEach(TestdataListEntityProvidingEntity.class, false),
                                NeighborhoodsJoiners.lessThan(TestdataListEntityProvidingEntity::getCode));

        // Cached, entity on the left:
        // queried per-A both for the relocation destination and for the source block's start index.
        var entityDestinations = factory.forEach(TestdataListEntityProvidingEntity.class, false)
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

                private @Nullable Move<TestdataListEntityProvidingSolution> nextMove;

                @Override
                public boolean hasNext() {
                    while (nextMove == null && pairIterator.hasNext()) {
                        nextMove = draw();
                    }
                    return nextMove != null;
                }

                @Override
                public Move<TestdataListEntityProvidingSolution> next() {
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
                private @Nullable Move<TestdataListEntityProvidingSolution> draw() {
                    pairIterator.next();
                    var entityA = Objects.requireNonNull(pairIterator.a());
                    var entityB = Objects.requireNonNull(pairIterator.b());
                    // The drawn destination's entity picks the direction: the pair's other entity is the source.
                    var intoA = random.nextBoolean();
                    var destinationEntity = intoA ? entityA : entityB;
                    var sourceEntity = intoA ? entityB : entityA;
                    var destinationIterator = destinationInstance.iterator(destinationEntity, random);
                    if (!destinationIterator.hasNext()) {
                        return null; // No destination for destinationEntity.
                    }
                    var destination = Objects.requireNonNull(destinationIterator.next());
                    var sourceIterator = destinationInstance.iterator(sourceEntity, random);
                    if (!sourceIterator.hasNext()) {
                        return null; // No index for sourceEntity.
                    }
                    var start = Objects.requireNonNull(sourceIterator.next()).index();
                    var sourceSize = solutionView.countValues(variableMetaModel, sourceEntity);
                    if (start == sourceSize) {
                        return null; // The append position starts no block.
                    }
                    var length = 1 + random.nextInt(sourceSize - start);
                    // Some value in the block may be outside the destination entity's value range.
                    return isBlockInRange(solutionView, sourceEntity, start, length, destinationEntity)
                            ? new RelocateValueBlockMove(variableMetaModel, sourceEntity, start, length,
                                    destinationEntity, destination.index())
                            : null;
                }
            };
        });
    }

    private boolean isBlockInRange(SolutionView<TestdataListEntityProvidingSolution> solutionView,
            TestdataListEntityProvidingEntity sourceEntity, int start, int length,
            TestdataListEntityProvidingEntity destinationEntity) {
        for (var offset = 0; offset < length; offset++) {
            var value = solutionView.getValueAtIndex(variableMetaModel, sourceEntity, start + offset);
            if (!solutionView.isValueInRange(variableMetaModel, destinationEntity, value)) {
                return false;
            }
        }
        return true;
    }

}
