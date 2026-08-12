package ai.timefold.solver.core.impl.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * End-to-end regression for the {@code DefaultRetiringRandomIterator} fix, exercised through
 * {@code BiRandomMoveIterator}'s left draws. Before the fix, retiring an interior left entity biased its
 * surviving neighbors (the old "snap to nearest active index" correction); only edge retirements were fair.
 */
@Execution(ExecutionMode.CONCURRENT)
class BiRandomMoveIteratorRetirementProbabilityIT {

    private static final int ENTITY_COUNT = 20;
    // Interior retirements, not edges: the old bug was already unbiased at the edges.
    private static final Set<Integer> DEAD_ENTITY_INDEX_SET = Set.of(4, 8, 12, 16);
    private static final int WARM_UP_DRAW_COUNT = 1_000;
    private static final int MEASURED_DRAW_COUNT = 200_000;

    @Test
    void interiorRetirementsStayUniformOverSurvivingEntities() {
        var entityList = new ArrayList<TestdataEntity>(ENTITY_COUNT);
        var valueList = new ArrayList<TestdataValue>();
        for (var i = 0; i < ENTITY_COUNT; i++) {
            if (DEAD_ENTITY_INDEX_SET.contains(i)) {
                // No value shares this entity's code, so it can never find a match and gets retired.
                entityList.add(new TestdataEntity("dead" + i));
            } else {
                entityList.add(new TestdataEntity("k" + i));
                valueList.add(new TestdataValue("k" + i));
            }
        }
        var solution = new TestdataSolution("solution");
        solution.setEntityList(entityList);
        solution.setValueList(valueList);

        var variable = TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);
        var moveProvider = new PickMatchingByCode(variable);
        var context = NeighborhoodTester.build(moveProvider, TestdataSolution.buildMetaModel()).using(solution);
        var iterator = context.getMovesAsIterator(move -> move);

        // Give every dead entity plenty of chances to be drawn and permanently retired before measuring.
        for (var i = 0; i < WARM_UP_DRAW_COUNT; i++) {
            iterator.next();
        }

        var counts = new HashMap<String, Integer>();
        for (var i = 0; i < MEASURED_DRAW_COUNT; i++) {
            var move = iterator.next();
            var entity = (TestdataEntity) move.getPlanningEntities().getFirst();
            counts.merge(entity.getCode(), 1, Integer::sum);
        }

        var liveEntityCodeSet = entityList.stream()
                .map(TestdataEntity::getCode)
                .filter(code -> !code.startsWith("dead"))
                .collect(Collectors.toSet());
        // The dead entities must never appear, and every live entity must be reachable.
        assertThat(counts.keySet()).containsExactlyInAnyOrderElementsOf(liveEntityCodeSet);

        var expected = MEASURED_DRAW_COUNT / (double) liveEntityCodeSet.size();
        for (var entry : counts.entrySet()) {
            var actual = entry.getValue();
            assertThat(actual)
                    .as(() -> "Entity %s drawn %d times, expected close to %.0f (uniform over %d live entities)."
                            .formatted(entry.getKey(), actual, expected, liveEntityCodeSet.size()))
                    .isCloseTo((int) expected, Percentage.withPercentage(10));
        }
    }

    /**
     * Picks an (entity, value) pair whose codes are equal, so every entity has at most one matching value.
     */
    @NullMarked
    private record PickMatchingByCode(PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable)
            implements
                MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            var matchingCode = NeighborhoodsJoiners.equal(TestdataEntity::getCode, TestdataValue::getCode);
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream, matchingCode)
                    .asMove((solutionView, entity, value) -> Moves.change(variable, entity, value));
        }

    }

}
