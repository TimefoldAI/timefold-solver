package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class MassChangeMoveProviderTest {

    @Test
    void mixedValueSampleYieldsOneMoveSettingEveryMemberToALegalValue() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();

        // generateSolution(2, 2) cycles distinct values across entities, so entity0 and entity1 differ.
        var solution = TestdataSolution.generateSolution(2, 2);
        var entity0 = solution.getEntityList().get(0);
        var entity1 = solution.getEntityList().get(1);
        assertThat(entity0.getValue()).isNotEqualTo(entity1.getValue());

        var context = NeighborhoodTester
                .build(new MassChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(move -> (MassChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    var destination = move.getPlanningValues().getFirst();
                    assertThat(move.getPlanningEntities()).isSubsetOf(entity0, entity1);
                    assertThat(destination).isIn(solution.getValueList());
                });
    }

    @Test
    void assignSideDisabledExcludesUnassignedEntities() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var unassigned = new TestdataAllowsUnassignedEntity("unassigned", null);
        var assigned1 = new TestdataAllowsUnassignedEntity("assigned1", value1);
        var assigned2 = new TestdataAllowsUnassignedEntity("assigned2", value2);

        var solution = new TestdataAllowsUnassignedSolution("s");
        solution.setValueList(List.of(value1, value2));
        solution.setEntityList(List.of(unassigned, assigned1, assigned2));

        // crossingNull=false: the source excludes unassigned entities entirely - not "isolating" the assign side,
        // but removing it, since false now governs the source too (see the class javadoc).
        var context = NeighborhoodTester
                .build(new MassChangeMoveProvider<>(variableMetaModel, Samplers.all(), false), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    assertThat(move.getPlanningEntities()).doesNotContain(unassigned);
                    assertThat(move.getPlanningValues().getFirst()).isNotNull(); // No unassign either, without crossingNull.
                });
    }

    @Test
    void mixedSampleMayAssignOrUnassign() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();

        var value = new TestdataValue("v");
        var unassigned = new TestdataAllowsUnassignedEntity("unassigned", null);
        var assigned = new TestdataAllowsUnassignedEntity("assigned", value);

        var solution = new TestdataAllowsUnassignedSolution("s");
        solution.setValueList(List.of(value));
        solution.setEntityList(List.of(unassigned, assigned));

        // Default constructor: crossingNull is true, because this variable allows unassigned values.
        var context = NeighborhoodTester
                .build(new MassChangeMoveProvider<>(variableMetaModel, Samplers.all()), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (MassChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    assertThat(move.getPlanningEntities()).containsExactlyInAnyOrder(unassigned, assigned);
                    var destination = move.getPlanningValues().getFirst();
                    if (destination != null) {
                        assertThat(destination).isEqualTo(value);
                    }
                });
        // The relaxed assertion above would also pass if one branch never fired;
        // these two lines make sure the relaxation isn't silently hiding a broken branch.
        // A null destination unassigns the assigned member; a non-null destination assigns the unassigned member.
        assertThat(moves)
                .anyMatch(move -> move.getPlanningValues().getFirst() == null)
                .anyMatch(move -> move.getPlanningValues().getFirst() != null);
    }

    @Test
    void crossingNullDefaultFalseWhenVariableDoesNotAllowUnassigned() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();

        var solution = TestdataSolution.generateSolution(2, 2);
        var entity0 = solution.getEntityList().get(0);
        var entity1 = solution.getEntityList().get(1);
        assertThat(entity0.getValue()).isNotEqualTo(entity1.getValue());

        // Default constructor: crossingNull is false, because this variable does not allow unassigned values -
        // the constructor must not throw for the default, unlike an explicit true would.
        var context = NeighborhoodTester
                .build(new MassChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);
        var moves = context.getMovesAsStream(move -> (MassChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves).isNotEmpty();
    }

    @Test
    void constructorRejectsExplicitCrossingNullOnNonUnassignedVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MassChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2), true));
    }

    @Test
    void homogeneousSampleNeverProducesANoOpMove() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();

        var solution = TestdataSolution.generateSolution(2, 5);
        var entityList = solution.getEntityList();
        var sharedValue = solution.getValueList().getFirst();
        for (var entity : entityList) {
            entity.setValue(sharedValue); // Every entity shares one value -> every sample is homogeneous.
        }

        var context = NeighborhoodTester
                .build(new MassChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(move -> (MassChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move)
                .limit(200)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getPlanningValues().getFirst()).isNotEqualTo(sharedValue));
    }

    @Test
    void sizeOneSampleProducesNoMove() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();

        var solution = TestdataSolution.generateSolution(2, 5);

        var context = NeighborhoodTester
                .build(new MassChangeMoveProvider<>(variableMetaModel, Samplers.exactly(1)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream().limit(10).toList();
        assertThat(moves).isEmpty();
    }

    @Test
    void emptyIntersectionEndsTheIteratorInsteadOfHanging() {
        var solutionMetaModel = TestdataAllowsUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .basicVariable();

        var north1 = new TestdataValue("north1");
        var north2 = new TestdataValue("north2");
        var south1 = new TestdataValue("south1");
        var south2 = new TestdataValue("south2");
        // Disjoint per-entity ranges: no destination is ever legal for both at once.
        var northEntity = new TestdataAllowsUnassignedEntityProvidingEntity("north", List.of(north1, north2), north1);
        var southEntity = new TestdataAllowsUnassignedEntityProvidingEntity("south", List.of(south1, south2), south1);

        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s");
        solution.setEntityList(List.of(northEntity, southEntity));

        // Samplers.all() drains the whole 2-entity dataset every draw,
        // so every sample is {north, south} and the intersection is always empty.
        // crossingNull=false, explicitly: with the default (true, since this variable allows unassigned values) a null destination is still legal -
        // see emptyIntersectionStillYieldsNullDestinationWhenCrossingNull below.
        var context = NeighborhoodTester
                .build(new MassChangeMoveProvider<>(variableMetaModel, Samplers.all(), false), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream().limit(10).toList();
        assertThat(moves).isEmpty();
    }

    @Test
    void emptyIntersectionStillYieldsNullDestinationWhenCrossingNull() {
        var solutionMetaModel = TestdataAllowsUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .basicVariable();

        var north1 = new TestdataValue("north1");
        var north2 = new TestdataValue("north2");
        var south1 = new TestdataValue("south1");
        var south2 = new TestdataValue("south2");
        // Same disjoint-range fixture as emptyIntersectionEndsTheIteratorInsteadOfHanging above.
        var northEntity = new TestdataAllowsUnassignedEntityProvidingEntity("north", List.of(north1, north2), north1);
        var southEntity = new TestdataAllowsUnassignedEntityProvidingEntity("south", List.of(south1, south2), south1);

        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s");
        solution.setEntityList(List.of(northEntity, southEntity));

        // Default constructor: crossingNull is true. Regression test for the provenEmpty || rollNull ordering:
        // the non-null intersection is still empty,
        // but a null destination (unassigning the whole sample) is legal for both entities regardless,
        // so it is offered instead of nothing.
        var context = NeighborhoodTester
                .build(new MassChangeMoveProvider<>(variableMetaModel, Samplers.all()), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassChangeMove<TestdataAllowsUnassignedEntityProvidingSolution, TestdataAllowsUnassignedEntityProvidingEntity, TestdataValue>) move)
                .limit(10)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getPlanningValues().getFirst()).isNull());
    }

    @Test
    void pinnedEntitySharingAValueWithFreeEntitiesNeverJoinsASample() {
        var solutionMetaModel = TestdataPinnedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntity.class).basicVariable();

        var v0 = new TestdataValue("v0");
        var v1 = new TestdataValue("v1");
        var pinnedEntity = new TestdataPinnedEntity("pinned", v0, true);
        var free1 = new TestdataPinnedEntity("free1", v0, false);
        var free2 = new TestdataPinnedEntity("free2", v0, false);

        var solution = new TestdataPinnedSolution("s");
        solution.setValueList(List.of(v0, v1));
        solution.setEntityList(List.of(pinnedEntity, free1, free2));

        var context = NeighborhoodTester
                .build(new MassChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
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

}
