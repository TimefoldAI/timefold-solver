package ai.timefold.solver.core.preview.api.neighborhood.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

// Much of the test coverage for NeighborhoodTester is in tests for the specific MoveProviders.
// There is no better coverage than real-world use cases. (Eating our own dog food.)
class NeighborhoodTesterTest {

    @Test
    void temporaryMoveExecutionDoesNotAffectMoveIterator() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();

        var solution = TestdataSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValue(null);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);

        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var firstMove = Moves.change(variableMetaModel, firstEntity, firstValue);
        context.producesAllOf(
                firstMove,
                Moves.change(variableMetaModel, firstEntity, secondValue),
                Moves.change(variableMetaModel, secondEntity, firstValue),
                Moves.change(variableMetaModel, secondEntity, secondValue));

        context.getMoveTestContext()
                .executeTemporarily(firstMove, solutionView -> assertThat(firstEntity.getValue())
                        .isEqualTo(firstValue));
        assertThat(firstEntity.getValue()).isNull();

        // After a temporary execution and its automatic revert, the same 4 moves are still producible.
        context.producesAllOf(
                firstMove,
                Moves.change(variableMetaModel, firstEntity, secondValue),
                Moves.change(variableMetaModel, secondEntity, firstValue),
                Moves.change(variableMetaModel, secondEntity, secondValue));
    }

    @Test
    void newIteratorAfterMoveExecution() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();

        var solution = TestdataSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValue(null);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);

        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var firstMove = Moves.change(variableMetaModel, firstEntity, firstValue);
        context.producesAllOf(
                firstMove,
                Moves.change(variableMetaModel, firstEntity, secondValue),
                Moves.change(variableMetaModel, secondEntity, firstValue),
                Moves.change(variableMetaModel, secondEntity, secondValue));

        context.getMoveTestContext().execute(firstMove);
        assertThat(firstEntity.getValue()).isEqualTo(firstValue);

        // New moves, now that firstEntity has been permanently changed to firstValue.
        context.producesAllOf(
                Moves.change(variableMetaModel, firstEntity, secondValue),
                Moves.change(variableMetaModel, secondEntity, firstValue),
                Moves.change(variableMetaModel, secondEntity, secondValue));
        context.producesNoneOf(firstMove); // No-op: firstEntity is already firstValue.
    }

    @Test
    void withinOverridesTheIterationLimitWithoutMutatingTheOriginalContext() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();

        var solution = TestdataSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().getFirst();
        firstEntity.setValue(null);
        var firstValue = solution.getValueList().getFirst();

        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var narrowedContext = context.within(5);
        narrowedContext.producesAllOf(Moves.change(variableMetaModel, firstEntity, firstValue));
        // The original context is unaffected and still uses its own (larger) default limit.
        context.producesAllOf(Moves.change(variableMetaModel, firstEntity, firstValue));
    }

    @Test
    void producesAllOfFailsWhenTheMoveIsNeverProduced() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();

        var solution = TestdataSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().getFirst();
        var currentValue = firstEntity.getValue();

        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        // Changing an entity to its own current value is a no-op; ChangeMoveProvider never produces it.
        assertThatThrownBy(() -> context.within(50)
                .producesAllOf(Moves.change(variableMetaModel, firstEntity, currentValue)))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void producesNoneOfFailsWhenTheMoveIsProduced() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();

        var solution = TestdataSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().getFirst();
        firstEntity.setValue(null);
        var firstValue = solution.getValueList().getFirst();

        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        assertThatThrownBy(() -> context.producesNoneOf(Moves.change(variableMetaModel, firstEntity, firstValue)))
                .isInstanceOf(AssertionError.class);
    }

}
