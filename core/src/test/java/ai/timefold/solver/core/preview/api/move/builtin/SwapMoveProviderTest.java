package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SwapMoveProviderTest {

    @Test
    void univariate() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class);
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();

        var solution = TestdataSolution.generateSolution(2, 3);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var e3 = solution.getEntityList().get(2);

        // With 3 entities, only 3 swap moves are possible: e1 <-> e2, e1 <-> e3, e2 <-> e3.
        // But we only have 2 values, guaranteeing that two entities (e1 and e3) share a value,
        // making that swap a no-op. Each remaining pair is produced in both directions,
        // as swap(a, b) and swap(b, a) are distinct moves.
        var context = NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.swap(variableMetaModel, e1, e2),
                Moves.swap(variableMetaModel, e2, e1),
                Moves.swap(variableMetaModel, e2, e3),
                Moves.swap(variableMetaModel, e3, e2));
        context.producesNoneOf(
                Moves.swap(variableMetaModel, e1, e3), // No-op: e1 and e3 share a value.
                Moves.swap(variableMetaModel, e3, e1));
    }

    @Test
    void multivariate() {
        var solutionMetaModel = TestdataMultiVarSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataMultiVarEntity.class);
        var variableMetaModelList = allVariables(entityMetaModel);

        var solution = TestdataMultiVarSolution.generateSolution(3, 2, 2);
        var e1 = solution.getMultiVarEntityList().get(0);
        var e2 = solution.getMultiVarEntityList().get(1);
        var e3 = solution.getMultiVarEntityList().get(2);

        // With 3 entities, only 3 swap moves are possible: e1 <-> e2, e1 <-> e3, e2 <-> e3.
        // But we only have 2 unique combinations of values, guaranteeing that two entities (e1 and e3) share values,
        // making that swap a no-op. Each remaining pair is produced in both directions.
        var context = NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.swap(variableMetaModelList, e1, e2),
                Moves.swap(variableMetaModelList, e2, e1),
                Moves.swap(variableMetaModelList, e2, e3),
                Moves.swap(variableMetaModelList, e3, e2));
        context.producesNoneOf(
                Moves.swap(variableMetaModelList, e1, e3), // No-op: e1 and e3 share values.
                Moves.swap(variableMetaModelList, e3, e1));
    }

    @Test
    void pinnedEntitySkipped() {
        var solutionMetaModel = TestdataPinnedSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntity.class);
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntity.class).basicVariable();

        var solution = TestdataPinnedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        firstEntity.setPinned(true);

        // With only 2 entities and one pinned, there is no valid swap partner.
        NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution)
                .producesNoneOf(
                        Moves.swap(variableMetaModel, firstEntity, secondEntity),
                        Moves.swap(variableMetaModel, secondEntity, firstEntity));
    }

    @Test
    void fromEntity() {
        var solutionMetaModel = TestdataEntityProvidingSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class);
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class).basicVariable();

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var e1 = new TestdataEntityProvidingEntity("e1", List.of(v1, v2));
        e1.setValue(v1);
        var e2 = new TestdataEntityProvidingEntity("e2", List.of(v1, v2));
        e2.setValue(v2);
        var e3 = new TestdataEntityProvidingEntity("e3", List.of(v3));
        e3.setValue(v3);
        var solution = new TestdataEntityProvidingSolution("s1");
        solution.setEntityList(List.of(e1, e2, e3));

        // e1(v1, range={v1,v2}) <-> e2(v2, range={v1,v2}): valid swap, produced in both directions.
        // e1 <-> e3, e2 <-> e3: v3 not in e1/e2's range, and v1/v2 not in e3's range -> excluded.
        var context = NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.swap(variableMetaModel, e1, e2),
                Moves.swap(variableMetaModel, e2, e1));
        context.producesNoneOf(
                Moves.swap(variableMetaModel, e1, e3),
                Moves.swap(variableMetaModel, e3, e1),
                Moves.swap(variableMetaModel, e2, e3),
                Moves.swap(variableMetaModel, e3, e2));
    }

    @Test
    void multivariateWithExclusions() {
        var solutionMetaModel = TestdataMultiVarSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.entity(TestdataMultiVarEntity.class);
        var allowedVariableMetaModels = entityMetaModel.variables().stream()
                .filter(v -> !v.name().contains("tertiary"))
                .map(v -> (PlanningVariableMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity, ?>) v)
                .toList();
        var solution = TestdataMultiVarSolution.generateSolution(3, 1, 2);
        var e1 = solution.getMultiVarEntityList().get(0);
        var e2 = solution.getMultiVarEntityList().get(1);
        var e3 = solution.getMultiVarEntityList().get(2);

        // With 3 entities, only 3 swap moves are possible: e1 <-> e2, e1 <-> e3, e2 <-> e3.
        // We only have 1 value for primary and secondary variables,
        // therefore with the tertiary variable excluded, there will be no swap moves, in either direction.
        NeighborhoodTester.build(new SwapMoveProvider<>(allowedVariableMetaModels), solutionMetaModel)
                .using(solution)
                .producesNoneOf(
                        Moves.swap(allowedVariableMetaModels, e1, e2),
                        Moves.swap(allowedVariableMetaModels, e2, e1),
                        Moves.swap(allowedVariableMetaModels, e1, e3),
                        Moves.swap(allowedVariableMetaModels, e3, e1),
                        Moves.swap(allowedVariableMetaModels, e2, e3),
                        Moves.swap(allowedVariableMetaModels, e3, e2));
    }

    @SuppressWarnings("unchecked")
    private static List<PlanningVariableMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity, Object>> allVariables(
            PlanningEntityMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity> entityMetaModel) {
        return entityMetaModel.variables().stream()
                .map(v -> (PlanningVariableMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity, Object>) v)
                .toList();
    }

    @Test
    void mixedLegalityAcrossVariablesRejectsWholePair() {
        var solutionMetaModel = TestdataAllowsUnassignedMultiVarEntityProvidingSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedMultiVarEntityProvidingEntity.class);
        @SuppressWarnings("unchecked")
        var variableMetaModelList = entityMetaModel.variables().stream()
                .map(v -> (PlanningVariableMetaModel<TestdataAllowsUnassignedMultiVarEntityProvidingSolution, TestdataAllowsUnassignedMultiVarEntityProvidingEntity, Object>) v)
                .toList();

        var v1 = new TestdataValue("v1");
        var v3 = new TestdataValue("v3");
        var v4 = new TestdataValue("v4");

        // e1 and e3 differ on both "value" and "secondValue".
        // The "value" swap is legal in both directions (v1 and v4 are in both ranges),
        // but the "secondValue" swap is not: e1's secondValueRange does not contain e3's v3.
        // One legal variable is not enough to save the pair;
        // the whole swap is rejected.
        var e1 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("e1", List.of(v1, v4), List.of(v1, v4));
        e1.setValue(v1);
        e1.setSecondValue(v1);
        var e3 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("e3", List.of(v1, v4), List.of(v1, v3, v4));
        e3.setValue(v4);
        e3.setSecondValue(v3);

        // The third variable is solution-scoped and both entities leave it null,
        // so it never differs and never decides the outcome.
        var solution = new TestdataAllowsUnassignedMultiVarEntityProvidingSolution("s", List.of(v1));
        solution.setEntityList(List.of(e1, e3));

        NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution)
                .producesNoneOf(
                        Moves.swap(variableMetaModelList, e1, e3),
                        Moves.swap(variableMetaModelList, e3, e1));
    }

    @Test
    void singleVariableConstructorMatchesOneElementList() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();

        var solution = TestdataSolution.generateSolution(2, 3);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);

        var expectedMove = Moves.swap(variableMetaModel, e1, e2);

        NeighborhoodTester.build(new SwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesAllOf(expectedMove);
        NeighborhoodTester.build(new SwapMoveProvider<>(List.of(variableMetaModel)), solutionMetaModel)
                .using(solution)
                .producesAllOf(expectedMove);
    }

    @Test
    void emptyListConstructorThrows() {
        assertThatThrownBy(() -> new SwapMoveProvider<TestdataSolution, TestdataEntity>(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is empty");
    }

}
