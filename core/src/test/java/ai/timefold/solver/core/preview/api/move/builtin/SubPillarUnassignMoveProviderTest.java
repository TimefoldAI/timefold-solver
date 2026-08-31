package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SubPillarUnassignMoveProviderTest {

    @Test
    void subpillarMembersAreAlwaysASubsetOfTheFullPillar() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 6);
        var entityList = solution.getEntityList();
        var sharedValue = solution.getValueList().getFirst();
        for (var entity : entityList) {
            entity.setValue(sharedValue); // All entities share one value -> one pillar.
        }

        var context = NeighborhoodTester
                .build(new SubPillarUnassignMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(200)
                .toList();
        var fullPillarMembers = new HashSet<>(entityList);
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    assertThat(move.getPlanningEntities())
                            .hasSizeLessThanOrEqualTo(2)
                            .allMatch(member -> fullPillarMembers.contains((TestdataAllowsUnassignedEntity) member));
                    assertThat(move.getPlanningValues().getFirst()).isNull();
                });
    }

    @Test
    void differentDrawsProduceDifferentSubpillars() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 6);
        var entityList = solution.getEntityList();
        var sharedValue = solution.getValueList().getFirst();
        for (var entity : entityList) {
            entity.setValue(sharedValue);
        }

        var context = NeighborhoodTester
                .build(new SubPillarUnassignMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var distinctMemberSets = context.getMovesAsStream(
                move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(200)
                .map(move -> new HashSet<>(move.getPlanningEntities()))
                .collect(Collectors.toCollection(HashSet::new));
        assertThat(distinctMemberSets).hasSizeGreaterThan(1);
    }

    @Test
    void survivesSlicesSmallerThanTheSamplersMinimumSize() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        // One value shared by 2 entities (a legal pillar for a minimumSize-2 sampler),
        // and 20 values each held by exactly 1 entity (a slice too small to ever succeed).
        // Skewed heavily toward the too-small slices,
        // so a provider that gives up on its first bad draw
        // - instead of retrying another slice value -
        // reliably produces zero moves here.
        var goodValue = new TestdataValue("good");
        List<TestdataValue> valueList = new ArrayList<>();
        valueList.add(goodValue);
        List<TestdataAllowsUnassignedEntity> entityList = new ArrayList<>();
        entityList.add(new TestdataAllowsUnassignedEntity("good-0", goodValue));
        entityList.add(new TestdataAllowsUnassignedEntity("good-1", goodValue));
        for (var i = 0; i < 20; i++) {
            var singletonValue = new TestdataValue("v" + i);
            valueList.add(singletonValue);
            entityList.add(new TestdataAllowsUnassignedEntity("singleton-" + i, singletonValue));
        }
        var solution = new TestdataAllowsUnassignedSolution("s");
        solution.setValueList(valueList);
        solution.setEntityList(entityList);

        var context = NeighborhoodTester
                .build(new SubPillarUnassignMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    assertThat(move.getPlanningEntities()).containsExactlyInAnyOrder(entityList.get(0), entityList.get(1));
                    assertThat(move.getPlanningValues().getFirst()).isNull();
                });
    }

    @Test
    void pinnedEntityNeverUnassigned() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedEntity.class).basicVariable();

        var v0 = new TestdataValue("v0");
        var pinnedEntity = new TestdataPinnedAllowsUnassignedEntity("pinned", v0, true);
        var free1 = new TestdataPinnedAllowsUnassignedEntity("free1", v0, false);
        var free2 = new TestdataPinnedAllowsUnassignedEntity("free2", v0, false);

        var solution = new TestdataPinnedAllowsUnassignedSolution("s");
        solution.setValueList(List.of(v0));
        solution.setEntityList(List.of(pinnedEntity, free1, free2));

        var context = NeighborhoodTester
                .build(new SubPillarUnassignMoveProvider<>(variableMetaModel, Samplers.pillar(Samplers.exactly(2))),
                        solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassChangeMove<TestdataPinnedAllowsUnassignedSolution, TestdataPinnedAllowsUnassignedEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .flatExtracting(MassChangeMove::getPlanningEntities)
                .doesNotContain(pinnedEntity);
    }

}
