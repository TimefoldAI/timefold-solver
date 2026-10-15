package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.move.builtin.MassChangeMove;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class PillarChangeMoveProviderTest {

    @Test
    void homogeneityAndNoOpExcluded() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();

        var solution = TestdataSolution.generateSolution(3, 4);
        var entityList = solution.getEntityList();
        var valueList = solution.getValueList();
        var sharedValue = valueList.getFirst();
        // entity0 and entity1 share a value, forming a pillar of size 2.
        entityList.get(0).setValue(sharedValue);
        entityList.get(1).setValue(sharedValue);
        // entity2 and entity3 each get their own distinct value, forming size-1 pillars.
        entityList.get(2).setValue(valueList.get(1));
        entityList.get(3).setValue(valueList.get(2));

        var context = NeighborhoodTester.build(new PillarChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(move -> (MassChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move)
                .limit(200)
                .toList();
        assertThat(moves).isNotEmpty();
        assertThat(moves).allSatisfy(move -> {
            var entities = move.getPlanningEntities();
            // Every generated pillar must be homogeneous: all members share the same (pre-move) value.
            var currentValues = entities.stream()
                    .map(e -> ((TestdataEntity) e).getValue())
                    .collect(Collectors.toSet());
            assertThat(currentValues).hasSize(1);
            // No move to the pillar's own current value.
            assertThat(move.getPlanningValues()).doesNotContain(currentValues.iterator().next());
            // Size-1 pillars are never generated for change.
            assertThat(entities).hasSizeGreaterThanOrEqualTo(2);
        });
    }

    @Test
    void pinnedEntityExcludedFromPillar() {
        var solutionMetaModel = TestdataPinnedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntity.class)
                .basicVariable();

        var v0 = new TestdataValue("v0");
        var v1 = new TestdataValue("v1");
        // pinnedEntity shares v0 with free1 and free2,
        // but forEach(..., false) excludes pinned entities from the entity source,
        // so it must never join their pillar.
        var pinnedEntity = new TestdataPinnedEntity("pinned", v0, true);
        var free1 = new TestdataPinnedEntity("free1", v0, false);
        var free2 = new TestdataPinnedEntity("free2", v0, false);

        var solution = new TestdataPinnedSolution("s");
        solution.setValueList(List.of(v0, v1));
        solution.setEntityList(List.of(pinnedEntity, free1, free2));

        var context = NeighborhoodTester.build(new PillarChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
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
    void entityDependentRangeRejectsOutOfRangeDestination() {
        var solutionMetaModel = TestdataEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class)
                .basicVariable();

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var value3 = new TestdataValue("v3");

        // Pillar A: two entities sharing value1, each with a range that also includes value2.
        var entityA1 = new TestdataEntityProvidingEntity("a1", List.of(value1, value2));
        entityA1.setValue(value1);
        var entityA2 = new TestdataEntityProvidingEntity("a2", List.of(value1, value2));
        entityA2.setValue(value1);
        // Pillar B: two entities sharing value3, each with a range restricted to value3 only.
        var entityB1 = new TestdataEntityProvidingEntity("b1", List.of(value3));
        entityB1.setValue(value3);
        var entityB2 = new TestdataEntityProvidingEntity("b2", List.of(value3));
        entityB2.setValue(value3);

        var solution = new TestdataEntityProvidingSolution("s");
        solution.setEntityList(List.of(entityA1, entityA2, entityB1, entityB2));

        var pillarA = Sample.of(List.of(entityA1, entityA2));
        var pillarB = Sample.of(List.of(entityB1, entityB2));

        var context = NeighborhoodTester.build(new PillarChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        // Pillar A can legally move to value2 (in range for both members).
        context.producesAllOf(Moves.massChange(variableMetaModel, pillarA, value2));
        // Pillar B has no legal destination: value1/value2 are out of range for its members,
        // and value3 is its own current value.
        context.producesNoneOf(
                Moves.massChange(variableMetaModel, pillarB, value1),
                Moves.massChange(variableMetaModel, pillarB, value2),
                Moves.massChange(variableMetaModel, pillarB, value3));
    }

    @Test
    void retirementLeavesOnlyTheLegalPillarsMoves() {
        var solutionMetaModel = TestdataEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class)
                .basicVariable();

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var value3 = new TestdataValue("v3");

        // Pillar A: legal destination value2 exists.
        var entityA1 = new TestdataEntityProvidingEntity("a1", List.of(value1, value2));
        entityA1.setValue(value1);
        var entityA2 = new TestdataEntityProvidingEntity("a2", List.of(value1, value2));
        entityA2.setValue(value1);
        // Pillar B: no legal destination exists (range restricted to its own current value).
        var entityB1 = new TestdataEntityProvidingEntity("b1", List.of(value3));
        entityB1.setValue(value3);
        var entityB2 = new TestdataEntityProvidingEntity("b2", List.of(value3));
        entityB2.setValue(value3);

        var solution = new TestdataEntityProvidingSolution("s");
        solution.setEntityList(List.of(entityA1, entityA2, entityB1, entityB2));

        var pillarA = Sample.of(List.of(entityA1, entityA2));

        var context = NeighborhoodTester.build(new PillarChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var expectedMove = Moves.massChange(variableMetaModel, pillarA, value2);
        var distinctMoves = context.getMovesAsStream()
                .limit(200)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // Pillar B is retired after its probes come back empty, so it never produces a move;
        // Pillar A's only legal destination keeps recurring, so the distinct move set stays finite (size 1).
        assertThat(distinctMoves).containsExactly(expectedMove);
    }

    @Test
    void lowLegalFractionDestinationIsStillFound() {
        // Originally a regression test for the fixed-width probe (createRightIterator used to try a flat 9 draws regardless of pool size,
        // so this 1-in-50 fixture would almost always miss).
        // The destination search no longer draws from a 50-value pool at all:
        // it samples from entityA2's range ([value1, value2], the smaller of the pillar's two distinct ranges),
        // where value2 is found deterministically, not probabilistically.
        var solutionMetaModel = TestdataEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class)
                .basicVariable();

        var values = new ArrayList<TestdataValue>();
        for (var i = 1; i <= 50; i++) {
            values.add(new TestdataValue("v" + i));
        }
        var value1 = values.get(0);
        var value2 = values.get(1);

        // entityA1 accepts every value;
        // entityA2 accepts only value1 (current) and value2, so value2 is pillar A's only legal destination out of the 50-value pool.
        var entityA1 = new TestdataEntityProvidingEntity("a1", values);
        entityA1.setValue(value1);
        var entityA2 = new TestdataEntityProvidingEntity("a2", List.of(value1, value2));
        entityA2.setValue(value1);

        var solution = new TestdataEntityProvidingSolution("s");
        solution.setEntityList(List.of(entityA1, entityA2));

        var pillarA = Sample.of(List.of(entityA1, entityA2));
        var expectedMove = Moves.massChange(variableMetaModel, pillarA, value2);

        var context = NeighborhoodTester.build(new PillarChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream().limit(10).toList();
        assertThat(moves).contains(expectedMove);
    }

    @Test
    void crossingNullDefaultTrueAlsoUnassignsPillar() {
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
        entityList.get(2).setValue(valueList.get(1));
        entityList.get(3).setValue(null);
        var pillar = Sample.of(List.of(entityList.get(0), entityList.get(1)));

        // Default constructor: crossingNull is true, because this variable allows unassigned values.
        var context = NeighborhoodTester.build(new PillarChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(Moves.massChange(variableMetaModel, pillar, null));
    }

    @Test
    void crossingNullFalseNeverUnassignsPillar() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(3, 4);
        var entityList = solution.getEntityList();
        var valueList = solution.getValueList();
        var sharedValue = valueList.getFirst();
        entityList.get(0).setValue(sharedValue);
        entityList.get(1).setValue(sharedValue);
        entityList.get(2).setValue(valueList.get(1));
        entityList.get(3).setValue(null);
        var pillar = Sample.of(List.of(entityList.get(0), entityList.get(1)));

        var context =
                NeighborhoodTester.build(new PillarChangeMoveProvider<>(variableMetaModel, false), solutionMetaModel)
                        .using(solution);
        context.producesNoneOf(Moves.massChange(variableMetaModel, pillar, null));
    }

    @Test
    void emptyIntersectionStillYieldsNullDestination() {
        var solutionMetaModel = TestdataAllowsUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .basicVariable();

        var value1 = new TestdataValue("v1");
        // Pillar: two entities sharing value1, each restricted to value1 only.
        // No non-null destination exists (the pillar's own value is excluded),
        // but crossingNull=true still offers the null destination -
        // this is the provenEmpty || rollNull ordering: a pillar whose non-null intersection is empty still has a legal null destination.
        var entityA1 = new TestdataAllowsUnassignedEntityProvidingEntity("a1", List.of(value1), value1);
        var entityA2 = new TestdataAllowsUnassignedEntityProvidingEntity("a2", List.of(value1), value1);

        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s");
        solution.setEntityList(List.of(entityA1, entityA2));

        var pillar = Sample.of(List.of(entityA1, entityA2));

        var context = NeighborhoodTester.build(new PillarChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(Moves.massChange(variableMetaModel, pillar, null));
        context.producesNoneOf(Moves.massChange(variableMetaModel, pillar, value1)); // No-op.
    }

    @Test
    void constructorRejectsExplicitCrossingNullOnNonUnassignedVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class).basicVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PillarChangeMoveProvider<>(variableMetaModel, true));
    }

}
