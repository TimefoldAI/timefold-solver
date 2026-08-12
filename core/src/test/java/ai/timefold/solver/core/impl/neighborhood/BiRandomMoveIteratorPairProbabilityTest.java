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

import org.assertj.core.data.Percentage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Protects {@code BiRandomMoveIterator}'s per-pair fairness fix: a left picked uniformly and then rejected
 * with probability {@code 1 - weight/bound} makes the resulting pair probability uniform, not just the left
 * draw, so a pair in a small bucket must not be drawn any more (or less) often, per pair, than a pair in a
 * large bucket.
 * <p>
 * The joiner here must be an indexing {@code equal}, not {@code filtering()}: a filtering()-only join has no
 * composite key to restrict the right side by, so {@code weight == bound} for every left always, a structural
 * no-op this fix deliberately leaves alone (see {@code BiRandomMoveIterator}'s javadoc). This test used to pin
 * the opposite (biased) behavior, over a filtering()-only joiner, before the fix landed.
 */
class BiRandomMoveIteratorPairProbabilityTest {

    private static final int DRAW_COUNT = 1_000_000;
    private static final int SMALL_PARTNER_COUNT = 2;
    private static final int LARGE_PARTNER_COUNT = 20;

    @Test
    void perPairProbabilityIsUniformAcrossBucketSizes() {
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
        // Fixed, uniform-over-pairs probability is 1 / (leftCount * totalPartnerCount) for every pair,
        // regardless of which bucket it is in; before the fix, the small bucket's rate was ~10x the large one's.
        assertThat(smallPerPairProbability)
                .as("a pair's per-pair rate must not depend on its bucket size (small: %s, large: %s)",
                        smallPerPairProbability, largePerPairProbability)
                .isCloseTo(largePerPairProbability, Percentage.withPercentage(5));
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
     * Picks (entity, value) pairs whose codes share a prefix, so "small" only ever pairs with "small-v*"
     * values, and "large" only ever pairs with "large-v*" values. An indexing {@code equal} join, not
     * {@code filtering()}: the fix this test protects is a structural no-op without one.
     */
    @NullMarked
    private record PickMatchingPair(PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable)
            implements
                MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            var matchingPrefix = NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(
                    TestdataEntity::getCode, value -> value.getCode().split("-")[0]);
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream, matchingPrefix)
                    .asMove((solutionView, entity, value) -> Moves.change(variable, entity, value));
        }

    }

}
