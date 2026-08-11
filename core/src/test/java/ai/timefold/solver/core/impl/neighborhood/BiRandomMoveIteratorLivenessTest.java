package ai.timefold.solver.core.impl.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

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

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * {@code BiRandomMoveIterator} must always terminate:
 * an empty left dataset, an empty right dataset, and a {@code filtering()} joiner that rejects every pair
 * are all different ways for every left tuple to end up dead,
 * and none of them may hang.
 * A plain, synchronous assertion is enough,
 * since each case is bounded by construction (at most one bail-out per left tuple);
 * a genuine regression would hang the whole build,
 * not just fail an assertion,
 * since the working random used here is checked for thread ownership and
 * cannot safely be driven from a JUnit timeout's watcher thread.
 */
class BiRandomMoveIteratorLivenessTest {

    private static final PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> VARIABLE =
            TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

    @Test
    void emptyRightDataset_terminates() {
        assertNeverHangs(new PickEntityAndValue(false, true));
    }

    @Test
    void emptyLeftDataset_terminates() {
        assertNeverHangs(new PickEntityAndValue(true, false));
    }

    @Test
    void filteringJoinerRejectsEveryPair_terminates() {
        assertNeverHangs(new PickEntityAndValue(false, false));
    }

    private void assertNeverHangs(MoveProvider<TestdataSolution> moveProvider) {
        var solution = TestdataSolution.generateSolution(3, 3);
        var context = NeighborhoodTester.build(moveProvider, TestdataSolution.buildMetaModel()).using(solution);
        var iterator = context.getMovesAsIterator(move -> move);

        assertThat(iterator.hasNext()).isFalse();
    }

    /**
     * Picks a (entity, value) pair, matching nothing:
     * either the left or the right stream is forced empty by a rejecting {@code filter()},
     * or (if neither is) the {@code filtering()} joiner itself rejects every pair.
     */
    @NullMarked
    private record PickEntityAndValue(boolean forceEmptyLeft, boolean forceEmptyRight)
            implements
                MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
            if (forceEmptyLeft) {
                entityStream = entityStream.filter((solutionView, entity) -> false);
            }
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            if (forceEmptyRight) {
                valueStream = valueStream.filter((solutionView, value) -> false);
            }
            var rejectEveryPair = NeighborhoodsJoiners
                    .<TestdataSolution, TestdataEntity, TestdataValue> filtering((solutionView, entity, value) -> false);
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream, rejectEveryPair)
                    .asMove((solutionView, entity, value) -> Moves.change(VARIABLE, entity, value));
        }

    }

}
