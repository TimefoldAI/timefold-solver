package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.ARBITRARY;
import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.EMPTY;
import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.NO_DYNAMIC_EDGES;
import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.SINGLE_DIRECTIONAL_PARENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.declarative.concurrent.TestdataConcurrentValue;
import ai.timefold.solver.core.testdomain.declarative.extended.TestdataDeclarativeExtendedSolution;
import ai.timefold.solver.core.testdomain.declarative.follower.TestdataFollowerSolution;
import ai.timefold.solver.core.testdomain.declarative.simple_chained.TestdataChainedSimpleVarSolution;
import ai.timefold.solver.core.testdomain.declarative.simple_list.TestdataDeclarativeSimpleListSolution;

import org.junit.jupiter.api.Test;

class GraphStructureTest {
    @Test
    void simpleListStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataDeclarativeSimpleListSolution.buildSolutionDescriptor()))
                .hasFieldOrPropertyWithValue("structure", SINGLE_DIRECTIONAL_PARENT)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS);
    }

    @Test
    void simpleChainedStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataChainedSimpleVarSolution.buildSolutionDescriptor()))
                .hasFieldOrPropertyWithValue("structure", ARBITRARY);
    }

    @Test
    void extendedSimpleListStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataDeclarativeExtendedSolution.buildSolutionDescriptor()))
                .hasFieldOrPropertyWithValue("structure", SINGLE_DIRECTIONAL_PARENT)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS);
    }

    @Test
    void concurrentValuesStructureWithoutGroups() {
        var value1 = new TestdataConcurrentValue("v1");
        var value2 = new TestdataConcurrentValue("v2");
        value2.setConcurrentValueGroup(Collections.emptyList());
        assertThat(GraphStructure.determineGraphStructure(
                TestdataConcurrentSolution.buildSolutionDescriptor(),
                value1, value2))
                .hasFieldOrPropertyWithValue("structure", SINGLE_DIRECTIONAL_PARENT)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS);
    }

    @Test
    void concurrentValuesStructureWithGroups() {
        var value1 = new TestdataConcurrentValue("v1");
        var value2 = new TestdataConcurrentValue("v2");
        var group = List.of(value1, value2);
        value2.setConcurrentValueGroup(group);
        assertThat(GraphStructure.determineGraphStructure(
                TestdataConcurrentSolution.buildSolutionDescriptor(),
                value1, value2))
                .hasFieldOrPropertyWithValue("structure", ARBITRARY);
    }

    @Test
    void followerStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataFollowerSolution.buildSolutionDescriptor()))
                .hasFieldOrPropertyWithValue("structure", NO_DYNAMIC_EDGES);
    }

    @Test
    void emptyStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataSolution.buildSolutionDescriptor()))
                .hasFieldOrPropertyWithValue("structure", EMPTY);
    }
}
