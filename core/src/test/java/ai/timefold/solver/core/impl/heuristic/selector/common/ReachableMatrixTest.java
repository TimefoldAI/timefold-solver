package ai.timefold.solver.core.impl.heuristic.selector.common;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.junit.jupiter.api.Test;

class ReachableMatrixTest {

    @Test
    void testReachableEntities() {
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
                .getReachableValeMatrix(entityDescriptor.getGenuineListVariableDescriptor());

        assertThat(reachableValues.extractReachableEntities(v1)).containsExactlyInAnyOrder(a);
        assertThat(reachableValues.extractReachableEntities(v2)).containsExactlyInAnyOrder(a, b);
        assertThat(reachableValues.extractReachableEntities(v3)).containsExactlyInAnyOrder(a, b, c);
        assertThat(reachableValues.extractReachableEntities(v4)).containsExactlyInAnyOrder(c);
        assertThat(reachableValues.extractReachableEntities(v5)).containsExactlyInAnyOrder(c);
    }

    @Test
    void testReachableValues() {
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
                .getReachableValeMatrix(entityDescriptor.getGenuineListVariableDescriptor());

        assertThat(reachableValues.extractReachableValues(v1)).containsExactlyInAnyOrder(v2, v3);
        assertThat(reachableValues.extractReachableValues(v2)).containsExactlyInAnyOrder(v1, v3);
        assertThat(reachableValues.extractReachableValues(v3)).containsExactlyInAnyOrder(v1, v2, v4, v5);
        assertThat(reachableValues.extractReachableValues(v4)).containsExactlyInAnyOrder(v3, v5);
        assertThat(reachableValues.extractReachableValues(v5)).containsExactlyInAnyOrder(v3, v4);
    }
}
