package ai.timefold.solver.core.impl.heuristic.selector.common;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues.RangeIndex;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class ReachableValuesTest {

    @Test
    void testSharedEntityValueRange() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        // A and B share an identical value range; C has a distinct one.
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2, v3));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v1, v2, v3));
        var c = new TestdataListEntityProvidingEntity("C", List.of(v3, v4));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getListVariableDescriptor());

        // Both A and B must appear — addEntity must be called for each entity, not just the canonical one.
        assertThat(reachableValues.extractEntitiesAsList(v1)).containsExactlyInAnyOrder(a, b);
        assertThat(reachableValues.extractEntitiesAsList(v2)).containsExactlyInAnyOrder(a, b);
        assertThat(reachableValues.extractEntitiesAsList(v3)).containsExactlyInAnyOrder(a, b, c);
        assertThat(reachableValues.extractEntitiesAsList(v4)).containsExactlyInAnyOrder(c);

        assertThat(reachableValues.isEntityReachable(v1, a)).isTrue();
        assertThat(reachableValues.isEntityReachable(v1, b)).isTrue();
        assertThat(reachableValues.isEntityReachable(v1, c)).isFalse();

        // v3 appears in both the shared range [v1,v2,v3] and C's range [v3,v4]; co-values = union minus self.
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v3, v1, v2, v4);
        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v1, v2, v3);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v4, v3);
    }

    @Test
    void valuesWithEqualRangesGetEqualReachabilityExceptThemselves() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        // v1 and v2 are only in the range of A and B; v4 and v5 only in the ranges of C and D.
        var c = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5));
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2, v3));
        var d = new TestdataListEntityProvidingEntity("D", List.of(v4, v5));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v1, v2, v3));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(c, a, d, b));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var entityDescriptor = scoreDirector.getSolutionDescriptor()
                .findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getListVariableDescriptor());

        // Entity lists keep the order of the solution's entity list.
        assertThat(reachableValues.extractEntitiesAsList(v1)).containsExactly(a, b);
        assertThat(reachableValues.extractEntitiesAsList(v2)).containsExactly(a, b);
        assertThat(reachableValues.extractEntitiesAsList(v3)).containsExactly(c, a, b);
        assertThat(reachableValues.extractEntitiesAsList(v4)).containsExactly(c, d);
        assertThat(reachableValues.extractMovableEntitiesAsList(v5)).containsExactly(c, d);
        assertThat(reachableValues.getReachableEntitiesSize(v1)).isEqualTo(2);
        assertThat(reachableValues.getReachableEntitiesSize(v3)).isEqualTo(3);
        assertThat(reachableValues.getReachableEntitiesSize(v5)).isEqualTo(2);
        assertThat(reachableValues.isEntityReachable(v3, d)).isFalse();
        assertThat(reachableValues.isEntityReachable(v4, d)).isTrue();

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v1, v2, v3);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v2, v1, v3);
        assertThat(reachableValues.extractMovableValuesAsList(v2)).containsExactlyInAnyOrder(v2, v1, v3);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v3, v1, v2, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v4, v3, v5);
        assertThat(reachableValues.extractMovableValuesAsList(v5)).containsExactlyInAnyOrder(v5, v3, v4);
        assertThat(reachableValues.isValueReachable(v1, v1)).isFalse();
        assertThat(reachableValues.isValueReachable(v1, v2)).isTrue();
        assertThat(reachableValues.isValueReachable(v4, v5)).isTrue();
        assertThat(reachableValues.isValueReachable(v1, v4)).isFalse();
        assertThat(reachableValues.isValueReachable(v3, v5)).isTrue();
    }

    @Test
    void valueReachabilityWithManyRangesAndRepeatedOrigins() {
        // More than 64 distinct ranges: the range intersection reads several words before a repeated origin switches to its own value bitset.
        var valueCount = 71;
        var values = new ArrayList<TestdataListEntityProvidingValue>(valueCount);
        for (var i = 0; i < valueCount; i++) {
            values.add(new TestdataListEntityProvidingValue("V" + i));
        }
        var entities = new ArrayList<TestdataListEntityProvidingEntity>(valueCount - 1);
        for (var i = 0; i < valueCount - 1; i++) {
            entities.add(new TestdataListEntityProvidingEntity("E" + i, List.of(values.get(i), values.get(i + 1))));
        }
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(entities);
        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var entityDescriptor = scoreDirector.getSolutionDescriptor()
                .findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getListVariableDescriptor());

        for (var repeat = 0; repeat < 3; repeat++) {
            for (var i = 0; i < valueCount; i++) {
                for (var j = 0; j < valueCount; j++) {
                    assertThat(reachableValues.isValueReachable(values.get(i), values.get(j)))
                            .as("V%d reaches V%d", i, j)
                            .isEqualTo(Math.abs(i - j) == 1);
                }
            }
        }
        // Alternating origins never repeat, so they use the range intersection.
        for (var i = 1; i < valueCount; i++) {
            assertThat(reachableValues.isValueReachable(values.get(i), values.get(i - 1))).isTrue();
            assertThat(reachableValues.isValueReachable(values.get(i - 1), values.get(i))).isTrue();
            assertThat(reachableValues.isValueReachable(values.get(i), values.get(0))).isEqualTo(i == 1);
        }
    }

    @Test
    void testReachableValuesByEntity() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2, v3), List.of(v1, v2));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var c = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v4, v5));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getListVariableDescriptor());

        assertThat(reachableValues.extractEntitiesAsList(v1)).containsExactlyInAnyOrder(a);
        assertThat(reachableValues.extractEntitiesAsList(v2)).containsExactlyInAnyOrder(a, b);
        assertThat(reachableValues.extractEntitiesAsList(v3)).containsExactlyInAnyOrder(a, b, c);
        assertThat(reachableValues.extractEntitiesAsList(v4)).containsExactlyInAnyOrder(c);
        assertThat(reachableValues.extractEntitiesAsList(v5)).containsExactlyInAnyOrder(c);

        assertThat(reachableValues.isEntityReachable(v1, a)).isTrue();
        assertThat(reachableValues.isEntityReachable(v1, b)).isFalse();
        assertThat(reachableValues.isEntityReachable(v1, c)).isFalse();

        assertThat(reachableValues.valueHasMatchingType(v1)).isTrue();
        assertThat(reachableValues.valueHasMatchingType(a)).isFalse();
    }

    @Test
    void testReachableValuesByValue() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2, v3), List.of(v1, v2));
        var b = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var c = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v4, v5));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getListVariableDescriptor());

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v1, v2, v3);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v2, v1, v3);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v3, v1, v2, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v4, v3, v5);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v5, v3, v4);

        // Only origin
        assertThat(reachableValues.isValueReachable(v1, v2)).isTrue();
        assertThat(reachableValues.isValueReachable(v1, v3)).isTrue();
        assertThat(reachableValues.isValueReachable(v1, v5)).isFalse();

        // Null value is not accepted because the setting allowUnassigned is false
        assertThat(reachableValues.isValueReachable(v1, null)).isFalse();
    }

    @Test
    void testUnassignedReachableValues() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var a = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v1, v2, v3), v1);
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3), v3);
        var c = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v3, v4, v5), v4);
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataAllowsUnassignedEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().getFirst());

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v1, v2, v3);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v2, v1, v3);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v3, v1, v2, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v4, v3, v5);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v5, v3, v4);

        // Only origin
        assertThat(reachableValues.isValueReachable(v1, v2)).isTrue();
        assertThat(reachableValues.isValueReachable(v1, v3)).isTrue();
        assertThat(reachableValues.isValueReachable(v1, v5)).isFalse();

        // Null value is not accepted because the setting allowUnassigned is false
        assertThat(reachableValues.isValueReachable(v1, null)).isTrue();
    }

    @Test
    void sortAscendingReachableValues() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var a = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v3, v2, v1));
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v3, v2));
        var c = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v5, v4, v3, v2));
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataAllowsUnassignedEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().getFirst(),
                        new TestdataObjectSorter<TestdataAllowsUnassignedEntityProvidingSolution, TestdataValue>());

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v1, v2, v3);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v2, v1, v3, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v3, v1, v2, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v4, v2, v3, v5);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v5, v2, v3, v4);
    }

    @Test
    void sortDescendingReachableValues() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var a = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v2, v3, v1));
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3));
        var c = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v2, v3, v4, v5));
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataAllowsUnassignedEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().getFirst(),
                        new TestdataObjectSorter<TestdataAllowsUnassignedEntityProvidingSolution, TestdataValue>(false));

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v1, v3, v2);
        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v1, v3, v2);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v2, v5, v4, v3, v1);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v3, v5, v4, v2, v1);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v4, v5, v3, v2);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v5, v4, v3, v2);
    }

    @Test
    void multipleSorters() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var a = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v2, v3, v1));
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3));
        var c = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v2, v3, v4, v5));
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptor(TestdataAllowsUnassignedEntityProvidingEntity.class);

        // No sorter
        var noSorterValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().getFirst(), null);
        assertThat(noSorterValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v2, v3, v1, v4, v5);

        // Ascending sorter replaces the no sorter
        var ascendingSorter = new TestdataObjectSorter<TestdataAllowsUnassignedEntityProvidingSolution, TestdataValue>(true);
        var ascendingValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().getFirst(), ascendingSorter);
        assertThat(ascendingValues).isNotSameAs(noSorterValues)
                .isSameAs(scoreDirector.getValueRangeManager()
                        .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().getFirst(), ascendingSorter));
        assertThat(noSorterValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v2, v1, v3, v4, v5);

        // Descending sorter
        var descendingSorter = new TestdataObjectSorter<TestdataAllowsUnassignedEntityProvidingSolution, TestdataValue>(false);
        var descendingValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().getFirst(), descendingSorter);
        assertThat(descendingValues).isNotSameAs(ascendingValues)
                .isNotSameAs(noSorterValues)
                .isSameAs(scoreDirector.getValueRangeManager()
                        .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().getFirst(), descendingSorter));
        assertThat(descendingValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v2, v5, v4, v3, v1);

        // Null sorter returns the ascending sorter
        var otherNoSorterValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().getFirst(), ascendingSorter);
        assertThat(otherNoSorterValues).isSameAs(ascendingValues);
    }

    @Test
    void indexOfInValueList() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var a = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v2, v3, v1));
        var b = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3));
        var c = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v2, v3, v4, v5));
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a, b, c));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var variableDescriptor = scoreDirector.getSolutionDescriptor()
                .findEntityDescriptor(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .getGenuineVariableDescriptorList().getFirst();
        var unsortedValues =
                scoreDirector.getValueRangeManager().<Object, TestdataValue> getReachableValues(variableDescriptor);
        var sortedValues = scoreDirector.getValueRangeManager().getReachableValues(variableDescriptor,
                new TestdataObjectSorter<TestdataAllowsUnassignedEntityProvidingSolution, TestdataValue>(false));

        var allValues = List.of(v1, v2, v3, v4, v5);
        // A list built before the first lookup and lists built after it.
        unsortedValues.extractValuesAsList(v4);
        sortedValues.extractValuesAsList(v4);
        for (var reachableValues : List.of(unsortedValues, sortedValues)) {
            for (var value : allValues) {
                assertThat(reachableValues.extractValuesAsList(value).get(reachableValues.indexOfInValueList(value)))
                        .isSameAs(value);
            }
            assertThat(reachableValues.indexOfInValueList(new TestdataValue("Unknown"))).isEqualTo(-1);
        }
    }

    @Test
    void unstableEntityValueRangeFails() {
        // Each read of the range creates new values, which are not equal to the values of an earlier read.
        var unstableRange = new AbstractList<TestdataValue>() {
            @Override
            public TestdataValue get(int index) {
                return new TestdataValue("V" + index);
            }

            @Override
            public int size() {
                return 2;
            }
        };
        var a = new TestdataAllowsUnassignedEntityProvidingEntity("A", unstableRange);
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(a));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var variableDescriptor = scoreDirector.getSolutionDescriptor()
                .findEntityDescriptor(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .getGenuineVariableDescriptorList().getFirst();
        var valueRangeManager = scoreDirector.getValueRangeManager();
        assertThatThrownBy(() -> valueRangeManager.getReachableValues(variableDescriptor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("V0")
                .hasMessageContaining(variableDescriptor.getVariableName())
                .hasMessageContaining("@ValueRangeProvider");
    }

    @Test
    void rangeIndexComparesArrayContents() {
        var values = new BitSet();
        values.set(1);
        var rangeIndex = new RangeIndex(new int[] { 0, 0 }, new int[][] { { 0, 1 } },
                new BitSet[] { values }, 1);
        var equalRangeIndex = new RangeIndex(new int[] { 0, 0 }, new int[][] { { 0, 1 } },
                new BitSet[] { (BitSet) values.clone() }, 1);
        var otherRangeIndex = new RangeIndex(new int[] { 0, 0 }, new int[][] { { 0 } },
                new BitSet[] { values }, 1);

        assertThat(rangeIndex)
                .isEqualTo(equalRangeIndex)
                .hasSameHashCodeAs(equalRangeIndex)
                .hasToString(equalRangeIndex.toString())
                .isNotEqualTo(otherRangeIndex);
    }

    @Test
    void valuesInNoRangeAreNotReachable() {
        var v1 = new TestdataListValue("V1");
        var v2 = new TestdataListValue("V2");
        var solution = new TestdataListSolution();
        solution.setValueList(List.of(v1, v2));
        solution.setEntityList(List.of());

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var variableDescriptor = scoreDirector.getSolutionDescriptor()
                .findEntityDescriptor(TestdataListEntity.class)
                .getListVariableDescriptor();
        var reachableValues = scoreDirector.getValueRangeManager()
                .<TestdataListEntity, TestdataListValue> getReachableValues(variableDescriptor);

        assertThat(reachableValues.isValueReachable(v1, v2)).isFalse();
    }

    @Test
    void sharedListsAreUnmodifiable() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var a = new TestdataListEntityProvidingEntity("A", List.of(v1, v2));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(a));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var entityDescriptor = scoreDirector.getSolutionDescriptor()
                .findEntityDescriptor(TestdataListEntityProvidingEntity.class);
        var reachableValues = scoreDirector.getValueRangeManager()
                .<TestdataListEntityProvidingEntity, TestdataListEntityProvidingValue> getReachableValues(
                        entityDescriptor.getListVariableDescriptor());

        var valueList = reachableValues.extractValuesAsList(v1);
        assertThatThrownBy(() -> valueList.set(0, v2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> valueList.sort(null)).isInstanceOf(UnsupportedOperationException.class);
        var entityList = reachableValues.extractEntitiesAsList(v1);
        assertThatThrownBy(() -> entityList.set(0, a)).isInstanceOf(UnsupportedOperationException.class);
    }
}
