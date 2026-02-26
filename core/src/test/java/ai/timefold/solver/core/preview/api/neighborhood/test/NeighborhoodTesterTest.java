package ai.timefold.solver.core.preview.api.neighborhood.test;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.preview.api.move.builtin.ChangeMove;
import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.assertj.core.error.AssertionErrorCreator;
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

        var evaluatedNeighborhood = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var moveList = evaluatedNeighborhood
                .getMovesAsList(move -> (ChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move);

        assertThat(moveList).hasSize(4);

        // Look for the move in the list, instead of expecting them at any particular position.
        var firstMove = moveList.stream()
                .filter(move -> move.getPlanningEntities().contains(firstEntity)
                        && move.getPlanningValues().contains(firstValue))
                .findFirst()
                .orElseThrow(() -> new AssertionErrorCreator()
                        .assertionError("Move not found in move list."));
        evaluatedNeighborhood.getMoveTestContext()
                .executeTemporarily(firstMove, solutionView -> assertThat(firstEntity.getValue())
                        .isEqualTo(firstValue));
        assertThat(firstEntity.getValue()).isNull();

        // There should be 3 more moves in the iterator, each different from the first.
        // - Set the firstEntity value to secondValue,
        // - set the secondEntity value to firstValue,
        // - set the secondEntity value to secondValue.
        moveList.stream()
                .filter(move -> move.getPlanningEntities().contains(firstEntity)
                        && move.getPlanningValues().contains(secondValue))
                .findFirst()
                .orElseThrow(() -> new AssertionErrorCreator()
                        .assertionError("Move not found in move list."));

        moveList.stream()
                .filter(move -> move.getPlanningEntities().contains(secondEntity)
                        && move.getPlanningValues().contains(firstValue))
                .findFirst()
                .orElseThrow(() -> new AssertionErrorCreator()
                        .assertionError("Move not found in move list."));

        moveList.stream()
                .filter(move -> move.getPlanningEntities().contains(secondEntity)
                        && move.getPlanningValues().contains(secondValue))
                .findFirst()
                .orElseThrow(() -> new AssertionErrorCreator()
                        .assertionError("Move not found in move list."));
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

        var evaluatedNeighborhood = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var moveList =
                evaluatedNeighborhood
                        .getMovesAsList(move -> (ChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(4);

        // Look for the move in the list, instead of expecting them at any particular position.
        var firstMove = moveList.stream()
                .filter(move -> move.getPlanningEntities().contains(firstEntity)
                        && move.getPlanningValues().contains(firstValue))
                .findFirst()
                .orElseThrow(() -> new AssertionErrorCreator()
                        .assertionError("Move not found in move list."));
        evaluatedNeighborhood.getMoveTestContext()
                .execute(firstMove);
        assertThat(firstEntity.getValue()).isEqualTo(firstValue);

        // New move list, now that the move has been executed, changing firstEntity's value to firstValue.
        // There should be 3 more moves in the iterator, each different from the first.
        // - Set the firstEntity value to secondValue,
        // - set the secondEntity value to firstValue,
        // - set the secondEntity value to secondValue.
        moveList.stream()
                .filter(move -> move.getPlanningEntities().contains(firstEntity)
                        && move.getPlanningValues().contains(secondValue))
                .findFirst()
                .orElseThrow(() -> new AssertionErrorCreator()
                        .assertionError("Move not found in move list."));

        moveList.stream()
                .filter(move -> move.getPlanningEntities().contains(secondEntity)
                        && move.getPlanningValues().contains(firstValue))
                .findFirst()
                .orElseThrow(() -> new AssertionErrorCreator()
                        .assertionError("Move not found in move list."));

        moveList.stream()
                .filter(move -> move.getPlanningEntities().contains(secondEntity)
                        && move.getPlanningValues().contains(secondValue))
                .findFirst()
                .orElseThrow(() -> new AssertionErrorCreator()
                        .assertionError("Move not found in move list."));
    }

}
