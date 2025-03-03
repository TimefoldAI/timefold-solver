package ai.timefold.solver.core.impl.domain.variable.listener.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.function.QuadConsumer;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.EntityVariableOrFactReference;
import ai.timefold.solver.core.impl.domain.variable.declarative.LoopedTracker;
import ai.timefold.solver.core.impl.domain.variable.declarative.TopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableId;
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
import ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRRoutePlan;
import ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRVehicle;
import ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRVisit;
import ai.timefold.solver.core.impl.testdata.domain.shadow.order.TestdataShadowVariableOrderEntity;
import ai.timefold.solver.core.impl.testdata.domain.shadow.order.TestdataShadowVariableOrderSolution;

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

    private static class MockTopologicalOrderGraph implements TopologicalOrderGraph {
        Object[] nodeToEntities;
        VariableId[] nodeToVariableId;

        public MockTopologicalOrderGraph(int size) {
            nodeToEntities = new Object[size];
            nodeToVariableId = new VariableId[size];
        }

        @Override
        public <Solution_> void withNodeData(List<EntityVariableOrFactReference<Solution_>> nodes) {
            nodeToEntities = nodes.stream().map(EntityVariableOrFactReference::entity).toArray(Object[]::new);
            nodeToVariableId = nodes.stream().map(EntityVariableOrFactReference::variableId).toArray(VariableId[]::new);
        }

        public void addEdge(VariableId fromId, Object fromEntity, VariableId toId, Object toEntity) {
        }

        public void removeEdge(VariableId fromId, Object fromEntity, VariableId toId, Object toEntity) {

        }

        @Override
        public void addEdge(int from, int to) {
            addEdge(nodeToVariableId[from], nodeToEntities[from], nodeToVariableId[to], nodeToEntities[to]);
        }

        @Override
        public void removeEdge(int from, int to) {
            removeEdge(nodeToVariableId[from], nodeToEntities[from], nodeToVariableId[to], nodeToEntities[to]);
        }

        @Override
        public PrimitiveIterator.OfInt nodeForwardEdges(int from) {
            return IntStream.empty().iterator();
        }

        @Override
        public boolean isLooped(LoopedTracker loopedTracker, int node) {
            return false;
        }

        @Override
        public int getTopologicalOrder(int node) {
            return 0;
        }
    }

    @Test
    void shadowVariableListGraphEvents() {
        var solutionDescriptor = TestdataFSRRoutePlan.buildSolutionDescriptor();
        InnerScoreDirector<TestdataFSRRoutePlan, HardSoftScore> scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);

        AtomicReference<MockTopologicalOrderGraph> graphReference = new AtomicReference<>(null);
        var registry = new NotifiableRegistry<>(solutionDescriptor);
        VariableListenerSupport<TestdataFSRRoutePlan> variableListenerSupport;

        var vehicle1 = new TestdataFSRVehicle("1");
        var vehicle2 = new TestdataFSRVehicle("2");
        var vehicle3 = new TestdataFSRVehicle("3");

        var visitA1 = new TestdataFSRVisit("a1");
        var visitA2 = new TestdataFSRVisit("a2");

        var visitB1 = new TestdataFSRVisit("b1");
        var visitB2 = new TestdataFSRVisit("b2");
        var visitB3 = new TestdataFSRVisit("b3");

        var visitC = new TestdataFSRVisit("c");

        var groupA = List.of(visitA1, visitA2);
        visitA1.setVisitGroup(groupA);
        visitA2.setVisitGroup(groupA);

        var groupB = List.of(visitB1, visitB2, visitB3);
        visitB1.setVisitGroup(groupB);
        visitB2.setVisitGroup(groupB);
        visitB3.setVisitGroup(groupB);

        vehicle1.setVisits(List.of(visitA1, visitB1, visitC));
        vehicle2.setVisits(List.of(visitA2, visitB2));
        vehicle3.setVisits(List.of(visitB3));

        vehicle1.updateVisitShadows();
        vehicle2.updateVisitShadows();
        vehicle3.updateVisitShadows();

        var solution = new TestdataFSRRoutePlan();
        solution.setVehicles(List.of(vehicle1, vehicle2, vehicle3));
        solution.setVisits(List.of(visitA1, visitA2, visitB1, visitB2, visitB3, visitC));

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

        var root = VariableId.entity(TestdataFSRVisit.class);
        var previous = root.previous();
        var inverse = root.inverse();

        var serviceReadyTime = root.child("serviceReadyTime");
        var serviceStartTime = root.child("serviceStartTime");
        var serviceFinishTime = root.child("serviceFinishTime");

        var group = root.group(TestdataFSRVisit.class, 0);
        var groupReadyTime = group.child("serviceReadyTime");
        var previousFinishTime = previous.child("serviceFinishTime");

        var expectedAddCount = new AtomicInteger(0);
        var expectedRemoveCount = new AtomicInteger(0);

        QuadConsumer<VariableId, Object, VariableId, Object> verifyAddEdge = (fromId, fromObj, toId, toObj) -> {
            verify(graph).addEdge(fromId, fromObj, toId, toObj);
            expectedAddCount.getAndIncrement();
        };

        QuadConsumer<VariableId, Object, VariableId, Object> verifyRemoveEdge = (fromId, fromObj, toId, toObj) -> {
            verify(graph).removeEdge(fromId, fromObj, toId, toObj);
            expectedRemoveCount.getAndIncrement();
        };

        for (var visit : solution.getVisits()) {
            verifyAddEdge.accept(root, visit, previous, visit);
            verifyAddEdge.accept(root, visit, inverse, visit);
            verifyAddEdge.accept(root, visit, group, visit);
            verifyAddEdge.accept(root, visit, serviceReadyTime, visit);
            verifyAddEdge.accept(root, visit, serviceStartTime, visit);
            verifyAddEdge.accept(root, visit, serviceFinishTime, visit);
            verifyAddEdge.accept(previous, visit, previousFinishTime, visit);
            verifyAddEdge.accept(group, visit, groupReadyTime, visit);

            verifyAddEdge.accept(previousFinishTime, visit, serviceReadyTime, visit);
            verifyAddEdge.accept(inverse, visit, serviceReadyTime, visit);

            verifyAddEdge.accept(serviceReadyTime, visit, serviceStartTime, visit);
            verifyAddEdge.accept(groupReadyTime, visit, serviceStartTime, visit);
            verifyAddEdge.accept(serviceStartTime, visit, serviceFinishTime, visit);

            if (visit.getPreviousVisit() != null) {
                verifyAddEdge.accept(serviceFinishTime, visit.getPreviousVisit(), previousFinishTime, visit);
            }

            if (visit.getVisitGroup() != null) {
                for (var element : visit.getVisitGroup()) {
                    verifyAddEdge.accept(serviceReadyTime, element, groupReadyTime, visit);
                }
            }
        }
        verify(graph, times(expectedAddCount.get())).addEdge(any(), any(), any(), any());
        verify(graph, times(0)).removeEdge(any(), any(), any(), any());

        reset(graph);
        expectedAddCount.set(0);
        expectedRemoveCount.set(0);

        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        variableListenerSupport.beforeListVariableChanged(listVariableDescriptor, vehicle1, 1, 3);

        verifyRemoveEdge.accept(serviceFinishTime, visitA1, previousFinishTime, visitB1);
        verifyRemoveEdge.accept(serviceFinishTime, visitB1, previousFinishTime, visitC);

        verify(graph, times(0)).addEdge(any(), any(), any(), any());
        verify(graph, times(expectedRemoveCount.get())).removeEdge(any(), any(), any(), any());

        reset(graph);
        expectedAddCount.set(0);
        expectedRemoveCount.set(0);

        vehicle1.setVisits(List.of(visitA1, visitC));
        variableListenerSupport.afterElementUnassigned(listVariableDescriptor, visitB1);
        verify(graph, times(0)).addEdge(any(), any(), any(), any());
        verify(graph, times(0)).removeEdge(any(), any(), any(), any());

        // Edges are added only when variable listeners are triggered,
        // since we require ListVariableState to be up-to-date
        variableListenerSupport.afterListVariableChanged(listVariableDescriptor, vehicle1, 1, 2);
        variableListenerSupport.triggerVariableListenersInNotificationQueues();

        verifyAddEdge.accept(serviceFinishTime, visitA1, previousFinishTime, visitC);
        verify(graph, times(expectedAddCount.get())).addEdge(any(), any(), any(), any());
        verify(graph, times(0)).removeEdge(any(), any(), any(), any());
    }
}
