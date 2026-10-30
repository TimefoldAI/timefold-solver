package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencySolution;
import ai.timefold.solver.core.testdomain.shadow.dependency.TestdataDependencyValue;

import org.junit.jupiter.api.Test;

class VariableReferenceGraphBuilderTest {

    private static final SolutionDescriptor<TestdataDependencySolution> SOLUTION_DESCRIPTOR =
            TestdataDependencySolution.buildSolutionDescriptor();

    private int nextGroupId = 0;

    private VariableUpdaterInfo<TestdataDependencySolution> updaterOf(String variableName) {
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

    private GraphNode<TestdataDependencySolution> addNode(VariableReferenceGraphBuilder<TestdataDependencySolution> builder,
            TestdataDependencyValue entity, String variableName) {
        var updater = updaterOf(variableName);
        builder.addVariableReferenceEntity(entity, updater);
        return builder.lookupOrError(updater.id(), entity);
    }

    private GraphNode<TestdataDependencySolution> addSharedNode(
            VariableReferenceGraphBuilder<TestdataDependencySolution> builder,
            TestdataDependencyValue entity, String variableName, Object... groupedEntities) {
        var updater = sharedUpdaterOf(variableName, groupedEntities);
        builder.addVariableReferenceEntity(entity, updater);
        return builder.lookupOrError(updater.id(), entity);
    }

    @Test
    void mergesNodesOfSameEntityInVariableTopologicalOrder() {
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.<TestdataDependencySolution> empty());
        var entityA = new TestdataDependencyValue("a", Duration.ofMinutes(1));
        var entityB = new TestdataDependencyValue("b", Duration.ofMinutes(2));
        // Add endTime before startTime so the merge cannot just keep insertion order.
        var aStart = addNode(builder, entityA, "startTime");
        var aEnd = addNode(builder, entityA, "endTime");
        var bStart = addNode(builder, entityB, "startTime");
        var bEnd = addNode(builder, entityB, "endTime");
        builder.addFixedEdge(aStart, aEnd);
        builder.addFixedEdge(bStart, bEnd);

        builder.build(DefaultTopologicalOrderGraph::new, false);

        assertThat(builder.nodeList).hasSize(2);
        for (var node : builder.nodeList) {
            assertThat(node.variableReferences()).hasSize(2);
            assertThat(node.variableReferences().get(0).id().name()).isEqualTo("startTime");
            assertThat(node.variableReferences().get(1).id().name()).isEqualTo("endTime");
        }
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
    void doesNotMergeEntityWithMixedLoopStatus() {
        // a.startTime and b.startTime form the loop a.startTime -> b.startTime -> a.startTime,
        // but a.endTime is not in the loop.
        // Merging a's nodes would make a.endTime looped, so a must not be merged.
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.<TestdataDependencySolution> empty());
        var entityA = new TestdataDependencyValue("a", Duration.ofMinutes(1));
        var entityB = new TestdataDependencyValue("b", Duration.ofMinutes(2));
        var aStart = addNode(builder, entityA, "startTime");
        var aEnd = addNode(builder, entityA, "endTime");
        var bStart = addNode(builder, entityB, "startTime");
        builder.addInitialDynamicEdge(aStart, bStart);
        builder.addFixedEdge(bStart, aStart);

        builder.build(DefaultTopologicalOrderGraph::new, false);

        assertThat(builder.nodeList).hasSize(3);
    }

    @Test
    void doesNotMergeAcrossAliasGroups() {
        // a and b start are calculated together; c is calculated by itself.
        // a.end and b.end cannot be merged with (a&b).start, but c.start and c.end can be merged
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.<TestdataDependencySolution> empty());
        var entityA = new TestdataDependencyValue("a", Duration.ofMinutes(1));
        var entityB = new TestdataDependencyValue("b", Duration.ofMinutes(2));
        var entityC = new TestdataDependencyValue("c", Duration.ofMinutes(3));
        var abStartShared = addSharedNode(builder, entityA, "startTime", entityA, entityB);
        var cStart = addNode(builder, entityC, "startTime");
        var aEnd = addNode(builder, entityA, "endTime");
        var bEnd = addNode(builder, entityB, "endTime");
        var cEnd = addNode(builder, entityC, "endTime");

        builder.addFixedEdge(abStartShared, aEnd);
        builder.addFixedEdge(abStartShared, bEnd);
        builder.addFixedEdge(cStart, cEnd);

        builder.build(DefaultTopologicalOrderGraph::new, false);

        assertThat(builder.nodeList).hasSize(4);

        for (var node : builder.nodeList) {
            if (node.entity() == entityC) {
                assertThat(node.variableReferences()).hasSize(2);
            } else {
                if (node.variableReferences().getFirst().variableDescriptor().getVariableName().equals("startTime")) {
                    assertThat(node.variableReferences()).hasSize(1);
                    assertThat(node.groupEntityIds()).hasSize(2);
                } else {
                    assertThat(node.variableReferences()).hasSize(1);
                    assertThat(node.groupEntityIds()).isNull();
                }
            }
        }
    }
}
