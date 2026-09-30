package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.ARBITRARY;
import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE;
import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.EMPTY;
import static ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure.LIST_ELEMENT_BLOCK;
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
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataAlignedListElementEntity;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataAlignedListElementSolution;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataAlignedListElementValue;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataListElementEntity;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataListElementSolution;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataListElementValue;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataMixedListElementEntity;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataMixedListElementSolution;
import ai.timefold.solver.core.testdomain.shadow.list_element.TestdataMixedListElementValue;
import ai.timefold.solver.core.testdomain.shadow.multi_directional_parent.TestdataMultiDirectionConcurrentEntity;
import ai.timefold.solver.core.testdomain.shadow.multi_directional_parent.TestdataMultiDirectionConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_directional_parent.TestdataMultiDirectionConcurrentValue;
import ai.timefold.solver.core.testdomain.shadow.multi_entity.TestdataMultiEntityDependencyEntity;
import ai.timefold.solver.core.testdomain.shadow.multi_entity.TestdataMultiEntityDependencySolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity.TestdataMultiEntityDependencyValue;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced.TestdataMultiEntityChainElementSourcedSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced.TestdataMultiEntityChainElementSourcedVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced.TestdataMultiEntityChainElementSourcedVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_extended.TestdataMultiEntityChainExtendedPriorityVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_extended.TestdataMultiEntityChainExtendedSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_extended.TestdataMultiEntityChainExtendedVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_extended.TestdataMultiEntityChainExtendedVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_loop.TestdataMultiEntityChainLoopSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_loop.TestdataMultiEntityChainLoopVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_loop.TestdataMultiEntityChainLoopVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next.TestdataMultiEntityChainNextVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_slack.TestdataMultiEntityChainSlackSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_slack.TestdataMultiEntityChainSlackVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_slack.TestdataMultiEntityChainSlackVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_watched.TestdataMultiEntityChainWatchedSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_watched.TestdataMultiEntityChainWatchedVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_watched.TestdataMultiEntityChainWatchedVisit;
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
    void listElementStructure() {
        var entity = new TestdataListElementEntity("e1");
        var value = new TestdataListElementValue("v1");
        assertThat(GraphStructure.determineGraphStructure(
                TestdataListElementSolution.buildSolutionDescriptor(), entity, value))
                .hasFieldOrPropertyWithValue("structure", LIST_ELEMENT_BLOCK)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS)
                .hasFieldOrPropertyWithValue("blockedElementClass", TestdataListElementValue.class);
    }

    @Test
    void listElementStructureWithGenuineVariableOnElement() {
        var entity = new TestdataMixedListElementEntity("e1");
        var value = new TestdataMixedListElementValue("v1");
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMixedListElementSolution.buildSolutionDescriptor(), entity, value))
                .hasFieldOrPropertyWithValue("structure", ARBITRARY);
    }

    @Test
    void listElementStructureWithAlignmentKey() {
        var entity = new TestdataAlignedListElementEntity("e1");
        var value = new TestdataAlignedListElementValue("v1", "g1");
        // An alignment key updates every entity of its group at once,
        // which the block node's entity at a time updates cannot do.
        assertThat(GraphStructure.determineGraphStructure(
                TestdataAlignedListElementSolution.buildSolutionDescriptor(), entity, value))
                .hasFieldOrPropertyWithValue("structure", ARBITRARY);
    }

    @Test
    void multiEntityChainStructure() {
        var vehicleA = new TestdataMultiEntityChainVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainVehicle("B", 0);
        vehicleB.setPreviousVehicles(List.of(vehicleA));
        var visit = new TestdataMultiEntityChainVisit("v1");
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiEntityChainSolution.buildSolutionDescriptor(), vehicleA, vehicleB, visit))
                .hasFieldOrPropertyWithValue("structure", LIST_ELEMENT_BLOCK)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS)
                .hasFieldOrPropertyWithValue("blockedElementClass", TestdataMultiEntityChainVisit.class);
    }

    @Test
    void multiEntityChainNextStructure() {
        var vehicle = new TestdataMultiEntityChainNextVehicle("A", 100);
        var visit = new TestdataMultiEntityChainNextVisit("v1");
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiEntityChainNextSolution.buildSolutionDescriptor(), vehicle, visit))
                .hasFieldOrPropertyWithValue("structure", LIST_ELEMENT_BLOCK)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.NEXT)
                .hasFieldOrPropertyWithValue("blockedElementClass", TestdataMultiEntityChainNextVisit.class);
    }

    @Test
    void multiEntityChainWithFactCollectionOfElements() {
        var vehicle = new TestdataMultiEntityChainWatchedVehicle("A", 0);
        var watcher = new TestdataMultiEntityChainWatchedVehicle("W", 0);
        var visit = new TestdataMultiEntityChainWatchedVisit("v1", 1);
        watcher.getWatchedVisits().add(visit);
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiEntityChainWatchedSolution.buildSolutionDescriptor(), vehicle, watcher, visit))
                .hasFieldOrPropertyWithValue("structure", ARBITRARY);
    }

    @Test
    void multiEntityChainWithPlanningVariableChainedVehicles() {
        var vehicle = new TestdataMultiEntityChainLoopVehicle("A", 0);
        var visit = new TestdataMultiEntityChainLoopVisit("v1", 1);
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiEntityChainLoopSolution.buildSolutionDescriptor(), vehicle, visit))
                .hasFieldOrPropertyWithValue("structure", LIST_ELEMENT_BLOCK)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS)
                .hasFieldOrPropertyWithValue("blockedElementClass", TestdataMultiEntityChainLoopVisit.class);
    }

    @Test
    void multiEntityChainWithElementSourcedEndTime() {
        var vehicle = new TestdataMultiEntityChainElementSourcedVehicle("A", 0);
        var visit = new TestdataMultiEntityChainElementSourcedVisit("v1", 1);
        // The vehicle's endTime never reads its own startTime, so nothing but the block node's edges
        // orders it after the route it summarizes.
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiEntityChainElementSourcedSolution.buildSolutionDescriptor(), vehicle, visit))
                .hasFieldOrPropertyWithValue("structure", LIST_ELEMENT_BLOCK)
                .hasFieldOrPropertyWithValue("direction", ParentVariableType.PREVIOUS)
                .hasFieldOrPropertyWithValue("blockedElementClass", TestdataMultiEntityChainElementSourcedVisit.class);
    }

    @Test
    void multiEntityChainWithElementsReadingAPostChainVariable() {
        var vehicle = new TestdataMultiEntityChainSlackVehicle("A");
        var visit = new TestdataMultiEntityChainSlackVisit("v1", 1);
        // The detection only judges the shape of the model; the loop the block node would close
        // with its vehicle's end time is left to the build, which falls back to the arbitrary graph.
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiEntityChainSlackSolution.buildSolutionDescriptor(), vehicle, visit))
                .hasFieldOrPropertyWithValue("structure", LIST_ELEMENT_BLOCK)
                .hasFieldOrPropertyWithValue("blockedElementClass", TestdataMultiEntityChainSlackVisit.class);
    }

    @Test
    void emptyStructure() {
        assertThat(GraphStructure.determineGraphStructure(
                TestdataSolution.buildSolutionDescriptor()))
                .hasFieldOrPropertyWithValue("structure", EMPTY);
    }

    @Test
    void multiEntityChainWithDeclarativeVisitSubclass() {
        var vehicle = new TestdataMultiEntityChainExtendedVehicle("A", 0);
        var visit = new TestdataMultiEntityChainExtendedVisit("v1", 1);
        var priorityVisit = new TestdataMultiEntityChainExtendedPriorityVisit("p1", 1, 10);
        // The block node's walk applies every element updater to every element,
        // so a declarative variable declared on a visit subclass falls back to the arbitrary graph.
        assertThat(GraphStructure.determineGraphStructure(
                TestdataMultiEntityChainExtendedSolution.buildSolutionDescriptor(), vehicle, visit, priorityVisit))
                .hasFieldOrPropertyWithValue("structure", ARBITRARY);
    }
}
