package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentEntity;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentValue;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencyEntity;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencySolution;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencyValue;

import org.junit.jupiter.api.Test;

class VariableReferenceGraphBuilderTest {

    private static final SolutionDescriptor<TestdataDependencySolution> SOLUTION_DESCRIPTOR =
            TestdataDependencySolution.buildSolutionDescriptor();

    private int nextGroupId = 0;

    private <Solution_> VariableUpdaterInfo<Solution_> updaterOf(SolutionDescriptor<Solution_> solutionDescriptor,
            String variableName) {
        var descriptor = solutionDescriptor.getDeclarativeShadowVariableDescriptors().stream()
                .filter(variable -> variable.getVariableName().equals(variableName))
                .findFirst()
                .orElseThrow();
        return new VariableUpdaterInfo<>(
                descriptor.getVariableMetaModel(),
                nextGroupId++,
                descriptor,
                new ConsistencyTracker<Solution_>()
                        .getDeclarativeEntityConsistencyState(descriptor.getEntityDescriptor()),
                descriptor.getMemberAccessor(),
                descriptor.getCalculator());
    }

    private VariableUpdaterInfo<TestdataDependencySolution> sharedUpdaterOf(String variableName, Object... entities) {
        var descriptor = SOLUTION_DESCRIPTOR.getDeclarativeShadowVariableDescriptors().stream()
                .filter(variable -> variable.getVariableName().equals(variableName))
                .findFirst()
                .orElseThrow();
        return new VariableUpdaterInfo<>(
                descriptor.getVariableMetaModel(),
                nextGroupId++,
                descriptor,
                new ConsistencyTracker<TestdataDependencySolution>()
                        .getDeclarativeEntityConsistencyState(descriptor.getEntityDescriptor()),
                descriptor.getMemberAccessor(),
                descriptor.getCalculator()).withGroupEntities(entities);
    }

    private <Solution_> GraphNode<Solution_> addNode(VariableReferenceGraphBuilder<Solution_> builder,
            Object entity, String variableName, SolutionDescriptor<Solution_> solutionDescriptor) {
        var updater = updaterOf(solutionDescriptor, variableName);
        builder.addVariableReferenceEntity(entity, false, updater);
        return builder.lookupOrError(updater.id(), entity);
    }

    private GraphNode<TestdataDependencySolution> addNode(VariableReferenceGraphBuilder<TestdataDependencySolution> builder,
            TestdataDependencyValue entity, String variableName) {
        return addNode(builder, entity, variableName, SOLUTION_DESCRIPTOR);
    }

    private <Solution_> GraphNode<Solution_> addDynamicNode(VariableReferenceGraphBuilder<Solution_> builder,
            Object entity, String variableName, SolutionDescriptor<Solution_> solutionDescriptor) {
        var updater = updaterOf(solutionDescriptor, variableName);
        builder.addVariableReferenceEntity(entity, true, updater);
        return builder.lookupOrError(updater.id(), entity);
    }

    private GraphNode<TestdataDependencySolution> addSharedNode(
            VariableReferenceGraphBuilder<TestdataDependencySolution> builder,
            TestdataDependencyValue entity, String variableName, Object... groupedEntities) {
        var updater = sharedUpdaterOf(variableName, groupedEntities);
        builder.addVariableReferenceEntity(entity, false, updater);
        return builder.lookupOrError(updater.id(), entity);
    }

    @Test
    void mergesNodesOfSameEntityInVariableTopologicalOrder() {
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.<TestdataDependencySolution> empty());
        var startTime = LocalDate.of(2000, 1, 1).atStartOfDay();
        var entity = new TestdataDependencyEntity(startTime);
        var valueA = new TestdataDependencyValue("a", Duration.ofMinutes(1));
        var valueB = new TestdataDependencyValue("b", Duration.ofMinutes(2));

        valueA.setEntity(entity);
        valueB.setEntity(entity);
        valueB.setPreviousValue(valueA);

        // Add endTime before startTime so the merge cannot just keep insertion order.
        var aStart = addNode(builder, valueA, "startTime");
        var aEnd = addNode(builder, valueA, "endTime");
        var bStart = addNode(builder, valueB, "startTime");
        var bEnd = addNode(builder, valueB, "endTime");
        builder.addFixedEdge(aStart, aEnd);
        builder.addFixedEdge(bStart, bEnd);
        builder.addInitialDynamicEdge(aStart, bEnd);

        var graph = builder.build(DefaultTopologicalOrderGraph::new, false);

        assertThat(builder.nodeList).hasSize(2);
        for (var node : builder.nodeList) {
            assertThat(node.variableReferences()).hasSize(2);
            assertThat(node.variableReferences().get(0).id().name()).isEqualTo("startTime");
            assertThat(node.variableReferences().get(1).id().name()).isEqualTo("endTime");
        }

        graph.updateChanged();
        assertThat(valueA.getStartTime()).isEqualTo(startTime);
        assertThat(valueA.getEndTime()).isEqualTo(startTime.plusMinutes(1));
        assertThat(valueB.getStartTime()).isEqualTo(startTime.plusMinutes(1));
        assertThat(valueB.getEndTime()).isEqualTo(startTime.plusMinutes(3));
    }

    @Test
    void doesNotMergeNodesIfMergingCreatesFixedLoop() {
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.<TestdataDependencySolution> empty());
        var entityA = new TestdataDependencyValue("a", Duration.ofMinutes(1));
        var entityX = new TestdataDependencyValue("x", Duration.ofMinutes(2));
        var aStart = addNode(builder, entityA, "startTime");
        var aEnd = addNode(builder, entityA, "endTime");
        var xStart = addNode(builder, entityX, "startTime");
        var xEnd = addNode(builder, entityX, "endTime");

        builder.addFixedEdge(aStart, aEnd);
        builder.addFixedEdge(xStart, xEnd);
        builder.addFixedEdge(xStart, aEnd);
        builder.addFixedEdge(aStart, xEnd);

        builder.build(DefaultTopologicalOrderGraph::new, false);

        assertThat(builder.nodeList).hasSize(4);

        //a.endTime and x.endTime cannot be calculated until both a.startTime and x.startTime are
        for (var node : builder.nodeList) {
            assertThat(node.variableReferences()).hasSize(1);
        }
    }

    @Test
    void mergesNodesThatAreAlreadyInLoop() {
        // The unmerged graph has the loop
        // a.startTime -> a.endTime -> b.startTime -> b.endTime -> a.startTime,
        // through the initial dynamic edge a.endTime -> b.startTime.
        // The merge only considers the fixed edges, which are acyclic,
        // so a's nodes and b's nodes are merged.
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.<TestdataDependencySolution> empty());
        var entityA = new TestdataDependencyValue("a", Duration.ofMinutes(1));
        var entityB = new TestdataDependencyValue("b", Duration.ofMinutes(2));
        var aStart = addNode(builder, entityA, "startTime");
        var aEnd = addNode(builder, entityA, "endTime");
        var bStart = addNode(builder, entityB, "startTime");
        var bEnd = addNode(builder, entityB, "endTime");
        builder.addFixedEdge(aStart, aEnd);
        builder.addInitialDynamicEdge(aEnd, bStart);
        builder.addFixedEdge(bStart, bEnd);
        builder.addFixedEdge(bEnd, aStart);

        builder.build(DefaultTopologicalOrderGraph::new, false);

        assertThat(builder.nodeList).hasSize(2);
        for (var node : builder.nodeList) {
            assertThat(node.variableReferences()).hasSize(2);
        }
    }

    @Test
    void doesNotMergeNodesIfMergingCreatesLoopViaLongerPath() {
        // a.startTime -> a.endTime is a direct edge, but there is also
        // a.startTime -> x.startTime -> x.endTime -> a.endTime.
        // Merging either entity's nodes together with the other's would create
        // the loop a <-> x, so neither entity's nodes are merged.
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.<TestdataDependencySolution> empty());
        var entityA = new TestdataDependencyValue("a", Duration.ofMinutes(1));
        var entityX = new TestdataDependencyValue("x", Duration.ofMinutes(2));
        var aStart = addNode(builder, entityA, "startTime");
        var aEnd = addNode(builder, entityA, "endTime");
        var xStart = addNode(builder, entityX, "startTime");
        var xEnd = addNode(builder, entityX, "endTime");
        builder.addFixedEdge(aStart, aEnd);
        builder.addFixedEdge(aStart, xStart);
        builder.addFixedEdge(xStart, xEnd);
        builder.addFixedEdge(xEnd, aEnd);

        builder.build(DefaultTopologicalOrderGraph::new, false);

        assertThat(builder.nodeList).hasSize(4);
        for (var node : builder.nodeList) {
            assertThat(node.variableReferences()).hasSize(1);
        }
    }

    @Test
    void doesNotMergeAcrossAliasGroups() {
        // a and b start are calculated together; c is calculated by itself.
        // a.end and b.end cannot be merged with (a&b).start, but c.start and c.end can be merged
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.<TestdataDependencySolution> empty());
        var startTime = LocalDate.of(2000, 1, 1).atStartOfDay();
        var entity = new TestdataDependencyEntity(startTime);
        var valueA = new TestdataDependencyValue("a", Duration.ofMinutes(1));
        var valueB = new TestdataDependencyValue("b", Duration.ofMinutes(2));
        var valueC = new TestdataDependencyValue("c", Duration.ofMinutes(3));

        valueA.setEntity(entity);
        valueB.setEntity(entity);
        valueC.setEntity(entity);

        valueA.setPreviousValue(valueC);
        valueB.setPreviousValue(valueC);

        var abStartShared = addSharedNode(builder, valueA, "startTime", valueA, valueB);
        var aEnd = addNode(builder, valueA, "endTime");
        // define C in the middle so a stale index cause an exception
        var cStart = addNode(builder, valueC, "startTime");
        var cEnd = addNode(builder, valueC, "endTime");
        var bEnd = addNode(builder, valueB, "endTime");

        builder.addFixedEdge(abStartShared, aEnd);
        builder.addFixedEdge(abStartShared, bEnd);
        builder.addFixedEdge(cStart, cEnd);
        builder.addInitialDynamicEdge(cEnd, abStartShared);

        var graph = builder.build(DefaultTopologicalOrderGraph::new, false);

        assertThat(builder.nodeList).hasSize(4);

        for (var node : builder.nodeList) {
            if (node.entity() == valueC) {
                assertThat(node.variableReferences()).hasSize(2);
            } else {
                if (node.variableReferences().getFirst().id().name().equals("startTime")) {
                    assertThat(node.variableReferences()).hasSize(1);
                    assertThat(node.groupEntityIds()).hasSize(2);
                } else {
                    assertThat(node.variableReferences()).hasSize(1);
                    assertThat(node.groupEntityIds()).isNull();
                }
            }
        }

        graph.updateChanged();
        assertThat(valueC.getStartTime()).isEqualTo(startTime);
        assertThat(valueC.getEndTime()).isEqualTo(startTime.plusMinutes(3));
        assertThat(valueA.getStartTime()).isEqualTo(startTime.plusMinutes(3));
        assertThat(valueA.getEndTime()).isEqualTo(startTime.plusMinutes(4));
        assertThat(valueB.getStartTime()).isEqualTo(startTime.plusMinutes(3));
        assertThat(valueB.getEndTime()).isEqualTo(startTime.plusMinutes(5));
    }

    @Test
    void splitNodesOfSameEntityByNodesWithDynamicEdges() {
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.<TestdataConcurrentSolution> empty());
        var entity = new TestdataConcurrentEntity("entity");
        var valueA = new TestdataConcurrentValue("a");
        var valueB = new TestdataConcurrentValue("b");

        valueA.setEntity(entity);
        valueB.setEntity(entity);
        valueB.setPreviousValue(valueA);

        // Add endTime before startTime so the merge cannot just keep insertion order.
        var solutionDescriptor = TestdataConcurrentSolution.buildSolutionDescriptor();
        var aReady = addNode(builder, valueA, "serviceReadyTime", solutionDescriptor);
        var aStart = addDynamicNode(builder, valueA, "serviceStartTime", solutionDescriptor);
        var aEnd = addNode(builder, valueA, "serviceFinishTime", solutionDescriptor);
        var bReady = addNode(builder, valueB, "serviceReadyTime", solutionDescriptor);
        var bStart = addNode(builder, valueB, "serviceStartTime", solutionDescriptor);
        var bEnd = addDynamicNode(builder, valueB, "serviceFinishTime", solutionDescriptor);
        builder.addFixedEdge(aReady, aStart);
        builder.addFixedEdge(aStart, aEnd);
        builder.addFixedEdge(bReady, bStart);
        builder.addFixedEdge(bStart, bEnd);
        builder.addInitialDynamicEdge(aEnd, bStart);

        var graph = builder.build(DefaultTopologicalOrderGraph::new, false);

        assertThat(builder.nodeList).hasSize(4);
        for (var node : builder.nodeList) {
            // valueA is split at startTime
            if (node.entity() == valueA) {
                if (node.variableReferences().size() == 1) {
                    assertThat(node.variableReferences().getFirst().id().name())
                            .isEqualTo("serviceReadyTime");
                } else {
                    assertThat(node.variableReferences().size()).isEqualTo(2);
                    assertThat(node.variableReferences().getFirst().id().name())
                            .isEqualTo("serviceStartTime");
                    assertThat(node.variableReferences().getLast().id().name())
                            .isEqualTo("serviceFinishTime");
                }
            } else {
                // valueB is split at endTime
                if (node.variableReferences().size() == 1) {
                    assertThat(node.variableReferences().getFirst().id().name())
                            .isEqualTo("serviceFinishTime");
                } else {
                    assertThat(node.variableReferences().size()).isEqualTo(2);
                    assertThat(node.variableReferences().getFirst().id().name())
                            .isEqualTo("serviceReadyTime");
                    assertThat(node.variableReferences().getLast().id().name())
                            .isEqualTo("serviceStartTime");
                }
            }
        }

        graph.updateChanged();
        var startTime = TestdataConcurrentValue.BASE_START_TIME;
        assertThat(valueA.getServiceReadyTime()).isEqualTo(startTime);
        assertThat(valueA.getServiceStartTime()).isEqualTo(startTime);
        assertThat(valueA.getServiceFinishTime()).isEqualTo(startTime.plusMinutes(30L));
        assertThat(valueB.getServiceReadyTime()).isEqualTo(startTime.plusMinutes(60L));
        assertThat(valueB.getServiceStartTime()).isEqualTo(startTime.plusMinutes(60L));
        assertThat(valueB.getServiceFinishTime()).isEqualTo(startTime.plusMinutes(90L));
    }
}
