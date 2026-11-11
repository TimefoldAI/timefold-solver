package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testutil.MoveProviderAssertions;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class AssignMoveProviderTest {

    @Test
    void constructorRejectsNonUnassignedVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssignMoveProvider<>(variableMetaModel));
    }

    @Test
    void pinnedEntitySkipped() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataPinnedAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        firstEntity.setPinned(true);
        firstEntity.setValue(null);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);

        // firstEntity is pinned -> no assign moves. Only secondEntity gets assignments.
        var context = NeighborhoodTester.build(new AssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.change(variableMetaModel, secondEntity, firstValue),
                Moves.change(variableMetaModel, secondEntity, secondValue));
        context.producesNoneOf(
                Moves.change(variableMetaModel, firstEntity, firstValue),
                Moves.change(variableMetaModel, firstEntity, secondValue));
    }

    @Test
    void fromEntity() {
        var solutionMetaModel = TestdataAllowsUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .basicVariable();

        // entity1: range={v1,v2}, null; entity2: range={v1,v3}, null.
        var solution = TestdataAllowsUnassignedEntityProvidingSolution.generateSolution();
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var v1 = firstEntity.getValueRange().get(0);
        var v2 = firstEntity.getValueRange().get(1);
        var v3 = secondEntity.getValueRange().get(1);

        // Both entities start null; only values in each entity's range are offered.
        var context = NeighborhoodTester.build(new AssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.change(variableMetaModel, firstEntity, v1),
                Moves.change(variableMetaModel, firstEntity, v2),
                Moves.change(variableMetaModel, secondEntity, v1),
                Moves.change(variableMetaModel, secondEntity, v3));
        context.producesNoneOf(
                Moves.change(variableMetaModel, firstEntity, v3), // Not in firstEntity's range.
                Moves.change(variableMetaModel, secondEntity, v2)); // Not in secondEntity's range.
    }

    @Test
    void fromSolution() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValue(null);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);

        NeighborhoodTester.build(new AssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesAllOf(
                        Moves.change(variableMetaModel, firstEntity, firstValue),
                        Moves.change(variableMetaModel, firstEntity, secondValue),
                        Moves.change(variableMetaModel, secondEntity, firstValue),
                        Moves.change(variableMetaModel, secondEntity, secondValue));
    }

    @Test
    void id() {
        var unassignedVariableMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel()
                .genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable("value");
        assertThat(new AssignMoveProvider<>(unassignedVariableMetaModel).getId())
                .isEqualTo("AssignMoveProvider(TestdataAllowsUnassignedEntity.value)");
    }

    @Test
    void fromEntityNeverAssignsNull() {
        var solutionMetaModel = TestdataAllowsUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .basicVariable();
        var solution = TestdataAllowsUnassignedEntityProvidingSolution.generateSolution();

        // The entity's range holds null as an extra element, as the variable allows unassigned values.
        var context = NeighborhoodTester.build(new AssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        assertThat(context.getMovesAsStream().limit(1_000).toList())
                .hasSize(1_000)
                .allSatisfy(move -> assertThat(move.getPlanningValues()).doesNotContainNull());
    }

    @Test
    void everyDrawnMoveKeepsTheSolutionValid() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedEntity.class).basicVariable();
        var solution = TestdataPinnedAllowsUnassignedSolution.generateSolution(3, 8);
        var entityList = solution.getEntityList();
        for (var i = 0; i < entityList.size(); i++) {
            if (i % 2 == 0) {
                entityList.get(i).setValue(null);
            }
            entityList.get(i).setPinned(i % 4 == 0);
        }
        var context = NeighborhoodTester.build(new AssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        MoveProviderAssertions.assertDrawnMovesKeepSolutionValid(context, List.of(variableMetaModel), entityList, 2_000);
    }

    @Test
    void everyValidValueIsDrawnEquallyOften() {
        var solutionMetaModel = TestdataAllowsUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .basicVariable();
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var entity = new TestdataAllowsUnassignedEntityProvidingEntity("e1", List.of(v1, v2, v3));
        var other = new TestdataAllowsUnassignedEntityProvidingEntity("e2", List.of(v1));
        var assigned = new TestdataAllowsUnassignedEntityProvidingEntity("e3", List.of(v2, v3), v2);
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s");
        solution.setEntityList(List.of(entity, other, assigned));
        var context = NeighborhoodTester.build(new AssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var moves = MoveProviderAssertions.assertDrawnMovesKeepSolutionValid(context, List.of(variableMetaModel),
                solution.getEntityList(), 20_000);
        var movesOfEntity = moves.stream().filter(move -> move.getPlanningEntities().getFirst() == entity).toList();
        MoveProviderAssertions.assertEquallyOften(movesOfEntity, move -> move.getPlanningValues().getFirst(),
                List.of(v1, v2, v3), 0.3);
    }

}
