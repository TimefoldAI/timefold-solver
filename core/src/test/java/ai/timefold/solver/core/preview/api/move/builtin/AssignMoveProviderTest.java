package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;

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

}
