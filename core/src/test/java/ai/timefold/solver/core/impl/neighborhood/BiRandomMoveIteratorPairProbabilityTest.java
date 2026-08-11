package ai.timefold.solver.core.impl.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Pins the accepted per-left pair bias of {@code BiRandomMoveIterator}
 * (left picked uniformly, then right picked uniformly inside that left's bucket, same as the classic swap move selector):
 * a left tuple with few partners keeps a higher probability per pair
 * than a left tuple with many partners.
 * <p>
 * This is a deliberate, accepted trade-off, not a bug;
 * this test exists so that fixing it later (uniform probability over pairs instead of over lefts)
 * is a visible, deliberate change, not a silent side effect.
 */
class BiRandomMoveIteratorPairProbabilityTest {

    private static final int DRAW_COUNT = 40_000;
    private static final int SMALL_PARTNER_COUNT = 2;
    private static final int LARGE_PARTNER_COUNT = 20;

    @Test
    void smallBucketPairsAreDrawnMoreOftenPerPairThanLargeBucketPairs() {
        var smallEntity = new TestdataEntity("small");
        var largeEntity = new TestdataEntity("large");
        var smallValues = values("small-v", SMALL_PARTNER_COUNT);
        var largeValues = values("large-v", LARGE_PARTNER_COUNT);

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(smallEntity, largeEntity));
        var valueList = new ArrayList<TestdataValue>();
        valueList.addAll(smallValues);
        valueList.addAll(largeValues);
        solution.setValueList(valueList);

        var variable = TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);
        var moveProvider = new PickMatchingPair(variable);
        var context = NeighborhoodTester.build(moveProvider, TestdataSolution.buildMetaModel()).using(solution);
        var iterator = context.getMovesAsIterator(move -> move);

        var smallPairMoves = movesFor(variable, smallEntity, smallValues);
        var largePairMoves = movesFor(variable, largeEntity, largeValues);
        var smallCount = 0;
        var largeCount = 0;
        for (var i = 0; i < DRAW_COUNT; i++) {
            var move = iterator.next();
            if (smallPairMoves.contains(move)) {
                smallCount++;
            } else if (largePairMoves.contains(move)) {
                largeCount++;
            }
        }

        // Both must be reachable at all.
        assertThat(smallCount).isPositive();
        assertThat(largeCount).isPositive();

        var smallPerPairProbability = smallCount / (double) SMALL_PARTNER_COUNT;
        var largePerPairProbability = largeCount / (double) LARGE_PARTNER_COUNT;
        // Expected ratio is exactly LARGE_PARTNER_COUNT / SMALL_PARTNER_COUNT (=10); a wide margin keeps
        // this from being a flaky test while still catching a future "uniform over pairs" change.
        assertThat(smallPerPairProbability)
                .as("a pair in the %d-partner bucket should be drawn far more often, per pair, than one in the %d-partner bucket",
                        SMALL_PARTNER_COUNT, LARGE_PARTNER_COUNT)
                .isGreaterThan(largePerPairProbability * 3);
    }

    private static List<TestdataValue> values(String prefix, int count) {
        var values = new ArrayList<TestdataValue>(count);
        for (var i = 0; i < count; i++) {
            values.add(new TestdataValue(prefix + i));
        }
        return values;
    }

    private static Set<Move<TestdataSolution>> movesFor(
            PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable, TestdataEntity entity,
            List<TestdataValue> values) {
        Set<Move<TestdataSolution>> moves = new HashSet<>();
        for (var value : values) {
            moves.add(Moves.change(variable, entity, value));
        }
        return moves;
    }

    /**
     * Picks (entity, value) pairs where the value's code starts with the entity's code,
     * so "small" only ever pairs with "small-v*" values,
     * and "large" only ever pairs with "large-v*" values.
     */
    @NullMarked
    private record PickMatchingPair(PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable)
            implements
                MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            var matchingPrefix = NeighborhoodsJoiners
                    .<TestdataSolution, TestdataEntity, TestdataValue> filtering(
                            (solutionView, entity, value) -> value.getCode().startsWith(entity.getCode()));
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream, matchingPrefix)
                    .asMove((solutionView, entity, value) -> Moves.change(variable, entity, value));
        }

    }

}
