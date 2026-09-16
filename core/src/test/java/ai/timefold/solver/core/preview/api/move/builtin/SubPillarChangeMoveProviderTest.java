package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.move.builtin.MassChangeMove;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SubPillarChangeMoveProviderTest {

    @Test
    void subpillarMembersAreAlwaysASubsetOfTheFullPillar() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();

        var solution = TestdataSolution.generateSolution(2, 5);
        var entityList = solution.getEntityList();
        var sharedValue = solution.getValueList().getFirst();
        for (var entity : entityList) {
            entity.setValue(sharedValue); // All 5 entities share one value -> one pillar of size 5.
        }

        var context = NeighborhoodTester
                .build(new SubPillarChangeMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(move -> (MassChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move)
                .limit(200)
                .toList();
        var fullPillarMembers = new HashSet<>(entityList);
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getPlanningEntities())
                        .hasSizeLessThanOrEqualTo(2)
                        .allMatch(member -> fullPillarMembers.contains((TestdataEntity) member)));
    }

    @Test
    void differentDrawsProduceDifferentSubpillars() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();

        var solution = TestdataSolution.generateSolution(2, 5);
        var entityList = solution.getEntityList();
        var sharedValue = solution.getValueList().getFirst();
        for (var entity : entityList) {
            entity.setValue(sharedValue);
        }

        var context = NeighborhoodTester
                .build(new SubPillarChangeMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var distinctMemberSets = context
                .getMovesAsStream(move -> (MassChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move)
                .limit(200)
                .map(move -> new HashSet<>(move.getPlanningEntities()))
                .collect(Collectors.toCollection(HashSet::new));
        assertThat(distinctMemberSets).hasSizeGreaterThan(1);
    }

    @Test
    void pinnedEntityExcludedFromSubpillar() {
        var solutionMetaModel = TestdataPinnedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntity.class).basicVariable();

        var v0 = new TestdataValue("v0");
        var v1 = new TestdataValue("v1");
        // pinnedEntity shares v0 with free1 and free2,
        // but forEach(..., false) excludes pinned entities from the entity source,
        // so it must never join their subpillar, regardless of rule.
        var pinnedEntity = new TestdataPinnedEntity("pinned", v0, true);
        var free1 = new TestdataPinnedEntity("free1", v0, false);
        var free2 = new TestdataPinnedEntity("free2", v0, false);

        var solution = new TestdataPinnedSolution("s");
        solution.setValueList(List.of(v0, v1));
        solution.setEntityList(List.of(pinnedEntity, free1, free2));

        var context = NeighborhoodTester
                .build(new SubPillarChangeMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(move -> (MassChangeMove<TestdataPinnedSolution, TestdataPinnedEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .flatExtracting(MassChangeMove::getPlanningEntities)
                .doesNotContain(pinnedEntity);
    }

    @Test
    void crossingNullDefaultTrueAlsoUnassignsSubpillar() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(3, 6);
        var entityList = solution.getEntityList();
        var sharedValue = solution.getValueList().getFirst();
        for (var entity : entityList) {
            entity.setValue(sharedValue); // All entities share one value -> one pillar.
        }

        // Default constructor: crossingNull is true, because this variable allows unassigned values.
        var context = NeighborhoodTester
                .build(new SubPillarChangeMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(500)
                .toList();
        assertThat(moves).anyMatch(move -> move.getPlanningValues().getFirst() == null);
    }

    @Test
    void crossingNullFalseNeverUnassignsSubpillar() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(3, 6);
        var entityList = solution.getEntityList();
        var sharedValue = solution.getValueList().getFirst();
        for (var entity : entityList) {
            entity.setValue(sharedValue);
        }

        var context = NeighborhoodTester
                .build(new SubPillarChangeMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2)), false),
                        solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(200)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .noneMatch(move -> move.getPlanningValues().getFirst() == null);
    }

    @Test
    void constructorRejectsExplicitCrossingNullOnNonUnassignedVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> new SubPillarChangeMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2)), true));
    }

}
