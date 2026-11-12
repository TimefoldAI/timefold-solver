package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.entityrange.TestdataEntityRangeBaseEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.entityrange.TestdataEntityRangeChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.entityrange.TestdataEntityRangeSolution;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.pinned.TestdataPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.pinned.TestdataPinnedEntityProvidingSolution;
import ai.timefold.solver.core.testutil.MoveProviderAssertions;

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
    void endsWhenNoEntityHasAPartner() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class);
        // A single value: every entity has it, so every swap is a no-op.
        var solution = TestdataSolution.generateSolution(1, 3);

        var iterator = NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsIterator(move -> move);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void subclassOfInheritedVariableSwapsOnlyWithSubclass() {
        var solutionMetaModel = SolutionDescriptor.buildSolutionDescriptor(TestdataEntityRangeSolution.class,
                TestdataEntityRangeBaseEntity.class, TestdataEntityRangeChildEntity.class).getMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntityRangeChildEntity.class);
        var range = List.of("a", "b");
        var base = new TestdataEntityRangeBaseEntity(0, range);
        base.setValue("a");
        var child1 = new TestdataEntityRangeChildEntity(1, range);
        child1.setValue("b");
        var child2 = new TestdataEntityRangeChildEntity(2, range);
        child2.setValue("a");
        var solution = new TestdataEntityRangeSolution();
        solution.setEntityList(List.of(base, child1, child2));

        var iterator = NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsIterator(move -> move);
        for (var i = 0; i < 100; i++) {
            assertThat(iterator.next().getPlanningEntities()).containsExactlyInAnyOrder(child1, child2);
        }
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
    void uninitializedEntityNeverGivesNullToAVariableWhichDoesNotAllowIt() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class);
        var variableMetaModel = entityMetaModel.basicVariable();
        var solution = TestdataSolution.generateSolution(2, 3);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var e3 = solution.getEntityList().get(2);
        e1.setValue(null);

        var context = NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(Moves.swap(variableMetaModel, e2, e3));
        context.producesNoneOf(
                Moves.swap(variableMetaModel, e1, e2),
                Moves.swap(variableMetaModel, e2, e1),
                Moves.swap(variableMetaModel, e1, e3),
                Moves.swap(variableMetaModel, e3, e1));
    }

    @Test
    void singleVariableChecksTheLeftSideOfEveryCandidate() {
        var solutionMetaModel = TestdataEntityProvidingSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class);
        var variableMetaModel = entityMetaModel.basicVariable();
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var e1 = new TestdataEntityProvidingEntity("e1", List.of(v1, v2));
        e1.setValue(v1);
        // e2 accepts v1, so it is a candidate for e1; but e1 does not accept v3.
        var e2 = new TestdataEntityProvidingEntity("e2", List.of(v1, v3));
        e2.setValue(v3);
        var e3 = new TestdataEntityProvidingEntity("e3", List.of(v1, v2));
        e3.setValue(v2);
        var solution = new TestdataEntityProvidingSolution("s1");
        solution.setEntityList(List.of(e1, e2, e3));

        var context = NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.swap(variableMetaModel, e1, e3),
                Moves.swap(variableMetaModel, e3, e1));
        context.producesNoneOf(
                Moves.swap(variableMetaModel, e1, e2),
                Moves.swap(variableMetaModel, e2, e1));
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

    @Test
    void id() {
        var entityMetaModel = TestdataMultiVarSolution.buildMetaModel().genuineEntity(TestdataMultiVarEntity.class);
        List<PlanningVariableMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity, ?>> variableList =
                List.of(entityMetaModel.basicVariable("secondaryValue"), entityMetaModel.basicVariable("primaryValue"));
        var variableMetaModel = TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class)
                .basicVariable("value");
        assertThat(new SwapMoveProvider<>(variableList).getId())
                .isEqualTo("SwapMoveProvider(TestdataMultiVarEntity.primaryValue,secondaryValue)");
        assertThat(new SwapMoveProvider<>(variableMetaModel).getId())
                .isEqualTo("SwapMoveProvider(TestdataEntity.value)");
    }

    @Test
    void entityRangeWithPinnedEntityDrawsOnlyValidMovablePartners() {
        var solutionMetaModel = TestdataPinnedEntityProvidingSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntityProvidingEntity.class);
        var variableMetaModel = entityMetaModel.basicVariable();
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var pinned = new TestdataPinnedEntityProvidingEntity("pinned", List.of(v1, v2), v1, true);
        var e2 = new TestdataPinnedEntityProvidingEntity("e2", List.of(v1, v2), v2, false);
        var e3 = new TestdataPinnedEntityProvidingEntity("e3", List.of(v1, v2, v3), v1, false);
        var e4 = new TestdataPinnedEntityProvidingEntity("e4", List.of(v3), v3, false);
        var solution = new TestdataPinnedEntityProvidingSolution("s", List.of(pinned, e2, e3, e4));

        // Only e2 <-> e3 is valid; e4 cannot take v1 or v2, and the pinned entity must never move.
        var context = NeighborhoodTester.build(new SwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        var validMoves = List.of(Moves.swap(variableMetaModel, e2, e3), Moves.swap(variableMetaModel, e3, e2));
        context.producesAllOf(validMoves.toArray(Move[]::new));
        assertThat(context.getMovesAsStream().limit(1_000).toList())
                .hasSize(1_000)
                .allSatisfy(move -> assertThat(validMoves).contains(move));
    }

    @Test
    void everyDrawnMoveKeepsTheSolutionValid() {
        var solutionMetaModel = TestdataPinnedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntityProvidingEntity.class).basicVariable();
        var solution = MoveProviderAssertions.pinnedEntityRangeSolution(4, 12, 3);
        var context = NeighborhoodTester.build(new SwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        MoveProviderAssertions.assertDrawnMovesKeepSolutionValid(context, List.of(variableMetaModel),
                solution.getEntityList(), 2_000);
    }

    @Test
    void everyValidPartnerIsDrawnEquallyOften() {
        var solutionMetaModel = TestdataPinnedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntityProvidingEntity.class).basicVariable();
        var solution = MoveProviderAssertions.pinnedEntityRangeSolution(4, 12, 3);
        var left = solution.getEntityList().get(1);
        var expectedPartners = solution.getEntityList().stream()
                .filter(right -> right != left && !right.isPinned() && right.getValue() != left.getValue()
                        && right.getValueRange().contains(left.getValue())
                        && left.getValueRange().contains(right.getValue()))
                .toList();
        var context = NeighborhoodTester.build(new SwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var movesOfLeft = context.getMovesAsStream()
                .limit(40_000)
                .filter(move -> move.getPlanningEntities().getFirst() == left)
                .toList();
        assertThat(expectedPartners).hasSizeGreaterThan(1);
        MoveProviderAssertions.assertEquallyOften(movesOfLeft, move -> move.getPlanningEntities().getLast(),
                expectedPartners, 0.3);
    }

}
