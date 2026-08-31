package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class MassUnassignMoveProviderTest {

    @Test
    void constructorRejectsNonUnassignedVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MassUnassignMoveProvider<>(variableMetaModel, Samplers.exactly(2)));
    }

    @Test
    void mixedValueSampleYieldsOneMoveNullingEveryMember() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        var v0 = new TestdataValue("v0");
        var v1 = new TestdataValue("v1");
        var e0 = new TestdataAllowsUnassignedEntity("e0", v0);
        var e1 = new TestdataAllowsUnassignedEntity("e1", v1);
        var unassignedEntity = new TestdataAllowsUnassignedEntity("unassigned", null);

        var solution = new TestdataAllowsUnassignedSolution("s");
        solution.setValueList(List.of(v0, v1));
        solution.setEntityList(List.of(e0, e1, unassignedEntity));

        var context =
                NeighborhoodTester
                        .build(new MassUnassignMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                        .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    // Only already-assigned entities can be drawn; already-unassigned ones own no move here.
                    assertThat(move.getPlanningEntities()).doesNotContain(unassignedEntity);
                    assertThat(move.getPlanningEntities()).isSubsetOf(e0, e1);
                    assertThat(move.getPlanningValues().getFirst()).isNull();
                });
    }

    @Test
    void sizeOneSampleStillYieldsAMoveAndTheProviderIsNotCutOffEarly() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        // A Sampler that always stops right after the seed, i.e. a Sampler that always produces size-1 samples:
        // unlike MassChange/MassAssign, this must not end the provider early.
        var solution = TestdataAllowsUnassignedSolution.generateSolution(3, 5);

        var context =
                NeighborhoodTester
                        .build(new MassUnassignMoveProvider<>(variableMetaModel, Samplers.exactly(1)), solutionMetaModel)
                        .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .hasSize(50)
                .allSatisfy(move -> {
                    assertThat(move.getPlanningEntities()).hasSize(1);
                    assertThat(move.getPlanningValues().getFirst()).isNull();
                });
    }

    @Test
    void pinnedEntityNeverJoinsASample() {
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
                .build(new MassUnassignMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
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
