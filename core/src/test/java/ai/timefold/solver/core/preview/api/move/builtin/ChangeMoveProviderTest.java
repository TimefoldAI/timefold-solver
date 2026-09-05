package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Collections;

import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.incomplete.TestdataIncompleteValueRangeEntity;
import ai.timefold.solver.core.testdomain.valuerange.incomplete.TestdataIncompleteValueRangeSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ChangeMoveProviderTest {

    @Test
    void fromSolution() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();

        var solution = TestdataSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);
        secondEntity.setValue(secondValue);

        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.change(variableMetaModel, firstEntity, secondValue),
                Moves.change(variableMetaModel, secondEntity, firstValue));
        // Changing an entity to its own current value would be a no-op; the provider does not generate it.
        context.producesNoneOf(
                Moves.change(variableMetaModel, firstEntity, firstValue),
                Moves.change(variableMetaModel, secondEntity, secondValue));
    }

    @Test
    void fromSolutionIncompleteValueRange() {
        var solutionMetaModel = TestdataIncompleteValueRangeSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataIncompleteValueRangeEntity.class)
                .basicVariable();

        // The point of this test is to ensure that the move provider skips values that are not in the value range.
        var solution = TestdataIncompleteValueRangeSolution.generateSolution(2, 2);
        var valueNotInValueRange = new TestdataValue("third");
        solution.setValueListNotInValueRange(Collections.singletonList(valueNotInValueRange));

        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);
        firstEntity.setValue(firstValue);
        secondEntity.setValue(secondValue);

        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.change(variableMetaModel, firstEntity, secondValue),
                Moves.change(variableMetaModel, secondEntity, firstValue));
        context.producesNoneOf(
                Moves.change(variableMetaModel, firstEntity, firstValue), // No-op.
                Moves.change(variableMetaModel, secondEntity, secondValue), // No-op.
                Moves.change(variableMetaModel, firstEntity, valueNotInValueRange), // Not in value range.
                Moves.change(variableMetaModel, secondEntity, valueNotInValueRange)); // Not in value range.
    }

    @Test
    void fromEntity() {
        var solutionMetaModel = TestdataEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class)
                .basicVariable();

        var solution = TestdataEntityProvidingSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = firstEntity.getValueRange().getFirst();

        // One move is expected:
        // - firstEntity is already assigned to firstValue, the only possible value; skip.
        // - Assign secondEntity to firstValue,
        //   as it is currently assigned to secondValue, and the value range only contains firstValue.
        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(Moves.change(variableMetaModel, secondEntity, firstValue));
        context.producesNoneOf(Moves.change(variableMetaModel, firstEntity, firstValue)); // No-op.
    }

    @Test
    void fromEntityAllowsUnassigned() {
        var solutionMetaModel = TestdataAllowsUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedEntityProvidingSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getEntityList().get(0).getValueRange().getFirst();

        // One move is expected:
        // - secondEntity is assigned to secondValue, and the value range only contains firstValue;
        //   so a change to firstValue is generated.
        // - firstEntity is assigned to firstValue, same as its only possible non-null value; no change.
        // crossingNull=false locks in this pre-existing behaviour;
        // see crossingNullDefaultTrueAssignsAndUnassigns for the default (true, since this variable allows unassigned values).
        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel, false), solutionMetaModel)
                .using(solution);
        context.producesAllOf(Moves.change(variableMetaModel, secondEntity, firstValue));
        context.producesNoneOf(Moves.change(variableMetaModel, firstEntity, firstValue)); // No-op.
    }

    @Test
    void pinnedEntitySkipped() {
        var solutionMetaModel = TestdataPinnedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntity.class)
                .basicVariable();

        var solution = TestdataPinnedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().getFirst();
        var secondValue = solution.getValueList().get(1);
        firstEntity.setPinned(true);

        // firstEntity is pinned; only secondEntity can change to firstValue.
        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(Moves.change(variableMetaModel, secondEntity, firstValue));
        // secondEntity is already assigned to secondValue; that would be a no-op.
        // firstEntity is pinned, so no move exists for it at all, to any value.
        context.producesNoneOf(
                Moves.change(variableMetaModel, secondEntity, secondValue),
                Moves.change(variableMetaModel, firstEntity, firstValue),
                Moves.change(variableMetaModel, firstEntity, secondValue));
    }

    @Test
    void fromSolutionAllowsUnassigned() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var secondEntity = solution.getEntityList().get(1); // Assigned to secondValue.
        var firstValue = solution.getValueList().getFirst(); // Not assigned to any entity.
        var secondValue = solution.getValueList().get(1);

        // First entity is assigned to null, so it is filtered out of the source when crossingNull=false.
        // Second entity is assigned to secondValue, so the only applicable move changes it to firstValue.
        // crossingNull=false locks in this pre-existing behaviour;
        // see crossingNullDefaultTrueAssignsAndUnassigns for the default (true, since this variable allows unassigned values).
        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel, false), solutionMetaModel)
                .using(solution);
        context.producesAllOf(Moves.change(variableMetaModel, secondEntity, firstValue));
        context.producesNoneOf(Moves.change(variableMetaModel, secondEntity, secondValue)); // No-op.
    }

    @Test
    void crossingNullDefaultTrueAssignsAndUnassigns() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        // generateSolution(2, 2): firstEntity=null, secondEntity=secondValue.
        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().getFirst();
        var secondValue = solution.getValueList().get(1);

        // Default constructor: crossingNull is true, because this variable allows unassigned values.
        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.change(variableMetaModel, secondEntity, firstValue), // Ordinary change, as before.
                Moves.change(variableMetaModel, firstEntity, firstValue), // Assign: firstEntity was unassigned.
                Moves.change(variableMetaModel, firstEntity, secondValue), // Assign: firstEntity was unassigned.
                Moves.change(variableMetaModel, secondEntity, null)); // Unassign: secondEntity was assigned.
        context.producesNoneOf(
                Moves.change(variableMetaModel, secondEntity, secondValue), // No-op.
                Moves.change(variableMetaModel, firstEntity, null)); // No-op: already null.
    }

    @Test
    void crossingNullUnassignMoveIsUndoable() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        // generateSolution(2, 2): firstEntity=null, secondEntity=secondValue.
        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var secondEntity = solution.getEntityList().get(1);
        var originalValue = secondEntity.getValue();
        assertThat(originalValue).isNotNull();

        // Default constructor: crossingNull is true, because this variable allows unassigned values.
        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var unassignMove = context.getMovesAsStream()
                .filter(move -> move.equals(Moves.change(variableMetaModel, secondEntity, null)))
                .findFirst()
                .orElseThrow();

        context.getMoveTestContext().executeTemporarily(unassignMove,
                view -> assertThat(secondEntity.getValue()).isNull());
        assertThat(secondEntity.getValue()).isEqualTo(originalValue);
    }

    @Test
    void crossingNullAssignMoveIsUndoable() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        // generateSolution(2, 2): firstEntity=null, secondEntity=secondValue.
        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var firstValue = solution.getValueList().getFirst();
        assertThat(firstEntity.getValue()).isNull();

        // Default constructor: crossingNull is true, because this variable allows unassigned values.
        var context = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var assignMove = context.getMovesAsStream()
                .filter(move -> move.equals(Moves.change(variableMetaModel, firstEntity, firstValue)))
                .findFirst()
                .orElseThrow();

        context.getMoveTestContext().executeTemporarily(assignMove,
                view -> assertThat(firstEntity.getValue()).isEqualTo(firstValue));
        assertThat(firstEntity.getValue()).isNull();
    }

    @Test
    void constructorRejectsExplicitCrossingNullOnNonUnassignedVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ChangeMoveProvider<>(variableMetaModel, true));
    }

}
