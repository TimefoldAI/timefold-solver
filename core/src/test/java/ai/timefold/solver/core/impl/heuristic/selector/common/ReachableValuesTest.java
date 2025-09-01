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

        var iterator = reachableValues.getOriginalEntityIterator(reachableValues.getValueOrdinal(v1));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getEntityOrdinal(a));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalEntityIterator(reachableValues.getValueOrdinal(v2));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getEntityOrdinal(a));
        assertThat(iterator.next()).isEqualTo(reachableValues.getEntityOrdinal(b));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalEntityIterator(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getEntityOrdinal(a));
        assertThat(iterator.next()).isEqualTo(reachableValues.getEntityOrdinal(b));
        assertThat(iterator.next()).isEqualTo(reachableValues.getEntityOrdinal(c));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalEntityIterator(reachableValues.getValueOrdinal(v4));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getEntityOrdinal(c));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalEntityIterator(reachableValues.getValueOrdinal(v5));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getEntityOrdinal(c));
        assertThat(iterator.hasNext()).isFalse();

        // Only origin
        assertThat(reachableValues.isEntityReachable(reachableValues.getValueOrdinal(v1), reachableValues.getEntityOrdinal(a)))
                .isTrue();
        assertThat(reachableValues.isEntityReachable(reachableValues.getValueOrdinal(v1), reachableValues.getEntityOrdinal(b)))
                .isFalse();
        assertThat(reachableValues.isEntityReachable(reachableValues.getValueOrdinal(v1), reachableValues.getEntityOrdinal(c)))
                .isFalse();
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

        var iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v1));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v2));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v2));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v1));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v1));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v2));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v4));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v5));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v4));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v5));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v5));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v4));
        assertThat(iterator.hasNext()).isFalse();

        // Only origin
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), reachableValues.getValueOrdinal(v2)))
                .isTrue();
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), reachableValues.getValueOrdinal(v3)))
                .isTrue();
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), reachableValues.getValueOrdinal(v4)))
                .isFalse();
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), reachableValues.getValueOrdinal(v5)))
                .isFalse();

        // Null value is not accepted because the setting allowUnassigned is false
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), -1))
                .isFalse();
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

        var iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v1));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v2));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v2));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v1));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v1));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v2));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v4));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v5));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v4));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v5));
        assertThat(iterator.hasNext()).isFalse();

        iterator = reachableValues.getOriginalValueIterator(reachableValues.getValueOrdinal(v5));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v3));
        assertThat(iterator.next()).isEqualTo(reachableValues.getValueOrdinal(v4));
        assertThat(iterator.hasNext()).isFalse();

        // Only origin
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), reachableValues.getValueOrdinal(v2)))
                .isTrue();
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), reachableValues.getValueOrdinal(v3)))
                .isTrue();
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), reachableValues.getValueOrdinal(v4)))
                .isFalse();
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), reachableValues.getValueOrdinal(v5)))
                .isFalse();

        // Null value is not accepted because the setting allowUnassigned is false
        assertThat(reachableValues.isValueReachable(reachableValues.getValueOrdinal(v1), -1)).isTrue();
    }
}
