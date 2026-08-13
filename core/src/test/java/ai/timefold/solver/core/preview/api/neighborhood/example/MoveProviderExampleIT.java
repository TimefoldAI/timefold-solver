package ai.timefold.solver.core.preview.api.neighborhood.example;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * FULL_ASSERT score-corruption checks for the three example moves that carry new {@code execute} logic:
 * each executes exactly one already-validated move -
 * the same one each provider's own unit test proves is produced -
 * on a from-scratch-recalculating score director,
 * relying on {@link EnvironmentMode#FULL_ASSERT} to throw
 * if incremental and from-scratch scoring, or shadow variable state, ever disagree.
 */
@NullMarked
@Execution(ExecutionMode.CONCURRENT)
class MoveProviderExampleIT {

    @Test
    void swapValuesInCodeRangeDoesNotCorruptScore() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).<TestdataValue> basicVariable();

        // Same fixture as SwapValuesInCodeRangeMoveProviderTest: 3 values, 2 entities, v2 unassigned.
        var solution = TestdataSolution.generateSolution(3, 2);
        var e0 = solution.getEntityList().getFirst();
        var v2 = solution.getValueList().get(2);
        var move = Moves.change(variableMetaModel, e0, v2);

        assertNoScoreCorruption(TestdataSolution.buildSolutionDescriptor(), solution, move);
    }

    @Test
    void swapEntityTailsDoesNotCorruptScore() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class).<TestdataListValue> listVariable();

        // Same fixture as SwapEntityTailsMoveProviderTest: e0=[v0,v2,v4], e1=[v1,v3,v5], cut at index 1.
        var solution = TestdataListSolution.generateInitializedSolution(6, 2);
        var e0 = solution.getEntityList().get(0);
        var e1 = solution.getEntityList().get(1);
        var move = new SwapEntityTailsMove(variableMetaModel, e0, e1, 1);

        assertNoScoreCorruption(TestdataListSolution.buildSolutionDescriptor(), solution, move);
    }

    @Test
    void relocateValueBlockDoesNotCorruptScore() {
        var solutionMetaModel = TestdataListEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntityProvidingEntity.class)
                .<TestdataListEntityProvidingValue> listVariable();

        // Same fixture as RelocateValueBlockMoveProviderTest: A may hold all three; B may only hold v0 and v1.
        var v0 = new TestdataListEntityProvidingValue("v0");
        var v1 = new TestdataListEntityProvidingValue("v1");
        var v2 = new TestdataListEntityProvidingValue("v2");
        // A list variable's backing list must be mutable; List.of(...) would throw on the first real move.
        var entityA = new TestdataListEntityProvidingEntity("A", List.of(v0, v1, v2), new ArrayList<>(List.of(v0, v1, v2)));
        var entityB = new TestdataListEntityProvidingEntity("B", List.of(v0, v1), new ArrayList<>());
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(entityA, entityB));
        var move = new RelocateValueBlockMove(variableMetaModel, entityA, 0, 2, entityB, 0);

        assertNoScoreCorruption(TestdataListEntityProvidingSolution.buildSolutionDescriptor(), solution, move);
    }

    private static <Solution_> void assertNoScoreCorruption(SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ solution, Move<Solution_> move) {
        assertThatCode(() -> {
            try (var scoreDirector = new BavetConstraintStreamScoreDirectorFactory<>(solutionDescriptor,
                    constraintFactory -> new Constraint[] {
                            constraintFactory.forEach(Object.class).penalize(SimpleScore.ONE)
                                    .asConstraint("dummy constraint") },
                    EnvironmentMode.FULL_ASSERT).buildScoreDirector()) {
                scoreDirector.setWorkingSolution(solution);
                scoreDirector.calculateScore();
                // Exercise the record-and-undo path before the permanent one;
                // increases the chance that FULL_ASSERT catches a corruption specific to temporary move execution.
                scoreDirector.executeTemporaryMove(move, true);
                scoreDirector.executeMove(move);
                scoreDirector.calculateScore();
            }
        }).doesNotThrowAnyException();
    }

}
