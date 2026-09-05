package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.move.builtin.MassChangeMove;
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
class MassAssignMoveProviderTest {

    @Test
    void sampleMembersAreAlwaysASubsetOfTheUnassignedEntities() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        var value = new TestdataValue("v");
        var e0 = new TestdataAllowsUnassignedEntity("e0", null);
        var e1 = new TestdataAllowsUnassignedEntity("e1", null);
        var e2 = new TestdataAllowsUnassignedEntity("e2", null);
        var e3 = new TestdataAllowsUnassignedEntity("e3", null);
        var e4 = new TestdataAllowsUnassignedEntity("e4", null);
        var allUnassigned = List.of(e0, e1, e2, e3, e4);
        var solution = new TestdataAllowsUnassignedSolution("s");
        solution.setValueList(List.of(value));
        solution.setEntityList(new ArrayList<>(allUnassigned));

        var context = NeighborhoodTester
                .build(new MassAssignMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(200)
                .toList();
        assertThat(moves).isNotEmpty();
        var unassignedEntitySet = new HashSet<>(allUnassigned);
        assertThat(moves).allSatisfy(move -> {
            assertThat(move.getPlanningEntities())
                    .hasSizeLessThanOrEqualTo(2)
                    .allMatch(unassignedEntitySet::contains);
            assertThat(move.getPlanningValues().getFirst()).isEqualTo(value);
        });
    }

    @Test
    void differentDrawsProduceDifferentSamples() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        var value = new TestdataValue("v");
        var e0 = new TestdataAllowsUnassignedEntity("e0", null);
        var e1 = new TestdataAllowsUnassignedEntity("e1", null);
        var e2 = new TestdataAllowsUnassignedEntity("e2", null);
        var e3 = new TestdataAllowsUnassignedEntity("e3", null);
        var e4 = new TestdataAllowsUnassignedEntity("e4", null);
        var solution = new TestdataAllowsUnassignedSolution("s");
        solution.setValueList(List.of(value));
        solution.setEntityList(new ArrayList<>(List.of(e0, e1, e2, e3, e4)));

        var context = NeighborhoodTester
                .build(new MassAssignMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var distinctMemberSets = context
                .getMovesAsStream(
                        move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(200)
                .map(move -> new HashSet<>(move.getPlanningEntities()))
                .collect(Collectors.toSet());
        assertThat(distinctMemberSets).hasSizeGreaterThan(1);
    }

    @Test
    void pinnedUnassignedEntityNeverJoinsASample() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedEntity.class).basicVariable();

        var value = new TestdataValue("v");
        var pinnedEntity = new TestdataPinnedAllowsUnassignedEntity("pinned", null, true);
        var free1 = new TestdataPinnedAllowsUnassignedEntity("free1", null, false);
        var free2 = new TestdataPinnedAllowsUnassignedEntity("free2", null, false);

        var solution = new TestdataPinnedAllowsUnassignedSolution("s");
        solution.setValueList(List.of(value));
        solution.setEntityList(List.of(pinnedEntity, free1, free2));

        var context = NeighborhoodTester
                .build(new MassAssignMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
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
