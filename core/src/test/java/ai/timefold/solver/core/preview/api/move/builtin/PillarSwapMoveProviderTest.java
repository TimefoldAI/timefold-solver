package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.impl.move.builtin.PillarSwapMove;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.multivar.TestdataOtherValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class PillarSwapMoveProviderTest {

    @Test
    void equalValuesProduceNoMove() {
        var solution = TestdataSolution.generateSolution(1, 4); // Every entity on the same value.
        var moveList = NeighborhoodTester
                .build(new PillarSwapMoveProvider<>(TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class)),
                        TestdataSolution.buildMetaModel())
                .using(solution)
                .getMovesAsStream()
                .limit(50)
                .toList();
        // One value means one pillar;
        // no partner can differ,
        // so the only key retires and the iterator ends.
        // This also doubles as the termination test.
        assertThat(moveList).isEmpty();
    }

    @Test
    void atLeastOneDifferingVariableProducesMove() {
        var solutionMetaModel = TestdataMultiVarSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataMultiVarEntity.class);
        var variableMetaModelList = allVariables(entityMetaModel);

        var value0 = new TestdataValue("v0");
        var value1 = new TestdataValue("v1");
        var otherValue0 = new TestdataOtherValue("o0");

        // Pillar A and pillar B agree on primary and tertiary, differing only on secondary.
        var a1 = new TestdataMultiVarEntity("a1", value0, value0, otherValue0);
        var a2 = new TestdataMultiVarEntity("a2", value0, value0, otherValue0);
        var b1 = new TestdataMultiVarEntity("b1", value0, value1, otherValue0);
        var b2 = new TestdataMultiVarEntity("b2", value0, value1, otherValue0);

        var solution = new TestdataMultiVarSolution("s");
        solution.setValueList(List.of(value0, value1));
        solution.setOtherValueList(List.of(otherValue0));
        solution.setMultiVarEntityList(List.of(a1, a2, b1, b2));

        var pillarA = Sample.of(List.of(a1, a2));
        var pillarB = Sample.of(List.of(b1, b2));

        var context = NeighborhoodTester.build(new PillarSwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        // Mirrored pairs are both legal and both kept, as SwapMoveProvider keeps them.
        context.producesAllOf(
                Moves.pillarSwap(variableMetaModelList, pillarA, pillarB),
                Moves.pillarSwap(variableMetaModelList, pillarB, pillarA));
    }

    @Test
    void outOfRangeCrossValueRejectsWholePair() {
        var solutionMetaModel = TestdataEntityProvidingSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class);
        var variableMetaModelList = allVariables(entityMetaModel);

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");

        // Pillar A: both members accept either value.
        var a1 = new TestdataEntityProvidingEntity("a1", List.of(value1, value2));
        a1.setValue(value1);
        var a2 = new TestdataEntityProvidingEntity("a2", List.of(value1, value2));
        a2.setValue(value1);
        // Pillar B: both members are restricted to value2 only,
        // so accepting value1 (pillar A's current value) is out of range for every member.
        var b1 = new TestdataEntityProvidingEntity("b1", List.of(value2));
        b1.setValue(value2);
        var b2 = new TestdataEntityProvidingEntity("b2", List.of(value2));
        b2.setValue(value2);

        var solution = new TestdataEntityProvidingSolution("s");
        solution.setEntityList(List.of(a1, a2, b1, b2));

        var pillarA = Sample.of(List.of(a1, a2));
        var pillarB = Sample.of(List.of(b1, b2));

        var context = NeighborhoodTester.build(new PillarSwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        // Pillar A could accept value2,
        // but pillar B cannot accept value1:
        // the whole pair is rejected, in either direction.
        context.producesNoneOf(
                Moves.pillarSwap(variableMetaModelList, pillarA, pillarB),
                Moves.pillarSwap(variableMetaModelList, pillarB, pillarA));
    }

    @Test
    void mixedLegalityAcrossVariablesRejectsWholePair() {
        var solutionMetaModel = TestdataAllowsUnassignedMultiVarEntityProvidingSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedMultiVarEntityProvidingEntity.class);
        var variableMetaModelList = allVariables(entityMetaModel);

        var v1 = new TestdataValue("v1");
        var v3 = new TestdataValue("v3");
        var v4 = new TestdataValue("v4");

        // Pillar A and pillar B differ on both "value" and "secondValue".
        // The "value" swap is legal in both directions,
        // but the "secondValue" swap is not:
        // pillar A's secondValueRange does not contain pillar B's v3.
        // One legal variable is not enough to save the pair;
        // the whole swap is rejected.
        var a1 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("a1", List.of(v1, v4), List.of(v1, v4));
        a1.setValue(v1);
        a1.setSecondValue(v1);
        var b1 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("b1", List.of(v1, v4), List.of(v1, v3, v4));
        b1.setValue(v4);
        b1.setSecondValue(v3);

        // The third variable is solution-scoped
        // and both entities leave it null,
        // so it never differs and never decides the outcome.
        var solution = new TestdataAllowsUnassignedMultiVarEntityProvidingSolution("s", List.of(v1));
        solution.setEntityList(List.of(a1, b1));

        var pillarA = Sample.of(List.of(a1));
        var pillarB = Sample.of(List.of(b1));

        var context = NeighborhoodTester.build(new PillarSwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        context.producesNoneOf(
                Moves.pillarSwap(variableMetaModelList, pillarA, pillarB),
                Moves.pillarSwap(variableMetaModelList, pillarB, pillarA));
    }

    @Test
    void pinnedEntityNeverParticipatesInSwap() {
        var solutionMetaModel = TestdataPinnedSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntity.class);

        var v0 = new TestdataValue("v0");
        var v1 = new TestdataValue("v1");
        // pinnedEntity shares v0 with free1, forming what would be pillar A;
        // forEach(..., false) excludes pinned entities from the entity source,
        // so it must never appear in a swap.
        var pinnedEntity = new TestdataPinnedEntity("pinned", v0, true);
        var free1 = new TestdataPinnedEntity("free1", v0, false);
        var free2 = new TestdataPinnedEntity("free2", v1, false);
        var free3 = new TestdataPinnedEntity("free3", v1, false);

        var solution = new TestdataPinnedSolution("s");
        solution.setValueList(List.of(v0, v1));
        solution.setEntityList(List.of(pinnedEntity, free1, free2, free3));

        var context = NeighborhoodTester.build(new PillarSwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(move -> (PillarSwapMove<TestdataPinnedSolution, TestdataPinnedEntity>) move)
                .limit(100)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .flatExtracting(PillarSwapMove::getPlanningEntities)
                .doesNotContain(pinnedEntity);
    }

    @Test
    void threeAgainstFiveSwapsEveryMember() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class);
        var variableMetaModelList = allVariables(entityMetaModel);

        var solution = TestdataSolution.generateSolution(2, 8);
        var entityList = solution.getEntityList();
        var valueList = solution.getValueList();
        // First three entities share value0 (pillar A, size 3);
        // the remaining five share value1 (pillar B, size 5).
        // The legal-swap fraction stays high:
        // the only two keys in the pool both produce a legal swap in either direction.
        for (var i = 0; i < 3; i++) {
            entityList.get(i).setValue(valueList.getFirst());
        }
        for (var i = 3; i < 8; i++) {
            entityList.get(i).setValue(valueList.get(1));
        }
        var pillarA = Sample.of(List.copyOf(entityList.subList(0, 3)));
        var pillarB = Sample.of(List.copyOf(entityList.subList(3, 8)));

        var context = NeighborhoodTester.build(new PillarSwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.pillarSwap(variableMetaModelList, pillarA, pillarB),
                Moves.pillarSwap(variableMetaModelList, pillarB, pillarA));
    }

    @Test
    void sizeOnePillarsAreGenerated() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class);

        // Every entity gets its own distinct value, so every pillar has exactly one member.
        var solution = TestdataSolution.generateSolution(4, 4);

        var context = NeighborhoodTester.build(new PillarSwapMoveProvider<>(entityMetaModel), solutionMetaModel)
                .using(solution);
        var moveList = context.getMovesAsStream(move -> (PillarSwapMove<TestdataSolution, TestdataEntity>) move)
                .limit(100)
                .toList();
        assertThat(moveList).isNotEmpty();
        // Unlike PillarChangeMoveProvider, size-1 pillars are legal on both sides of a swap.
        assertThat(moveList).allSatisfy(move -> assertThat(move.getPlanningEntities()).hasSize(2));
    }

    @Test
    void excludedVariableIsIgnoredForKeyAndSwap() {
        var solutionMetaModel = TestdataMultiVarSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataMultiVarEntity.class);
        var allowedVariableMetaModelList = allVariables(entityMetaModel).stream()
                .filter(v -> !v.name().contains("secondary"))
                .toList();

        var value0 = new TestdataValue("v0");
        var value1 = new TestdataValue("v1");
        var otherValue0 = new TestdataOtherValue("o0");

        // Same setup as atLeastOneDifferingVariableProducesMove:
        // with the full variable list, A and B are distinct pillars (they differ on secondary).
        // Excluding secondary from the key merges them into one pillar (all four share primary and tertiary),
        // leaving no second distinct key to swap with.
        var a1 = new TestdataMultiVarEntity("a1", value0, value0, otherValue0);
        var a2 = new TestdataMultiVarEntity("a2", value0, value0, otherValue0);
        var b1 = new TestdataMultiVarEntity("b1", value0, value1, otherValue0);
        var b2 = new TestdataMultiVarEntity("b2", value0, value1, otherValue0);

        var solution = new TestdataMultiVarSolution("s");
        solution.setValueList(List.of(value0, value1));
        solution.setOtherValueList(List.of(otherValue0));
        solution.setMultiVarEntityList(List.of(a1, a2, b1, b2));

        var moveList = NeighborhoodTester
                .build(new PillarSwapMoveProvider<>(allowedVariableMetaModelList), solutionMetaModel)
                .using(solution)
                .getMovesAsStream()
                .limit(50)
                .toList();
        assertThat(moveList).isEmpty();
    }

    @Test
    void singleVariableConstructorMatchesOneElementList() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();

        var solution = TestdataSolution.generateSolution(2, 4);
        var entityList = solution.getEntityList();
        var valueList = solution.getValueList();
        entityList.get(0).setValue(valueList.get(0));
        entityList.get(1).setValue(valueList.get(0));
        entityList.get(2).setValue(valueList.get(1));
        entityList.get(3).setValue(valueList.get(1));
        var pillarA = Sample.of(List.of(entityList.get(0), entityList.get(1)));
        var pillarB = Sample.of(List.of(entityList.get(2), entityList.get(3)));

        var expectedMoveAtoB = Moves.pillarSwap(variableMetaModel, pillarA, pillarB);
        var expectedMoveBtoA = Moves.pillarSwap(variableMetaModel, pillarB, pillarA);

        NeighborhoodTester.build(new PillarSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesAllOf(expectedMoveAtoB, expectedMoveBtoA);
        NeighborhoodTester.build(new PillarSwapMoveProvider<>(List.of(variableMetaModel)), solutionMetaModel)
                .using(solution)
                .producesAllOf(expectedMoveAtoB, expectedMoveBtoA);
    }

    @Test
    void emptyListConstructorThrows() {
        assertThatThrownBy(() -> new PillarSwapMoveProvider<TestdataSolution, TestdataEntity>(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is empty");
    }

    @SuppressWarnings("unchecked")
    private static <Solution_, Entity_> List<PlanningVariableMetaModel<Solution_, Entity_, Object>> allVariables(
            PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel) {
        return entityMetaModel.variables().stream()
                .map(v -> (PlanningVariableMetaModel<Solution_, Entity_, Object>) v)
                .toList();
    }

}
