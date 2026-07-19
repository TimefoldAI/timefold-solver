package ai.timefold.solver.core.impl.domain.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.function.QuadConsumer;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.GraphNode;
import ai.timefold.solver.core.impl.domain.variable.declarative.TopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableUpdaterInfo;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.NeighborhoodNotifier;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentEntity;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentValue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ShadowVariableSupportTest {

    private static class MockTopologicalOrderGraph extends DefaultTopologicalOrderGraph implements TopologicalOrderGraph {
        Object[] nodeToEntities;
        VariableMetaModel<?, ?, ?>[][] nodeToVariableMetamodel;

        public MockTopologicalOrderGraph(int size) {
            super(size);
            nodeToEntities = new Object[size];
            nodeToVariableMetamodel = new VariableMetaModel[size][];
        }

        @Override
        public <Solution_> void withNodeData(List<GraphNode<Solution_>> nodes) {
            nodeToEntities = nodes.stream().map(GraphNode::entity).toArray(Object[]::new);
            nodeToVariableMetamodel = nodes.stream()
                    .map(e -> e.variableReferences().stream()
                            .map(VariableUpdaterInfo::id)
                            .toArray(VariableMetaModel[]::new))
                    .toArray(VariableMetaModel[][]::new);
        }

        public void addEdge(VariableMetaModel<?, ?, ?> fromId, Object fromEntity, VariableMetaModel<?, ?, ?> toId,
                Object toEntity) {
            // Mock to spy invocations
        }

        public void removeEdge(VariableMetaModel<?, ?, ?> fromId, Object fromEntity, VariableMetaModel<?, ?, ?> toId,
                Object toEntity) {
            // Mock to spy invocations
        }

        @Override
        public void addEdge(int fromNode, int toNode) {
            super.addEdge(fromNode, toNode);
            addEdge(nodeToVariableMetamodel[fromNode][nodeToVariableMetamodel[fromNode].length - 1], nodeToEntities[fromNode],
                    nodeToVariableMetamodel[toNode][0],
                    nodeToEntities[toNode]);
        }

        @Override
        public void removeEdge(int fromNode, int toNode) {
            super.removeEdge(fromNode, toNode);
            removeEdge(nodeToVariableMetamodel[fromNode][nodeToVariableMetamodel[fromNode].length - 1],
                    nodeToEntities[fromNode], nodeToVariableMetamodel[toNode][0],
                    nodeToEntities[toNode]);
        }
    }

    @Test
    void shadowVariableListGraphEvents() {
        var solutionDescriptor = TestdataConcurrentSolution.buildSolutionDescriptor();
        @SuppressWarnings("unchecked")
        var scoreDirector = (InnerScoreDirector<TestdataConcurrentSolution, HardSoftScore>) mock(InnerScoreDirector.class);
        @SuppressWarnings("unchecked")
        var neighborhoodNotifier = (NeighborhoodNotifier<TestdataConcurrentSolution>) Mockito.mock(NeighborhoodNotifier.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        when(scoreDirector.getNeighborhoodNotifier()).thenReturn(neighborhoodNotifier);
        var valueRangeManager = new ValueRangeManager<>(solutionDescriptor);
        when(scoreDirector.getValueRangeManager()).thenReturn(valueRangeManager);

        AtomicReference<MockTopologicalOrderGraph> graphReference = new AtomicReference<>(null);
        ShadowVariableSupport<TestdataConcurrentSolution> shadowVariableSupport;

        var vehicle1 = new TestdataConcurrentEntity("1");
        var vehicle2 = new TestdataConcurrentEntity("2");
        var vehicle3 = new TestdataConcurrentEntity("3");

        var visitA1 = new TestdataConcurrentValue("a1");
        var visitA2 = new TestdataConcurrentValue("a2");

        var visitB1 = new TestdataConcurrentValue("b1");
        var visitB2 = new TestdataConcurrentValue("b2");
        var visitB3 = new TestdataConcurrentValue("b3");

        var visitC = new TestdataConcurrentValue("c");

        var groupA = List.of(visitA1, visitA2);
        visitA1.setConcurrentValueGroup(groupA);
        visitA2.setConcurrentValueGroup(groupA);

        var groupB = List.of(visitB1, visitB2, visitB3);
        visitB1.setConcurrentValueGroup(groupB);
        visitB2.setConcurrentValueGroup(groupB);
        visitB3.setConcurrentValueGroup(groupB);

        vehicle1.setValues(List.of(visitA1, visitB1, visitC));
        vehicle2.setValues(List.of(visitA2, visitB2));
        vehicle3.setValues(List.of(visitB3));

        vehicle1.updateValueShadows();
        vehicle2.updateValueShadows();
        vehicle3.updateValueShadows();

        var solution = new TestdataConcurrentSolution();
        solution.setEntities(List.of(vehicle1, vehicle2, vehicle3));
        solution.setValues(List.of(visitA1, visitA2, visitB1, visitB2, visitB3, visitC));
        valueRangeManager.reset(solution);

        shadowVariableSupport =
                new ShadowVariableSupport<>(scoreDirector, size -> {
                    var out = spy(new MockTopologicalOrderGraph(size));
                    graphReference.set(out);
                    return out;
                });
        shadowVariableSupport.linkShadowVariables();
        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        shadowVariableSupport.resetWorkingSolution();

        var graph = graphReference.get();

        var entityMetamodel = solutionDescriptor.getMetaModel()
                .entity(TestdataConcurrentValue.class);
        var serviceReadyTime = entityMetamodel.variable("serviceReadyTime");
        var serviceStartTime = entityMetamodel.variable("serviceStartTime");
        var serviceFinishTime = entityMetamodel.variable("serviceFinishTime");

        var expectedAddCount = new AtomicInteger(0);
        var expectedRemoveCount = new AtomicInteger(0);

        QuadConsumer<VariableMetaModel<?, ?, ?>, Object, VariableMetaModel<?, ?, ?>, Object> verifyAddEdge =
                (fromId, fromObj, toId, toObj) -> {
                    verify(graph).addEdge(fromId, fromObj, toId, toObj);
                    expectedAddCount.getAndIncrement();
                };

        QuadConsumer<VariableMetaModel<?, ?, ?>, Object, VariableMetaModel<?, ?, ?>, Object> verifyRemoveEdge =
                (fromId, fromObj, toId, toObj) -> {
                    verify(graph).removeEdge(fromId, fromObj, toId, toObj);
                    expectedRemoveCount.getAndIncrement();
                };

        for (var visit : solution.getValues()) {

            // If a visit does not have a concurrent group, all variables of that visit share the same node.
            if (visit.getConcurrentValueGroup() != null && visit.equals(visit.getConcurrentValueGroup().get(0))) {
                // all values in the concurrent value group point to the representative member,
                // which is the first member of the group
                for (var element : visit.getConcurrentValueGroup()) {
                    verifyAddEdge.accept(serviceReadyTime, element, serviceStartTime, visit);
                    verifyAddEdge.accept(serviceStartTime, visit, serviceFinishTime, element);
                }
            }

            if (visit.getPreviousValue() != null) {
                verifyAddEdge.accept(serviceFinishTime, visit.getPreviousValue(), serviceReadyTime, visit);
            }
        }
        // Note: addEdge only adds an edge if it does not already exists in the graph,
        // so the count is less here
        verify(graph, atMost(expectedAddCount.get())).addEdge(any(), any(), any(), any());
        verify(graph, times(0)).removeEdge(any(), any(), any(), any());

        reset(graph);
        expectedAddCount.set(0);
        expectedRemoveCount.set(0);

        var previousElementDescriptor = solutionDescriptor.getEntityDescriptorStrict(TestdataConcurrentValue.class)
                .getShadowVariableDescriptor("previousValue");
        var vehicleDescriptor =
                solutionDescriptor.getEntityDescriptorStrict(TestdataConcurrentValue.class)
                        .getShadowVariableDescriptor("entity");

        shadowVariableSupport.beforeVariableChanged(previousElementDescriptor, visitB1);
        shadowVariableSupport.beforeVariableChanged(previousElementDescriptor, visitC);
        shadowVariableSupport.beforeVariableChanged(vehicleDescriptor, visitB1);
        shadowVariableSupport.beforeVariableChanged(vehicleDescriptor, visitC);

        verifyRemoveEdge.accept(serviceFinishTime, visitA1, serviceReadyTime, visitB1);
        verifyRemoveEdge.accept(serviceFinishTime, visitB1, serviceReadyTime, visitC);

        verify(graph, times(0)).addEdge(any(), any(), any(), any());
        verify(graph, times(expectedRemoveCount.get())).removeEdge(any(), any(), any(), any());

        reset(graph);
        expectedAddCount.set(0);
        expectedRemoveCount.set(0);

        vehicle1.setValues(List.of(visitA1, visitC));
        verify(graph, times(0)).addEdge(any(), any(), any(), any());
        verify(graph, times(0)).removeEdge(any(), any(), any(), any());

        // Edges are added only when shadow variables are updated,
        // since we require ListVariableState to be up-to-date
        visitC.setPreviousValue(visitA1);
        shadowVariableSupport.afterVariableChanged(previousElementDescriptor, visitC);
        visitC.setEntity(vehicle1);
        shadowVariableSupport.afterVariableChanged(vehicleDescriptor, visitC);
        visitB1.setPreviousValue(null);
        shadowVariableSupport.afterVariableChanged(previousElementDescriptor, visitB1);

        // The declarative shadow variable session update is still pending: score calculation would be unreliable.
        assertThatThrownBy(shadowVariableSupport::assertShadowVariablesAreUpToDate)
                .isInstanceOf(IllegalStateException.class);

        shadowVariableSupport.updateShadowVariables();

        // Triggering flushed the pending declarative update: shadow variables are up to date again.
        shadowVariableSupport.assertShadowVariablesAreUpToDate();

        verifyAddEdge.accept(serviceFinishTime, visitA1, serviceReadyTime, visitC);
        verify(graph, times(expectedAddCount.get())).addEdge(any(), any(), any(), any());
        verify(graph, times(0)).removeEdge(any(), any(), any(), any());
    }

    @Test
    void basicVariableChangeIsDispatchedEagerly() {
        var scoreDirector = basicScoreDirectorMock(TestdataSolution.buildSolutionDescriptor());
        var shadowVariableSupport =
                new ShadowVariableSupport<TestdataSolution>(scoreDirector, DefaultTopologicalOrderGraph::new);
        shadowVariableSupport.linkShadowVariables();

        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        var supply = shadowVariableSupport.demand(new BasicVariableStateDemand<>(variableDescriptor));

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var a = new TestdataEntity("a", val1);

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(a));
        solution.setValueList(List.of(val1, val2));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        shadowVariableSupport.resetWorkingSolution();

        assertThat((Collection<Object>) supply.getInverseCollection(val1)).containsExactly(a);
        assertThat((Collection<?>) supply.getInverseCollection(val2)).isEmpty();

        // Before/after, with no call to updateShadowVariables() in between.
        shadowVariableSupport.beforeVariableChanged(variableDescriptor, a);
        a.setValue(val2);
        shadowVariableSupport.afterVariableChanged(variableDescriptor, a);

        assertThat((Collection<?>) supply.getInverseCollection(val1)).isEmpty();
        assertThat((Collection<Object>) supply.getInverseCollection(val2)).containsExactly(a);
    }

    @Test
    void listVariableChangeIsDispatchedEagerly() {
        var variableDescriptor = TestdataAllowsUnassignedValuesListEntity.buildVariableDescriptorForValueList();
        var solutionDescriptor = variableDescriptor.getEntityDescriptor().getSolutionDescriptor();
        var scoreDirector = basicScoreDirectorMock(solutionDescriptor);

        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var e1 = new TestdataAllowsUnassignedValuesListEntity("e1", v1);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2));

        var valueRangeManager = ValueRangeManager.of(solutionDescriptor, solution);
        when(scoreDirector.getValueRangeManager()).thenReturn(valueRangeManager);
        when(scoreDirector.getWorkingSolution()).thenReturn(solution);

        var shadowVariableSupport =
                new ShadowVariableSupport<>(scoreDirector, DefaultTopologicalOrderGraph::new);
        shadowVariableSupport.linkShadowVariables();
        shadowVariableSupport.resetWorkingSolution();

        var supply = shadowVariableSupport.demand(variableDescriptor.getStateDemand());
        assertThat(supply.isAssigned(v2)).isFalse();

        // Before/after, with no call to updateShadowVariables() in between.
        shadowVariableSupport.beforeListVariableChanged(variableDescriptor, e1, 1, 2);
        e1.getValueList().add(v2);
        shadowVariableSupport.afterListVariableChanged(variableDescriptor, e1, 1, 2);

        assertThat(supply.isAssigned(v2)).isTrue();
    }

    private static <Solution_> InnerScoreDirector<Solution_, ?> basicScoreDirectorMock(
            SolutionDescriptor<Solution_> solutionDescriptor) {
        @SuppressWarnings("unchecked")
        var scoreDirector = (InnerScoreDirector<Solution_, ?>) mock(InnerScoreDirector.class);
        @SuppressWarnings("unchecked")
        var neighborhoodNotifier = (NeighborhoodNotifier<Solution_>) mock(NeighborhoodNotifier.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        when(scoreDirector.getNeighborhoodNotifier()).thenReturn(neighborhoodNotifier);
        return scoreDirector;
    }
}
