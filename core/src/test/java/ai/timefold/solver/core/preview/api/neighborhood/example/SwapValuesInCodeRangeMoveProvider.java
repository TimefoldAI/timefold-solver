package ai.timefold.solver.core.preview.api.neighborhood.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;

/**
 * For a randomly picked pair of distinct values,
 * and a randomly picked range of entity codes,
 * reassigns every entity in that range currently holding one of the two values to the other,
 * and composes the result into a single move.
 */
@NullMarked
final class SwapValuesInCodeRangeMoveProvider implements MoveProvider<TestdataSolution> {

    private final PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variableMetaModel;

    SwapValuesInCodeRangeMoveProvider(
            PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> factory) {
        // Cached: queried per-A, for both values of every candidate pair.
        var valueEntities = factory.forEach(TestdataValue.class, false)
                .join(factory.forEach(TestdataEntity.class, false),
                        NeighborhoodsJoiners.equal(value -> value, TestdataEntity::getValue))
                .asCachedDataset();

        // Just-in-time: many pairs, few ever drawn.
        // Ordered, so no mirrored duplicate.
        var valuePairs = factory.forEach(TestdataValue.class, false).asCachedDataset()
                .join(factory.forEach(TestdataValue.class, false), NeighborhoodsJoiners.lessThan(TestdataValue::getCode));

        // Just-in-time, same reasoning: the range bounds are drawn sparsely too.
        var entityCodes = factory.forEach(TestdataEntity.class, false)
                .map((solutionView, entity) -> entity.getCode())
                .distinct();
        var codeRanges = entityCodes.asCachedDataset().join(entityCodes, NeighborhoodsJoiners.lessThan(code -> code));

        return factory.buildMoveStream((session, random) -> {
            var valueEntityInstance = session.getInstance(valueEntities);
            var pairInstance = session.getInstance(valuePairs);
            var rangeInstance = session.getInstance(codeRanges);
            if (pairInstance.size() == 0 || rangeInstance.size() == 0) {
                return Collections.emptyIterator(); // Fewer than 2 values, or fewer than 2 distinct entity codes.
            }

            var moveList = new ArrayList<Move<TestdataSolution>>();
            var pairIterator = pairInstance.iterator();
            while (pairIterator.hasNext()) {
                pairIterator.next();
                var valueA = pairIterator.a();
                var valueB = pairIterator.b();

                var rangeIterator = rangeInstance.iterator();
                while (rangeIterator.hasNext()) {
                    rangeIterator.next();
                    var start = rangeIterator.a();
                    var end = rangeIterator.b();

                    var swapList = new ArrayList<Move<TestdataSolution>>();
                    collectSwaps(valueEntityInstance, valueA, valueB, start, end, swapList);
                    collectSwaps(valueEntityInstance, valueB, valueA, start, end, swapList);
                    if (!swapList.isEmpty()) {
                        moveList.add(Moves.compose(swapList));
                    }
                    // Else: the picked range held no entity for either value. Nothing to add for this combination.
                }
            }
            return moveList.iterator();
        });
    }

    private void collectSwaps(BiDatasetInstance<TestdataValue, TestdataEntity> valueEntityInstance,
            TestdataValue from, TestdataValue to, String start, String end, List<Move<TestdataSolution>> swapList) {
        var entityIterator = valueEntityInstance.iterator(from);
        while (entityIterator.hasNext()) {
            var entity = entityIterator.next();
            var code = entity.getCode();
            if (code.compareTo(start) >= 0 && code.compareTo(end) <= 0) {
                swapList.add(Moves.change(variableMetaModel, entity, to));
            }
        }
    }

}
