package ai.timefold.solver.core.impl.domain.variable.listener.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.function.QuadConsumer;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.EntityVariablePair;
import ai.timefold.solver.core.impl.domain.variable.declarative.TopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.ExternalizedSingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableListener;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.shadow.TestdataShadowingChainedSolution;
import ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentEntity;
import ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentSolution;
import ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.order.TestdataShadowVariableOrderEntity;
import ai.timefold.solver.core.impl.testdata.domain.shadow.order.TestdataShadowVariableOrderSolution;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.junit.jupiter.api.Test;

class VariableListenerSupportTest {

    @Test
    void demandBasic() {
        SolutionDescriptor<TestdataSolution> solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        TestdataSolution solution = new TestdataSolution();
        solution.setEntityList(Collections.emptyList());
        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        when(scoreDirector.getSupplyManager()).thenReturn(mock(SupplyManager.class));
        VariableListenerSupport<TestdataSolution> variableListenerSupport = VariableListenerSupport.create(scoreDirector);
        variableListenerSupport.linkVariableListeners();

        VariableDescriptor<TestdataSolution> variableDescriptor =
                solutionDescriptor.getEntityDescriptorStrict(TestdataEntity.class)
                        .getVariableDescriptor("value");

        SingletonInverseVariableSupply supply1 = variableListenerSupport
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        SingletonInverseVariableSupply supply2 = variableListenerSupport
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        assertThat(supply2).isSameAs(supply1);
    }

    @Test
    void demandChained() {
        SolutionDescriptor<TestdataChainedSolution> solutionDescriptor = TestdataChainedSolution.buildSolutionDescriptor();
        InnerScoreDirector<TestdataChainedSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        TestdataChainedSolution solution = new TestdataChainedSolution();
        solution.setChainedEntityList(Collections.emptyList());
        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        when(scoreDirector.getSupplyManager()).thenReturn(mock(SupplyManager.class));
        VariableListenerSupport<TestdataChainedSolution> variableListenerSupport =
                VariableListenerSupport.create(scoreDirector);
        variableListenerSupport.linkVariableListeners();

        VariableDescriptor<TestdataChainedSolution> variableDescriptor =
                solutionDescriptor.getEntityDescriptorStrict(TestdataChainedEntity.class)
                        .getVariableDescriptor("chainedObject");

        SingletonInverseVariableSupply supply1 = variableListenerSupport
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        assertThat(supply1)
                .isInstanceOf(ExternalizedSingletonInverseVariableSupply.class);
        SingletonInverseVariableSupply supply2 = variableListenerSupport
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        assertThat(supply2).isSameAs(supply1);
    }

    @Test
    void demandRichChained() {
        SolutionDescriptor<TestdataShadowingChainedSolution> solutionDescriptor =
                TestdataShadowingChainedSolution.buildSolutionDescriptor();
        InnerScoreDirector<TestdataShadowingChainedSolution, SimpleScore> scoreDirector =
                mock(InnerScoreDirector.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        TestdataShadowingChainedSolution solution = new TestdataShadowingChainedSolution();
        solution.setChainedEntityList(Collections.emptyList());
        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        when(scoreDirector.getSupplyManager()).thenReturn(mock(SupplyManager.class));
        VariableListenerSupport<TestdataShadowingChainedSolution> variableListenerSupport =
                VariableListenerSupport.create(scoreDirector);
        variableListenerSupport.linkVariableListeners();

        VariableDescriptor<TestdataShadowingChainedSolution> variableDescriptor = solutionDescriptor
                .getEntityDescriptorStrict(TestdataShadowingChainedEntity.class)
                .getVariableDescriptor("chainedObject");

        SingletonInverseVariableSupply supply1 = variableListenerSupport
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        assertThat(supply1)
                .isInstanceOf(SingletonInverseVariableListener.class);
        SingletonInverseVariableSupply supply2 = variableListenerSupport
                .demand(new SingletonInverseVariableDemand<>(variableDescriptor));
        assertThat(supply2).isSameAs(supply1);
    }

    @Test
    void shadowVariableListenerOrder() {
        EntityDescriptor<TestdataShadowVariableOrderSolution> entityDescriptor =
                TestdataShadowVariableOrderEntity.buildEntityDescriptor();
        SolutionDescriptor<TestdataShadowVariableOrderSolution> solutionDescriptor = entityDescriptor.getSolutionDescriptor();
        InnerScoreDirector<TestdataShadowVariableOrderSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);

        NotifiableRegistry<TestdataShadowVariableOrderSolution> registry = new NotifiableRegistry<>(solutionDescriptor);
        VariableListenerSupport<TestdataShadowVariableOrderSolution> variableListenerSupport =
                new VariableListenerSupport<>(scoreDirector, registry, DefaultTopologicalOrderGraph::new);

        variableListenerSupport.linkVariableListeners();

        assertThat(registry.getAll())
                .map(Object::toString)
                .containsExactly(
                        "(0) C",
                        "(1) D",
                        "(2) E",
                        "(3) FG");

        assertThat(registry.get(entityDescriptor))
                .map(Object::toString)
                .containsExactly(
                        "(0) C",
                        "(1) D",
                        "(2) E",
                        "(3) FG");

        assertThat(registry.get(entityDescriptor.getVariableDescriptor("x6A")))
                .map(VariableListenerNotifiable::toString)
                .containsExactly("(0) C");
        assertThat(registry.get(entityDescriptor.getVariableDescriptor("x5B")))
                .map(VariableListenerNotifiable::toString)
                .containsExactly("(2) E");
        assertThat(registry.get(entityDescriptor.getVariableDescriptor("x3C")))
                .map(VariableListenerNotifiable::toString)
                .containsExactly("(1) D", "(2) E");
        assertThat(registry.get(entityDescriptor.getVariableDescriptor("x1D")))
                .isEmpty();
        assertThat(registry.get(entityDescriptor.getVariableDescriptor("x2E")))
                .map(VariableListenerNotifiable::toString)
                .containsExactly("(3) FG");
        assertThat(registry.get(entityDescriptor.getVariableDescriptor("x4F")))
                .isEmpty();
        assertThat(registry.get(entityDescriptor.getVariableDescriptor("x0G")))
                .isEmpty();
    }

    private static class MockTopologicalOrderGraph extends DefaultTopologicalOrderGraph implements TopologicalOrderGraph {
        Object[] nodeToEntities;
        VariableMetaModel<?, ?, ?>[] nodeToVariableMetamodel;

        public MockTopologicalOrderGraph(int size) {
            super(size);
            nodeToEntities = new Object[size];
            nodeToVariableMetamodel = new VariableMetaModel[size];
        }

        @Override
        public void withNodeData(List<EntityVariablePair> nodes) {
            nodeToEntities = nodes.stream().map(EntityVariablePair::entity).toArray(Object[]::new);
            nodeToVariableMetamodel = nodes.stream().map(EntityVariablePair::variableId).toArray(VariableMetaModel[]::new);
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
        public void addEdge(int from, int to) {
            super.addEdge(from, to);
            addEdge(nodeToVariableMetamodel[from], nodeToEntities[from], nodeToVariableMetamodel[to], nodeToEntities[to]);
        }

        @Override
        public void removeEdge(int from, int to) {
            super.addEdge(from, to);
            removeEdge(nodeToVariableMetamodel[from], nodeToEntities[from], nodeToVariableMetamodel[to], nodeToEntities[to]);
        }
    }

    @Test
    void shadowVariableListGraphEvents() {
        var solutionDescriptor = TestdataConcurrentSolution.buildSolutionDescriptor();
        InnerScoreDirector<TestdataConcurrentSolution, HardSoftScore> scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);

        AtomicReference<MockTopologicalOrderGraph> graphReference = new AtomicReference<>(null);
        var registry = new NotifiableRegistry<>(solutionDescriptor);
        VariableListenerSupport<TestdataConcurrentSolution> variableListenerSupport;

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

        variableListenerSupport =
                new VariableListenerSupport<>(scoreDirector, registry, size -> {
                    var out = spy(new MockTopologicalOrderGraph(size));
                    graphReference.set(out);
                    return out;
                });
        variableListenerSupport.linkVariableListeners();
        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        variableListenerSupport.resetWorkingSolution();

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
            verifyAddEdge.accept(serviceReadyTime, visit, serviceStartTime, visit);
            verifyAddEdge.accept(serviceStartTime, visit, serviceFinishTime, visit);

            if (visit.getPreviousValue() != null) {
                verifyAddEdge.accept(serviceFinishTime, visit.getPreviousValue(), serviceReadyTime, visit);
            }

            if (visit.getConcurrentValueGroup() != null) {
                for (var element : visit.getConcurrentValueGroup()) {
                    verifyAddEdge.accept(serviceReadyTime, element, serviceStartTime, visit);
                }
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

        variableListenerSupport.beforeVariableChanged(previousElementDescriptor, visitB1);
        variableListenerSupport.beforeVariableChanged(previousElementDescriptor, visitC);
        variableListenerSupport.beforeVariableChanged(vehicleDescriptor, visitB1);
        variableListenerSupport.beforeVariableChanged(vehicleDescriptor, visitC);

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

        // Edges are added only when variable listeners are triggered,
        // since we require ListVariableState to be up-to-date
        visitC.setPreviousValue(visitA1);
        visitC.setEntity(vehicle1);
        visitB1.setPreviousValue(null);

        variableListenerSupport.triggerVariableListenersInNotificationQueues();

        verifyAddEdge.accept(serviceFinishTime, visitA1, serviceReadyTime, visitC);
        verify(graph, times(expectedAddCount.get())).addEdge(any(), any(), any(), any());
        verify(graph, times(0)).removeEdge(any(), any(), any(), any());
    }
}
