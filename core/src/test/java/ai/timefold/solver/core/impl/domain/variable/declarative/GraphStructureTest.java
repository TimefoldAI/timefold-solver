package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.ARBITRARY;
import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE;
import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.EMPTY;
import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.SINGLE_DIRECTIONAL_PARENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentValue;
import ai.timefold.solver.core.testdomain.shadow.extended.TestdataDeclarativeExtendedBaseValue;
import ai.timefold.solver.core.testdomain.shadow.extended.TestdataDeclarativeExtendedSolution;
import ai.timefold.solver.core.testdomain.shadow.extended.TestdataDeclarativeExtendedSubclassValue;
import ai.timefold.solver.core.testdomain.shadow.follower.TestdataFollowerEntity;
import ai.timefold.solver.core.testdomain.shadow.follower.TestdataFollowerSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_directional_parent.TestdataMultiDirectionConcurrentEntity;
import ai.timefold.solver.core.testdomain.shadow.multi_directional_parent.TestdataMultiDirectionConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_directional_parent.TestdataMultiDirectionConcurrentValue;
import ai.timefold.solver.core.testdomain.shadow.multi_entity.TestdataMultiEntityDependencyEntity;
import ai.timefold.solver.core.testdomain.shadow.multi_entity.TestdataMultiEntityDependencySolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity.TestdataMultiEntityDependencyValue;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListSolution;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListValue;

import org.junit.jupiter.api.Test;

class GraphStructureTest {
    @Test
    void emptySimpleListStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataDeclarativeSimpleListSolution.buildSolutionDescriptor()))
                .hasFieldOrPropertyWithValue("structure", EMPTY);
    }

    @Test
    void simpleListStructure() {
        var entity = new TestdataDeclarativeSimpleListValue();
        assertThat(GraphStructure.determineGraphStructure(
                TestdataDeclarativeSimpleListSolution.buildSolutionDescriptor(), entity))
                .hasFieldOrPropertyWithValue("structure", SINGLE_DIRECTIONAL_PARENT)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS);
    }

    @Test
    void extendedSimpleListStructure() {
        var entity = new TestdataDeclarativeExtendedSubclassValue();
        assertThat(GraphStructure.determineGraphStructure(
                TestdataDeclarativeExtendedSolution.buildSolutionDescriptor(), entity))
                .hasFieldOrPropertyWithValue("structure", SINGLE_DIRECTIONAL_PARENT)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS);
    }

    @Test
    void extendedSimpleListStructureWithoutDeclarativeEntities() {
        var entity = new TestdataDeclarativeExtendedBaseValue();
        assertThat(GraphStructure.determineGraphStructure(
                TestdataDeclarativeExtendedSolution.buildSolutionDescriptor(), entity))
                .hasFieldOrPropertyWithValue("structure", EMPTY);
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
                .hasFieldOrPropertyWithValue("structure", ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE);
    }

    @Test
    void followerStructure() {
        var entity = new TestdataFollowerEntity();
        assertThat(GraphStructure.determineGraphStructure(
                TestdataFollowerSolution.buildSolutionDescriptor(), entity))
                .hasFieldOrPropertyWithValue("structure", ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE);
    }

    @Test
    void multiEntity() {
        var entity = new TestdataMultiEntityDependencyEntity();
        var value = new TestdataMultiEntityDependencyValue();
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiEntityDependencySolution.buildSolutionDescriptor(), entity, value))
                .hasFieldOrPropertyWithValue("structure", ARBITRARY);
    }

    @Test
    void multiDirectionalParents() {
        var entity = new TestdataMultiDirectionConcurrentEntity();
        var value = new TestdataMultiDirectionConcurrentValue();
        value.setConcurrentValueGroup(List.of(value));
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiDirectionConcurrentSolution.buildSolutionDescriptor(), entity, value))
                .hasFieldOrPropertyWithValue("structure", ARBITRARY);
    }

    @Test
    void multiDirectionalParentsEmptyGroups() {
        var entity = new TestdataMultiDirectionConcurrentEntity();
        var value = new TestdataMultiDirectionConcurrentValue();
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiDirectionConcurrentSolution.buildSolutionDescriptor(), entity, value))
                .hasFieldOrPropertyWithValue("structure", SINGLE_DIRECTIONAL_PARENT)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS);
    }

    @Test
    void emptyStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataSolution.buildSolutionDescriptor()))
                .hasFieldOrPropertyWithValue("structure", EMPTY);
    }
}
