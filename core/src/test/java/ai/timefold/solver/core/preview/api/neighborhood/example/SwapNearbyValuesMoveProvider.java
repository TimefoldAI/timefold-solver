package ai.timefold.solver.core.preview.api.neighborhood.example;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.jspecify.annotations.NullMarked;

/**
 * Swaps two assigned values whose list positions are at most one index apart, in different entities,
 * as a single built-in {@link Moves#swap(PlanningListVariableMetaModel, PositionInList, PositionInList) swap move} -
 * no custom move, no composition.
 */
@NullMarked
final class SwapNearbyValuesMoveProvider implements MoveProvider<TestdataListEntityProvidingSolution> {

    private final PlanningListVariableMetaModel<TestdataListEntityProvidingSolution, TestdataListEntityProvidingEntity, TestdataListEntityProvidingValue> variableMetaModel;

    SwapNearbyValuesMoveProvider(
            PlanningListVariableMetaModel<TestdataListEntityProvidingSolution, TestdataListEntityProvidingEntity, TestdataListEntityProvidingValue> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<TestdataListEntityProvidingSolution> build(
            MoveStreamFactory<TestdataListEntityProvidingSolution> factory) {
        var nearbyValuePairs = factory.forEachAssignedValue(variableMetaModel)
                .asCachedDataset()
                .join(factory.forEachAssignedValue(variableMetaModel),
                        NeighborhoodsJoiners.lessThan(TestdataListEntityProvidingValue::getCode),
                        NeighborhoodsJoiners.filtering(this::isNearbySwap));

        return factory.buildMoveStream((session, random) -> {
            var instance = session.getInstance(nearbyValuePairs);
            var pairIterator = instance.exhaustiveIterator(random);
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return pairIterator.hasNext();
                }

                @Override
                public Move<TestdataListEntityProvidingSolution> next() {
                    pairIterator.next();
                    var solutionView = session.getSolutionView();
                    var positionA = solutionView.getPositionOf(variableMetaModel, pairIterator.a()).ensureAssigned();
                    var positionB = solutionView.getPositionOf(variableMetaModel, pairIterator.b()).ensureAssigned();
                    return Moves.swap(variableMetaModel, positionA, positionB);
                }
            };
        });
    }

    private boolean isNearbySwap(SolutionView<TestdataListEntityProvidingSolution> solutionView,
            TestdataListEntityProvidingValue valueA, TestdataListEntityProvidingValue valueB) {
        var positionA = solutionView.getPositionOf(variableMetaModel, valueA).ensureAssigned();
        var positionB = solutionView.getPositionOf(variableMetaModel, valueB).ensureAssigned();
        return positionA.entity() != positionB.entity()
                && Math.abs(positionA.index() - positionB.index()) <= 1
                && solutionView.isValueInRange(variableMetaModel, positionB.entity(), valueA)
                && solutionView.isValueInRange(variableMetaModel, positionA.entity(), valueB);
    }

}
