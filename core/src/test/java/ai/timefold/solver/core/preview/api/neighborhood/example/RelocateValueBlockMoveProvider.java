package ai.timefold.solver.core.preview.api.neighborhood.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.jspecify.annotations.NullMarked;

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

        // Plain, unjoined:
        // every entity's destinations at once, filtered inside the loop below.
        var destinations = factory.forEachDestination(variableMetaModel).asCachedDataset();

        return factory.buildMoveStream((session, random) -> {
            var pairInstance =
                    session.getInstance(entityPairs);
            if (pairInstance.size() == 0) {
                return Collections.emptyIterator(); // Fewer than 2 entities.
            }
            var destinationInstance = session.getInstance(destinations);
            var solutionView = session.getSolutionView();

            var moveList = new ArrayList<Move<TestdataListEntityProvidingSolution>>();
            var pairIterator = pairInstance.iterator();
            while (pairIterator.hasNext()) {
                pairIterator.next();
                var entityA = pairIterator.a();
                var entityB = pairIterator.b();
                collectRelocations(solutionView, destinationInstance, entityA, entityB, moveList);
                collectRelocations(solutionView, destinationInstance, entityB, entityA, moveList);
            }
            return moveList.iterator();
        });
    }

    private void collectRelocations(SolutionView<TestdataListEntityProvidingSolution> solutionView,
            UniDatasetInstance<PositionInList> destinationInstance, TestdataListEntityProvidingEntity sourceEntity,
            TestdataListEntityProvidingEntity destinationEntity,
            List<Move<TestdataListEntityProvidingSolution>> moveList) {
        var sourceSize = solutionView.countValues(variableMetaModel, sourceEntity);
        var destinationIterator = destinationInstance.iterator();
        while (destinationIterator.hasNext()) {
            var destination = Objects.requireNonNull(destinationIterator.next());
            if (destination.entity() != destinationEntity) {
                continue; // Not a destination in the entity we're relocating into this time.
            }
            for (var start = 0; start < sourceSize; start++) {
                for (var length = 1; start + length <= sourceSize; length++) {
                    if (isBlockInRange(solutionView, sourceEntity, start, length, destinationEntity)) {
                        moveList.add(new RelocateValueBlockMove(variableMetaModel, sourceEntity, start, length,
                                destinationEntity, destination.index()));
                    }
                }
            }
        }
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
