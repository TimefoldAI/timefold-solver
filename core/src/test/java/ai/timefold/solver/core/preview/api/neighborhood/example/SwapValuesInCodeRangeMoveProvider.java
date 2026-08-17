package ai.timefold.solver.core.preview.api.neighborhood.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

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
import org.jspecify.annotations.Nullable;

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
            // Both walks draw with replacement and never end.
            var pairIterator = pairInstance.iterator(random);
            var rangeIterator = rangeInstance.iterator(random);
            return new Iterator<>() {

                private @Nullable Move<TestdataSolution> nextMove;

                @Override
                public boolean hasNext() {
                    while (nextMove == null && pairIterator.hasNext() && rangeIterator.hasNext()) {
                        nextMove = draw();
                    }
                    return nextMove != null;
                }

                @Override
                public Move<TestdataSolution> next() {
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
                private @Nullable Move<TestdataSolution> draw() {
                    pairIterator.next();
                    var valueA = Objects.requireNonNull(pairIterator.a());
                    var valueB = Objects.requireNonNull(pairIterator.b());
                    rangeIterator.next();
                    var start = Objects.requireNonNull(rangeIterator.a());
                    var end = Objects.requireNonNull(rangeIterator.b());

                    // This list holds the components of the single composed move, not the neighborhood.
                    var swapList = new ArrayList<Move<TestdataSolution>>();
                    collectSwaps(valueEntityInstance, valueA, valueB, start, end, random, swapList);
                    collectSwaps(valueEntityInstance, valueB, valueA, start, end, random, swapList);
                    // An empty list means the drawn range held no entity for either value.
                    return swapList.isEmpty() ? null : Moves.compose(swapList);
                }
            };
        });
    }

    private void collectSwaps(BiDatasetInstance<TestdataValue, TestdataEntity> valueEntityInstance,
            TestdataValue from, TestdataValue to, String start, String end, RandomGenerator random,
            List<Move<TestdataSolution>> swapList) {
        // Every entity of this value, exactly once, in random order.
        var entityIterator = valueEntityInstance.exhaustiveIterator(from, random);
        while (entityIterator.hasNext()) {
            var entity = entityIterator.next();
            var code = entity.getCode();
            if (code.compareTo(start) >= 0 && code.compareTo(end) <= 0) {
                swapList.add(Moves.change(variableMetaModel, entity, to));
            }
        }
    }

}
