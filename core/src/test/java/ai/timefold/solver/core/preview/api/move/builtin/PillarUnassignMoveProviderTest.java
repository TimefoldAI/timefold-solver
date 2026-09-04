package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.move.builtin.MassChangeMove;
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
class PillarUnassignMoveProviderTest {

    @Test
    void constructorRejectsNonUnassignedVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PillarUnassignMoveProvider<>(variableMetaModel));
    }

    @Test
    void onlyDrawsAssignedEntitiesAndAlwaysAssignsNull() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        // generateSolution(2, 2): entity0 starts null, entity1 starts assigned.
        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var unassignedEntity = solution.getEntityList().get(0);
        var assignedEntity = solution.getEntityList().get(1);

        var context = NeighborhoodTester.build(new PillarUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    // Only the already-assigned entity can be a pillar member.
                    assertThat(move.getPlanningEntities()).doesNotContain(unassignedEntity);
                    assertThat(move.getPlanningEntities()).contains(assignedEntity);
                    // The destination is always null.
                    assertThat(move.getPlanningValues().getFirst()).isNull();
                });
    }

    @Test
    void pinnedEntityNeverUnassigned() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedEntity.class)
                .basicVariable();

        var v0 = new TestdataValue("v0");
        // pinnedEntity shares v0 with freeEntity, but forEach(..., false) excludes pinned entities from the entity source,
        // so it must never be unassigned.
        var pinnedEntity = new TestdataPinnedAllowsUnassignedEntity("pinned", v0, true);
        var freeEntity = new TestdataPinnedAllowsUnassignedEntity("free", v0, false);

        var solution = new TestdataPinnedAllowsUnassignedSolution("s");
        solution.setValueList(List.of(v0));
        solution.setEntityList(List.of(pinnedEntity, freeEntity));

        var context = NeighborhoodTester.build(new PillarUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (MassChangeMove<TestdataPinnedAllowsUnassignedSolution, TestdataPinnedAllowsUnassignedEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getPlanningEntities()).containsExactly(freeEntity));
    }

    @Test
    void unassignPillarIsHomogeneous() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(3, 4);
        var entityList = solution.getEntityList();
        var valueList = solution.getValueList();
        var sharedValue = valueList.getFirst();
        // entity0 and entity1 share a value, forming a pillar of size 2.
        entityList.get(0).setValue(sharedValue);
        entityList.get(1).setValue(sharedValue);
        // entity2 gets its own distinct value, forming a size-1 pillar.
        entityList.get(2).setValue(valueList.get(1));
        // entity3 stays unassigned and must never appear in a move.
        entityList.get(3).setValue(null);

        var context = NeighborhoodTester.build(new PillarUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(200)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    // Every generated pillar must be homogeneous: all members share the same (pre-move) value.
                    var currentValues = move.getPlanningEntities().stream()
                            .map(e -> ((TestdataAllowsUnassignedEntity) e).getValue())
                            .collect(Collectors.toSet());
                    assertThat(currentValues).hasSize(1).doesNotContainNull();
                    // Unassign always sets the destination to null; unlike MassChangeMove, size-1 is legal.
                    assertThat(move.getPlanningValues().getFirst()).isNull();
                });
    }

    @Test
    void undoRestoresOriginalValue() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var assignedEntity = solution.getEntityList().get(1);
        var originalValue = assignedEntity.getValue();
        assertThat(originalValue).isNotNull();

        var context = NeighborhoodTester.build(new PillarUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var move = context.getMovesAsStream().findFirst().orElseThrow();

        context.getMoveTestContext().executeTemporarily(move, view -> assertThat(assignedEntity.getValue()).isNull());
        assertThat(assignedEntity.getValue()).isEqualTo(originalValue);
    }

}
