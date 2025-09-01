package ai.timefold.solver.core.impl.heuristic.selector.common;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataMultiVarEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataMultiVarEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class ReachableEntitiesTest {

    @Test
    void testListVarReachableEntities() {
        var v1 = new TestdataListEntityProvidingValue("V1");
        var v2 = new TestdataListEntityProvidingValue("V2");
        var v3 = new TestdataListEntityProvidingValue("V3");
        var v4 = new TestdataListEntityProvidingValue("V4");
        var v5 = new TestdataListEntityProvidingValue("V5");
        var e1 = new TestdataListEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var e2 = new TestdataListEntityProvidingEntity("B", List.of(v2, v3), List.of(v3));
        var e3 = new TestdataListEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v4, v5));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataListEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var reachableEntities = scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataListEntityProvidingEntity.buildEntityDescriptor());

        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e1),
                reachableEntities.getReachableEntityOrdinal(e2))).isTrue();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e1),
                reachableEntities.getReachableEntityOrdinal(e3))).isFalse();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e2),
                reachableEntities.getReachableEntityOrdinal(e1))).isTrue();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e2),
                reachableEntities.getReachableEntityOrdinal(e3))).isTrue();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e3),
                reachableEntities.getReachableEntityOrdinal(e1))).isFalse();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e3),
                reachableEntities.getReachableEntityOrdinal(e2))).isTrue();
    }

    @Test
    void testSingleBasicVarReachableEntities() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var e1 = new TestdataAllowsUnassignedEntityProvidingEntity("A", List.of(v1, v2), v1);
        var e2 = new TestdataAllowsUnassignedEntityProvidingEntity("B", List.of(v2, v3), v3);
        var e3 = new TestdataAllowsUnassignedEntityProvidingEntity("C", List.of(v3, v4, v5), v4);
        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var reachableEntities = scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataAllowsUnassignedEntityProvidingEntity.buildEntityDescriptor());

        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e1),
                reachableEntities.getReachableEntityOrdinal(e2))).isTrue();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e1),
                reachableEntities.getReachableEntityOrdinal(e3))).isFalse();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e2),
                reachableEntities.getReachableEntityOrdinal(e1))).isTrue();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e2),
                reachableEntities.getReachableEntityOrdinal(e3))).isTrue();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e3),
                reachableEntities.getReachableEntityOrdinal(e1))).isFalse();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e3),
                reachableEntities.getReachableEntityOrdinal(e2))).isTrue();
    }

    @Test
    void testMultiBasicVarReachableEntities() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var e1 = new TestdataMultiVarEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var e2 = new TestdataMultiVarEntityProvidingEntity("B", List.of(v2, v3), List.of(v2, v3));
        var e3 = new TestdataMultiVarEntityProvidingEntity("C", List.of(v3, v4, v5), List.of(v3, v4, v5));
        var solution = new TestdataMultiVarEntityProvidingSolution();
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataMultiVarEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var reachableEntities = scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataMultiVarEntityProvidingEntity.buildEntityDescriptor());

        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e1),
                reachableEntities.getReachableEntityOrdinal(e2))).isTrue();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e1),
                reachableEntities.getReachableEntityOrdinal(e3))).isFalse();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e2),
                reachableEntities.getReachableEntityOrdinal(e1))).isTrue();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e2),
                reachableEntities.getReachableEntityOrdinal(e3))).isTrue();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e3),
                reachableEntities.getReachableEntityOrdinal(e1))).isFalse();
        assertThat(reachableEntities.isReachable(reachableEntities.getReachableEntityOrdinal(e3),
                reachableEntities.getReachableEntityOrdinal(e2))).isTrue();
    }

    @Test
    void testListIterator() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var e1 = new TestdataMultiVarEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var e2 = new TestdataMultiVarEntityProvidingEntity("B", List.of(v2, v3), List.of(v2, v3));
        var e3 = new TestdataMultiVarEntityProvidingEntity("C", List.of(v2, v4, v5), List.of(v4, v5));
        var solution = new TestdataMultiVarEntityProvidingSolution();
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataMultiVarEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var reachableEntities = scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataMultiVarEntityProvidingEntity.buildEntityDescriptor());

        // All items
        var iterator = reachableEntities.listIterator(0, 0);
        assertThat(iterator.hasPrevious()).isFalse();
        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isEqualTo(1);
        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isEqualTo(2);
        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterator.hasPrevious()).isTrue();
        assertThat(iterator.previous()).isEqualTo(1);
        assertThat(iterator.hasPrevious()).isFalse();

        // Only one item
        iterator = reachableEntities.listIterator(0, 1);
        assertThat(iterator.hasPrevious()).isFalse();
        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isEqualTo(2);
        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterator.hasPrevious()).isFalse();
    }

    @Test
    void testRandomIterator() {
        var v1 = new TestdataValue("V1");
        var v2 = new TestdataValue("V2");
        var v3 = new TestdataValue("V3");
        var v4 = new TestdataValue("V4");
        var v5 = new TestdataValue("V5");
        var e1 = new TestdataMultiVarEntityProvidingEntity("A", List.of(v1, v2), List.of(v1, v2));
        var e2 = new TestdataMultiVarEntityProvidingEntity("B", List.of(v2, v3), List.of(v2, v3));
        var e3 = new TestdataMultiVarEntityProvidingEntity("C", List.of(v2, v4, v5), List.of(v4, v5));
        var solution = new TestdataMultiVarEntityProvidingSolution();
        solution.setEntityList(List.of(e1, e2, e3));

        var scoreDirector = mockScoreDirector(TestdataMultiVarEntityProvidingSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var reachableEntities = scoreDirector.getValueRangeManager()
                .getReachableEntities(TestdataMultiVarEntityProvidingEntity.buildEntityDescriptor());

        var iterator = reachableEntities.randomIterator(0, new TestRandom(0, 0, 1));
        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isEqualTo(1);
        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isEqualTo(1);
        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isEqualTo(2);
        assertThat(iterator.hasNext()).isTrue();
    }
}
