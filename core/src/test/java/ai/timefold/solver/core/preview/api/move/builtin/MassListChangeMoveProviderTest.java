package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.move.builtin.MassListChangeMove;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class MassListChangeMoveProviderTest {

    @Test
    void sizeOneSampleProducesNoMove() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var values = new TestdataListValue[5];
        for (var i = 0; i < 5; i++) {
            values[i] = new TestdataListValue("v" + i);
        }
        var entity = new TestdataListEntity("A", values);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new MassListChangeMoveProvider<>(variableMetaModel, Samplers.exactly(1)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream().limit(10).toList();
        assertThat(moves).isEmpty();
    }

    @Test
    void sampleNeverIncludesAnUnassignedValue() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var assigned1 = new TestdataAllowsUnassignedValuesListValue("assigned1");
        var assigned2 = new TestdataAllowsUnassignedValuesListValue("assigned2");
        var unassigned = new TestdataAllowsUnassignedValuesListValue("unassigned");
        var entity = new TestdataAllowsUnassignedValuesListEntity("A", assigned1, assigned2);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(assigned1, assigned2, unassigned));

        var context = NeighborhoodTester
                .build(new MassListChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassListChangeMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getSample()).doesNotContain(unassigned));
    }

    @Test
    void destinationRespectsEveryMembersValueRange() {
        var solutionMetaModel = TestdataListUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListUnassignedEntityProvidingEntity.class)
                .listVariable("valueList", TestdataValue.class);

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        // entityA's range admits both values,
        // so any destination on entityA is legal for a sample drawn from entityA and/or entityB (v1 is shared).
        var entityA = new TestdataListUnassignedEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var entityB = new TestdataListUnassignedEntityProvidingEntity("B", List.of(v1));
        var solution = new TestdataListUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(entityA, entityB));

        var context = NeighborhoodTester
                .build(new MassListChangeMoveProvider<>(variableMetaModel, Samplers.all()), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassListChangeMove<TestdataListUnassignedEntityProvidingSolution, TestdataListUnassignedEntityProvidingEntity, TestdataValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    var destination = move.getDestination();
                    if (destination != null) {
                        TestdataListUnassignedEntityProvidingEntity destinationEntity = destination.entity();
                        assertThat(destinationEntity.getValueRange()).containsAll(move.getSample());
                    }
                });
    }

    @Test
    void crossingNullDefaultTrueAlsoUnassignsWholeSample() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var values = new TestdataAllowsUnassignedValuesListValue[8];
        for (var i = 0; i < 8; i++) {
            values[i] = new TestdataAllowsUnassignedValuesListValue("v" + i);
        }
        var entity = new TestdataAllowsUnassignedValuesListEntity("A", values);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new MassListChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);
        var moves = context.getMovesAsStream(
                move -> (MassListChangeMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) move)
                .limit(1000)
                .toList();
        assertThat(moves).anyMatch(move -> move.getDestination() == null);
    }

    @Test
    void crossingNullFalseNeverUnassignsSample() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var values = new TestdataAllowsUnassignedValuesListValue[8];
        for (var i = 0; i < 8; i++) {
            values[i] = new TestdataAllowsUnassignedValuesListValue("v" + i);
        }
        var entity = new TestdataAllowsUnassignedValuesListEntity("A", values);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new MassListChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2), false), solutionMetaModel)
                .using(solution);
        var moves = context.getMovesAsStream(
                move -> (MassListChangeMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) move)
                .limit(500)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .noneMatch(move -> move.getDestination() == null);
    }

    @Test
    void constructorRejectsExplicitCrossingNullOnVariableWithoutUnassignedValues() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MassListChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2), true));
    }

    @Test
    void differentDrawsProduceDifferentSamples() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var values = new TestdataListValue[10];
        for (var i = 0; i < 10; i++) {
            values[i] = new TestdataListValue("v" + i);
        }
        var entity = new TestdataListEntity("A", values);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new MassListChangeMoveProvider<>(variableMetaModel, Samplers.exactly(3)), solutionMetaModel)
                .using(solution);

        var distinctSamples = context
                .getMovesAsStream(
                        move -> (MassListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(100)
                .map(move -> move.getSample().toString())
                .collect(Collectors.toCollection(HashSet::new));
        assertThat(distinctSamples).hasSizeGreaterThan(1);
    }

    @Test
    void pinnedValueNeverJoinsASample() {
        var solutionMetaModel = TestdataPinnedUnassignedValuesListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataPinnedUnassignedValuesListValue.class);

        var pinned1 = new TestdataPinnedUnassignedValuesListValue("pinned1");
        var pinned2 = new TestdataPinnedUnassignedValuesListValue("pinned2");
        var free1 = new TestdataPinnedUnassignedValuesListValue("free1");
        var free2 = new TestdataPinnedUnassignedValuesListValue("free2");
        var entity = new TestdataPinnedUnassignedValuesListEntity("A", pinned1, pinned2, free1, free2);
        entity.setPlanningPinToIndex(2);
        var solution = new TestdataPinnedUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(pinned1, pinned2, free1, free2));

        var context = NeighborhoodTester
                .build(new MassListChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassListChangeMove<TestdataPinnedUnassignedValuesListSolution, TestdataPinnedUnassignedValuesListEntity, TestdataPinnedUnassignedValuesListValue>) move)
                .limit(100)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getSample()).doesNotContain(pinned1, pinned2));
    }

    @Test
    void destinationNeverFallsInThePinnedPrefix() {
        var solutionMetaModel = TestdataPinnedUnassignedValuesListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataPinnedUnassignedValuesListValue.class);

        var pinned1 = new TestdataPinnedUnassignedValuesListValue("pinned1");
        var pinned2 = new TestdataPinnedUnassignedValuesListValue("pinned2");
        var free1 = new TestdataPinnedUnassignedValuesListValue("free1");
        var free2 = new TestdataPinnedUnassignedValuesListValue("free2");
        var entity = new TestdataPinnedUnassignedValuesListEntity("A", pinned1, pinned2, free1, free2);
        entity.setPlanningPinToIndex(2);
        var solution = new TestdataPinnedUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(pinned1, pinned2, free1, free2));

        var context = NeighborhoodTester
                .build(new MassListChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (MassListChangeMove<TestdataPinnedUnassignedValuesListSolution, TestdataPinnedUnassignedValuesListEntity, TestdataPinnedUnassignedValuesListValue>) move)
                .limit(100)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    var destination = move.getDestination();
                    if (destination != null) {
                        assertThat(destination.index()).isGreaterThanOrEqualTo(2);
                    }
                });
    }

    @Test
    void fullyPinnedEntityIsNeverDrawnFromOrInto() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataPinnedAllowsUnassignedValuesListValue.class);

        var pinnedValues = new TestdataPinnedAllowsUnassignedValuesListValue[3];
        for (var i = 0; i < 3; i++) {
            pinnedValues[i] = new TestdataPinnedAllowsUnassignedValuesListValue("p" + i);
        }
        var pinnedEntity = new TestdataPinnedAllowsUnassignedValuesListEntity("pinned", List.of(pinnedValues));
        pinnedEntity.setPinned(true);

        var freeValues = new TestdataPinnedAllowsUnassignedValuesListValue[5];
        for (var i = 0; i < 5; i++) {
            freeValues[i] = new TestdataPinnedAllowsUnassignedValuesListValue("f" + i);
        }
        var freeEntity = new TestdataPinnedAllowsUnassignedValuesListEntity("free", List.of(freeValues));

        var solution = new TestdataPinnedAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(pinnedEntity, freeEntity));
        var allValues = new ArrayList<TestdataPinnedAllowsUnassignedValuesListValue>();
        allValues.addAll(List.of(pinnedValues));
        allValues.addAll(List.of(freeValues));
        solution.setValueList(allValues);

        var context = NeighborhoodTester
                .build(new MassListChangeMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassListChangeMove<TestdataPinnedAllowsUnassignedValuesListSolution, TestdataPinnedAllowsUnassignedValuesListEntity, TestdataPinnedAllowsUnassignedValuesListValue>) move)
                .limit(100)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    assertThat(pinnedEntity.getValueList()).doesNotContainAnyElementsOf(move.getSample());
                    var destination = move.getDestination();
                    if (destination != null) {
                        assertThat(destination.<TestdataPinnedAllowsUnassignedValuesListEntity> entity())
                                .isNotEqualTo(pinnedEntity);
                    }
                });
    }

}
