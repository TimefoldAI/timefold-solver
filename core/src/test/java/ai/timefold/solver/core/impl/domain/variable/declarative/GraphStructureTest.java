package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.declarative.extended.TestdataDeclarativeExtendedSolution;
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataFollowerSolution;
import ai.timefold.solver.core.testdomain.declarative.simple_list.TestdataDeclarativeSimpleListSolution;

import org.junit.jupiter.api.Test;

public class GraphStructureTest {
    @Test
    public void simpleListStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataDeclarativeSimpleListSolution.buildSolutionDescriptor()))
                .isEqualTo(GraphStructure.SINGLE_DIRECTIONAL_PARENT);
    }

    @Test
    public void extendedSimpleListStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataDeclarativeExtendedSolution.buildSolutionDescriptor()))
                .isEqualTo(GraphStructure.SINGLE_DIRECTIONAL_PARENT);
    }

    @Test
    public void concurrentValuesStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataConcurrentSolution.buildSolutionDescriptor()))
                .isEqualTo(GraphStructure.ARBITRARY);
    }

    @Test
    public void followerStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataFollowerSolution.buildSolutionDescriptor()))
                .isEqualTo(GraphStructure.NO_DYNAMIC_EDGES);
    }

    @Test
    public void emptyStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataSolution.buildSolutionDescriptor()))
                .isEqualTo(GraphStructure.EMPTY);
    }
}
