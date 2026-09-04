package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.move.builtin.PillarSwapMove;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.multivar.TestdataOtherValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SubPillarSwapMoveProviderTest {

    @Test
    void subpillarMembersAreAlwaysASubsetOfTheFullPillar() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class);

        // 5 entities on value0 (pillar A, size 5), 5 on value1 (pillar B, size 5).
        var solution = TestdataSolution.generateSolution(2, 10);
        var entityList = solution.getEntityList();
        var valueList = solution.getValueList();
        for (var i = 0; i < 5; i++) {
            entityList.get(i).setValue(valueList.getFirst());
        }
        for (var i = 5; i < 10; i++) {
            entityList.get(i).setValue(valueList.get(1));
        }
        var pillarA = new HashSet<>(entityList.subList(0, 5));
        var pillarB = new HashSet<>(entityList.subList(5, 10));

        var context = NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(entityMetaModel, Samplers.pillar(Samplers.exactly(2)),
                        Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(move -> (PillarSwapMove<TestdataSolution, TestdataEntity>) move)
                .limit(200)
                .toList();
        assertThat(moves).isNotEmpty();
        assertThat(moves).allSatisfy(move -> {
            assertThat(move.getPlanningEntities())
                    .hasSizeLessThanOrEqualTo(4) // At most 2 per side.
                    .allMatch(member -> pillarA.contains(member) || pillarB.contains(member));
        });
    }

    @Test
    void differentDrawsProduceDifferentSubpillars() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class);

        var solution = TestdataSolution.generateSolution(2, 10);
        var entityList = solution.getEntityList();
        var valueList = solution.getValueList();
        for (var i = 0; i < 5; i++) {
            entityList.get(i).setValue(valueList.getFirst());
        }
        for (var i = 5; i < 10; i++) {
            entityList.get(i).setValue(valueList.get(1));
        }

        var context = NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(entityMetaModel, Samplers.pillar(Samplers.exactly(2)),
                        Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var distinctMemberSets = context.getMovesAsStream(move -> (PillarSwapMove<TestdataSolution, TestdataEntity>) move)
                .limit(200)
                .map(move -> new HashSet<>(move.getPlanningEntities()))
                .collect(Collectors.toCollection(HashSet::new));
        assertThat(distinctMemberSets).hasSizeGreaterThan(1);
    }

    @Test
    void sharedSamplerInstanceOnBothSidesMatchesTwoSeparateInstances() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class);

        var solution = TestdataSolution.generateSolution(2, 10);
        var entityList = solution.getEntityList();
        var valueList = solution.getValueList();
        for (var i = 0; i < 5; i++) {
            entityList.get(i).setValue(valueList.getFirst());
        }
        for (var i = 5; i < 10; i++) {
            entityList.get(i).setValue(valueList.get(1));
        }

        // Two separate, independently-stateful sampler instances -
        // the ordinary way to call this constructor.
        var separateContext = NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(entityMetaModel, Samplers.pillar(Samplers.between(2, 4)),
                        Samplers.pillar(Samplers.between(2, 4))),
                        solutionMetaModel)
                .using(solution);
        var movesWithSeparateSamplers =
                separateContext.getMovesAsStream(move -> (PillarSwapMove<TestdataSolution, TestdataEntity>) move)
                        .limit(200)
                        .toList();

        // One stateful sampler instance shared by both sides. Safe per Sampler's class documentation:
        // reset() runs before every sample and a sample is assembled in full before it is returned,
        // so sharing does not change which moves are produced -
        // NeighborhoodTester always reseeds the working random the same way,
        // so the two runs are directly comparable.
        var sharedSampler = Samplers.<List<Object>, TestdataEntity> pillar(Samplers.between(2, 4));
        var sharedContext = NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(entityMetaModel, sharedSampler, sharedSampler), solutionMetaModel)
                .using(solution);
        var movesWithSharedSampler =
                sharedContext.getMovesAsStream(move -> (PillarSwapMove<TestdataSolution, TestdataEntity>) move)
                        .limit(200)
                        .toList();

        assertThat(movesWithSharedSampler).isEqualTo(movesWithSeparateSamplers);
    }

    @Test
    void bothSidesRespectTheirOwnRule() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class);

        var solution = TestdataSolution.generateSolution(2, 10);
        var entityList = solution.getEntityList();
        var valueList = solution.getValueList();
        for (var i = 0; i < 5; i++) {
            entityList.get(i).setValue(valueList.getFirst());
        }
        for (var i = 5; i < 10; i++) {
            entityList.get(i).setValue(valueList.get(1));
        }

        var context = NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(entityMetaModel, Samplers.pillar(Samplers.exactly(1)),
                        Samplers.pillar(Samplers.exactly(3))),
                        solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(move -> (PillarSwapMove<TestdataSolution, TestdataEntity>) move)
                .limit(200)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getPlanningEntities()).hasSizeLessThanOrEqualTo(1 + 3));
    }

    @Test
    void pinnedEntityNeverParticipatesInSubpillarSwap() {
        var solutionMetaModel = TestdataPinnedSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntity.class);

        var v0 = new TestdataValue("v0");
        var v1 = new TestdataValue("v1");
        var pinnedEntity = new TestdataPinnedEntity("pinned", v0, true);
        // Two free members on v0, not one:
        // pinnedEntity is excluded from the entity source (as asserted below),
        // so the v0 group's own minimum size for Samplers.exactly(2) must be met by free entities alone.
        var free1 = new TestdataPinnedEntity("free1", v0, false);
        var free1b = new TestdataPinnedEntity("free1b", v0, false);
        var free2 = new TestdataPinnedEntity("free2", v1, false);
        var free3 = new TestdataPinnedEntity("free3", v1, false);

        var solution = new TestdataPinnedSolution("s");
        solution.setValueList(List.of(v0, v1));
        solution.setEntityList(List.of(pinnedEntity, free1, free1b, free2, free3));

        var context = NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(entityMetaModel, Samplers.pillar(Samplers.exactly(2)),
                        Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
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
    void multivariateSwapUsesEveryListedVariable() {
        var solutionMetaModel = TestdataMultiVarSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.entity(TestdataMultiVarEntity.class);
        var variableMetaModelList = entityMetaModel.variables().stream()
                .map(v -> (PlanningVariableMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity, Object>) v)
                .toList();

        var value0 = new TestdataValue("v0");
        var value1 = new TestdataValue("v1");
        var otherValue0 = new TestdataOtherValue("o0");

        // Pillar A and pillar B agree on primary and tertiary, differing only on secondary.
        // Both groups have exactly 2 members,
        // so a Samplers.exactly(2) sampler always draws the whole group, making the subpillar swap deterministic -
        // same setup as PillarSwapMoveProviderTest.atLeastOneDifferingVariableProducesMove.
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

        NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(variableMetaModelList, Samplers.pillar(Samplers.exactly(2)),
                        Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution)
                .producesAllOf(
                        Moves.pillarSwap(variableMetaModelList, pillarA, pillarB),
                        Moves.pillarSwap(variableMetaModelList, pillarB, pillarA));
    }

    @Test
    void excludedVariableIsIgnoredForKeyAndSwap() {
        var solutionMetaModel = TestdataMultiVarSolution.buildMetaModel();
        var entityMetaModel = solutionMetaModel.entity(TestdataMultiVarEntity.class);
        var allowedVariableMetaModelList = entityMetaModel.variables().stream()
                .filter(v -> !v.name().contains("secondary"))
                .map(v -> (PlanningVariableMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity, Object>) v)
                .toList();

        var value0 = new TestdataValue("v0");
        var value1 = new TestdataValue("v1");
        var otherValue0 = new TestdataOtherValue("o0");

        // Same setup as multivariateSwapUsesEveryListedVariable.
        // Excluding secondary from the key merges A and B into one pillar (all four share primary and tertiary),
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
                .build(new SubPillarSwapMoveProvider<>(allowedVariableMetaModelList, Samplers.pillar(Samplers.exactly(2)),
                        Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution)
                .getMovesAsStream()
                .limit(50)
                .toList();
        assertThat(moveList).isEmpty();
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

        // Same setup as PillarSwapMoveProviderTest.mixedLegalityAcrossVariablesRejectsWholePair,
        // drawn through a size-1 sampler on each side so the subpillar equals the whole pillar.
        var a1 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("a1", List.of(v1, v4), List.of(v1, v4));
        a1.setValue(v1);
        a1.setSecondValue(v1);
        var b1 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("b1", List.of(v1, v4), List.of(v1, v3, v4));
        b1.setValue(v4);
        b1.setSecondValue(v3);

        var solution = new TestdataAllowsUnassignedMultiVarEntityProvidingSolution("s", List.of(v1));
        solution.setEntityList(List.of(a1, b1));

        var moveList = NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(variableMetaModelList, Samplers.pillar(Samplers.exactly(1)),
                        Samplers.pillar(Samplers.exactly(1))),
                        solutionMetaModel)
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

        NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2)),
                        Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution)
                .producesAllOf(expectedMoveAtoB, expectedMoveBtoA);
        NeighborhoodTester
                .build(new SubPillarSwapMoveProvider<>(List.of(variableMetaModel), Samplers.pillar(Samplers.exactly(2)),
                        Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution)
                .producesAllOf(expectedMoveAtoB, expectedMoveBtoA);
    }

    @Test
    void emptyListConstructorThrows() {
        assertThatThrownBy(() -> new SubPillarSwapMoveProvider<TestdataSolution, TestdataEntity>(List.of(),
                Samplers.pillar(Samplers.exactly(2)), Samplers.pillar(Samplers.exactly(2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is empty");
    }

}
