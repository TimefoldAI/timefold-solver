package ai.timefold.solver.core.impl.heuristic.selector.common;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class ReachableValuesTest {

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
                .getReachableValues(entityDescriptor.getGenuineListVariableDescriptor());

        assertThat(reachableValues.extractEntitiesAsList(v1)).containsExactlyInAnyOrder(a);
        assertThat(reachableValues.extractEntitiesAsList(v2)).containsExactlyInAnyOrder(a, b);
        assertThat(reachableValues.extractEntitiesAsList(v3)).containsExactlyInAnyOrder(a, b, c);
        assertThat(reachableValues.extractEntitiesAsList(v4)).containsExactlyInAnyOrder(c);
        assertThat(reachableValues.extractEntitiesAsList(v5)).containsExactlyInAnyOrder(c);

        assertThat(reachableValues.isEntityReachable(v1, a)).isTrue();
        assertThat(reachableValues.isEntityReachable(v1, b)).isFalse();
        assertThat(reachableValues.isEntityReachable(v1, c)).isFalse();

        assertThat(reachableValues.matchesValueClass(v1)).isTrue();
        assertThat(reachableValues.matchesValueClass(a)).isFalse();
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
                .getReachableValues(entityDescriptor.getGenuineListVariableDescriptor());

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v2, v3);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v1, v3);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v1, v2, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v3, v5);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v3, v4);

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
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0));

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v2, v3);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v1, v3);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v1, v2, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v3, v5);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v3, v4);

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
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0),
                        new TestdataObjectSorter<TestdataAllowsUnassignedEntityProvidingSolution, TestdataValue>());

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v2, v3);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v1, v3, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v1, v2, v4, v5);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v2, v3, v5);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v2, v3, v4);
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
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0),
                        new TestdataObjectSorter<TestdataAllowsUnassignedEntityProvidingSolution, TestdataValue>(false));

        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v3, v2);
        assertThat(reachableValues.extractValuesAsList(v1)).containsExactlyInAnyOrder(v3, v2);
        assertThat(reachableValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v5, v4, v3, v1);
        assertThat(reachableValues.extractValuesAsList(v3)).containsExactlyInAnyOrder(v5, v4, v2, v1);
        assertThat(reachableValues.extractValuesAsList(v4)).containsExactlyInAnyOrder(v5, v3, v2);
        assertThat(reachableValues.extractValuesAsList(v5)).containsExactlyInAnyOrder(v4, v3, v2);
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
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0), null);
        assertThat(noSorterValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v3, v1, v4, v5);

        // Ascending sorter replaces the no sorter
        var ascendingSorter = new TestdataObjectSorter<TestdataAllowsUnassignedEntityProvidingSolution, TestdataValue>(true);
        var ascendingValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0), ascendingSorter);
        assertThat(ascendingValues).isNotSameAs(noSorterValues)
                .isSameAs(scoreDirector.getValueRangeManager()
                        .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0), ascendingSorter));
        assertThat(noSorterValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v1, v3, v4, v5);

        // Descending sorter
        var descendingSorter = new TestdataObjectSorter<TestdataAllowsUnassignedEntityProvidingSolution, TestdataValue>(false);
        var descendingValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0), descendingSorter);
        assertThat(descendingValues).isNotSameAs(ascendingValues)
                .isNotSameAs(noSorterValues)
                .isSameAs(scoreDirector.getValueRangeManager()
                        .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0), descendingSorter));
        assertThat(descendingValues.extractValuesAsList(v2)).containsExactlyInAnyOrder(v5, v4, v3, v1);

        // Null sorter returns the ascending sorter
        var otherNoSorterValues = scoreDirector.getValueRangeManager()
                .getReachableValues(entityDescriptor.getGenuineVariableDescriptorList().get(0), ascendingSorter);
        assertThat(otherNoSorterValues).isSameAs(ascendingValues);
    }
}
