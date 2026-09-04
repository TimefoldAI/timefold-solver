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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.function.QuadConsumer;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.GraphNode;
import ai.timefold.solver.core.impl.domain.variable.declarative.TopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableUpdaterInfo;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.violation.ListVariableTracker;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.NeighborhoodNotifier;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.multivar.TestdataOtherValue;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentEntity;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentValue;
import ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse.TestdataBasicInverseEntity;
import ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse.TestdataBasicInverseGroup;
import ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse.TestdataBasicInverseOwner;
import ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse.TestdataBasicInverseSolution;
import ai.timefold.solver.core.testdomain.shadow.mixed.TestdataMixedEntity;
import ai.timefold.solver.core.testdomain.shadow.mixed.TestdataMixedSolution;
import ai.timefold.solver.core.testdomain.shadow.mixed.TestdataMixedValue;

import org.junit.jupiter.api.Test;

class SolverVariableSupportTest {

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
        var neighborhoodNotifier = (NeighborhoodNotifier<TestdataConcurrentSolution>) mock(NeighborhoodNotifier.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        when(scoreDirector.getNeighborhoodNotifier()).thenReturn(neighborhoodNotifier);
        var listVariableState = mock(ListVariableState.class);
        when(scoreDirector.getListVariableState(any(ListVariableDescriptor.class))).thenReturn(listVariableState);
        var valueRangeManager = new ValueRangeManager<>(solutionDescriptor);
        when(scoreDirector.getValueRangeManager()).thenReturn(valueRangeManager);

        AtomicReference<MockTopologicalOrderGraph> graphReference = new AtomicReference<>(null);
        SolverVariableSupport<TestdataConcurrentSolution> solverVariableSupport;

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

        solverVariableSupport =
                new SolverVariableSupport<>(scoreDirector, size -> {
                    var out = spy(new MockTopologicalOrderGraph(size));
                    graphReference.set(out);
                    return out;
                });
        solverVariableSupport.linkShadowVariables();
        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        solverVariableSupport.resetWorkingSolution();

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

        solverVariableSupport.beforeVariableChanged(previousElementDescriptor, visitB1);
        solverVariableSupport.beforeVariableChanged(previousElementDescriptor, visitC);
        solverVariableSupport.beforeVariableChanged(vehicleDescriptor, visitB1);
        solverVariableSupport.beforeVariableChanged(vehicleDescriptor, visitC);

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
        solverVariableSupport.afterVariableChanged(previousElementDescriptor, visitC);
        visitC.setEntity(vehicle1);
        solverVariableSupport.afterVariableChanged(vehicleDescriptor, visitC);
        visitB1.setPreviousValue(null);
        solverVariableSupport.afterVariableChanged(previousElementDescriptor, visitB1);

        // The declarative shadow variable session update is still pending: score calculation would be unreliable.
        assertThatThrownBy(solverVariableSupport::assertShadowVariablesAreUpToDate)
                .isInstanceOf(IllegalStateException.class);

        solverVariableSupport.updateShadowVariables();

        // Triggering flushed the pending declarative update: shadow variables are up to date again.
        solverVariableSupport.assertShadowVariablesAreUpToDate();

        verifyAddEdge.accept(serviceFinishTime, visitA1, serviceReadyTime, visitC);
        verify(graph, times(expectedAddCount.get())).addEdge(any(), any(), any(), any());
        verify(graph, times(0)).removeEdge(any(), any(), any(), any());
    }

    @Test
    void basicVariableChangeIsDispatchedEagerly() {
        var scoreDirector = basicScoreDirectorMock(TestdataSolution.buildSolutionDescriptor());
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        var solverVariableSupport = new SolverVariableSupport<>(scoreDirector, DefaultTopologicalOrderGraph::new);
        solverVariableSupport.linkShadowVariables();

        var basicVariableState = solverVariableSupport.getBasicVariableState(variableDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var a = new TestdataEntity("a", val1);

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(a));
        solution.setValueList(List.of(val1, val2));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        solverVariableSupport.resetWorkingSolution();

        assertThat(basicVariableState.getInverseCollection(val1)).containsExactly(a);
        assertThat((Collection<?>) basicVariableState.getInverseCollection(val2)).isEmpty();

        // Before/after, with no call to updateShadowVariables() in between.
        solverVariableSupport.beforeVariableChanged(variableDescriptor, a);
        a.setValue(val2);
        solverVariableSupport.afterVariableChanged(variableDescriptor, a);

        assertThat((Collection<?>) basicVariableState.getInverseCollection(val1)).isEmpty();
        assertThat(basicVariableState.getInverseCollection(val2)).containsExactly(a);
    }

    @Test
    void repeatedBasicVariableChangeOnSameEntityBeforeUpdateIsDispatchedCorrectly() {
        var scoreDirector = basicScoreDirectorMock(TestdataSolution.buildSolutionDescriptor());
        var solverVariableSupport = new SolverVariableSupport<>(scoreDirector, DefaultTopologicalOrderGraph::new);
        solverVariableSupport.linkShadowVariables();

        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        var basicVariableState = solverVariableSupport.getBasicVariableState(variableDescriptor);

        var val1 = new TestdataValue("1");
        var val2 = new TestdataValue("2");
        var val3 = new TestdataValue("3");
        var a = new TestdataEntity("a", val1);

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(a));
        solution.setValueList(List.of(val1, val2, val3));

        when(scoreDirector.getWorkingSolution()).thenReturn(solution);
        solverVariableSupport.resetWorkingSolution();

        assertThat(basicVariableState.getInverseCollection(val1)).containsExactly(a);
        assertThat((Collection<?>) basicVariableState.getInverseCollection(val2)).isEmpty();
        assertThat((Collection<?>) basicVariableState.getInverseCollection(val3)).isEmpty();

        // Two changes to the same entity back-to-back, with no call to updateShadowVariables() in between.
        solverVariableSupport.beforeVariableChanged(variableDescriptor, a);
        a.setValue(val2);
        solverVariableSupport.afterVariableChanged(variableDescriptor, a);

        solverVariableSupport.beforeVariableChanged(variableDescriptor, a);
        a.setValue(val3);
        solverVariableSupport.afterVariableChanged(variableDescriptor, a);

        assertThat((Collection<?>) basicVariableState.getInverseCollection(val1)).isEmpty();
        assertThat((Collection<?>) basicVariableState.getInverseCollection(val2)).isEmpty();
        assertThat(basicVariableState.getInverseCollection(val3)).containsExactly(a);
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
        var solverVariableSupport =
                new SolverVariableSupport<>(scoreDirector, DefaultTopologicalOrderGraph::new);
        solverVariableSupport.linkShadowVariables();
        solverVariableSupport.resetWorkingSolution();
        var listVariableState = solverVariableSupport.getListVariableState(variableDescriptor);

        assertThat(listVariableState.isAssigned(v2)).isFalse();

        // Before/after, with no call to updateShadowVariables() in between.
        solverVariableSupport.beforeListVariableChanged(variableDescriptor, e1, 1, 2);
        e1.getValueList().add(v2);
        solverVariableSupport.afterListVariableChanged(variableDescriptor, e1, 1, 2);

        assertThat(listVariableState.isAssigned(v2)).isTrue();
    }

    @Test
    void basicVariableStateIsCreatedOncePerVariable() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var scoreDirector = basicScoreDirectorMock(solutionDescriptor);
        var solverVariableSupport = new SolverVariableSupport<>(scoreDirector, DefaultTopologicalOrderGraph::new);
        solverVariableSupport.linkShadowVariables();
        var variableDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataEntity.class)
                .getGenuineVariableDescriptor("value");

        // The state is the single source of truth for the variable's inverse relation;
        // every caller must get the same instance, or they observe different inverse collections
        // and each extra instance is another handler notified on every variable change.
        var basicVariableState = solverVariableSupport.getBasicVariableState(variableDescriptor);
        assertThat(solverVariableSupport.getBasicVariableState(variableDescriptor)).isSameAs(basicVariableState);
        assertThat(solverVariableSupport.getBasicVariableState(variableDescriptor)).isSameAs(basicVariableState);
    }

    @Test
    void listVariableStateIsCreatedOncePerVariable() {
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var scoreDirector = basicScoreDirectorMock(solutionDescriptor);
        var solverVariableSupport = new SolverVariableSupport<>(scoreDirector, DefaultTopologicalOrderGraph::new);
        solverVariableSupport.linkShadowVariables();
        var variableDescriptor = solutionDescriptor.getListVariableDescriptor();

        var listVariableState = solverVariableSupport.getListVariableState(variableDescriptor);
        assertThat(listVariableState).isNotNull();
        assertThat(solverVariableSupport.getListVariableState(variableDescriptor)).isSameAs(listVariableState);
        assertThat(solverVariableSupport.getListVariableState(variableDescriptor)).isSameAs(listVariableState);
    }

    @Test
    void basicVariableStateSurvivesWorkingSolutionResets() {
        // This domain's declarative shadow variable chains through two genuine variables,
        // so rebuilding the shadow variable session asks for the basic variable state.
        // That rebuild happens on every setWorkingSolution() and on every problem change,
        // so a state which is not reused would accumulate one stale handler per reset.
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(TestdataBasicInverseSolution.class,
                TestdataBasicInverseEntity.class, TestdataBasicInverseGroup.class);
        var owner = new TestdataBasicInverseOwner("o1");
        var group = new TestdataBasicInverseGroup("g1");
        group.setOwner(owner);
        var entity = new TestdataBasicInverseEntity("e1");
        entity.setGroup(group);
        var solution = new TestdataBasicInverseSolution(List.of(entity), List.of(group), List.of(owner));
        var variableDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataBasicInverseEntity.class)
                .getGenuineVariableDescriptor("group");

        try (var scoreDirector = new EasyScoreDirectorFactory<>(solutionDescriptor, s -> SimpleScore.ZERO,
                EnvironmentMode.PHASE_ASSERT).buildScoreDirector()) {
            scoreDirector.setWorkingSolution(solution);
            var basicVariableState = scoreDirector.getBasicVariableState(variableDescriptor);
            for (var i = 0; i < 3; i++) {
                scoreDirector.setWorkingSolution(solution);
                assertThat(scoreDirector.getBasicVariableState(variableDescriptor)).isSameAs(basicVariableState);
            }
        }
    }

    @Test
    void mixedModelStatesAreCreatedOncePerVariable() {
        // A mixed model: the entity owns a list variable, and its values are entities in their own right,
        // carrying a basic variable, list-sourced inverse and previous shadows,
        // and a declarative shadow chaining through both.
        // Both the basic and the list handler registries are therefore populated at once.
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(TestdataMixedSolution.class,
                TestdataMixedEntity.class, TestdataMixedValue.class);
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        var delayVariableDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataMixedValue.class)
                .getGenuineVariableDescriptor("delay");

        var value1 = new TestdataMixedValue("v1");
        var value2 = new TestdataMixedValue("v2");
        var entity = new TestdataMixedEntity("e1");
        entity.setValueList(new ArrayList<>(List.of(value1, value2)));
        var solution = new TestdataMixedSolution();
        solution.setMixedEntityList(List.of(entity));
        solution.setMixedValueList(List.of(value1, value2));
        solution.setDelayList(List.of(1, 2));

        try (var scoreDirector = new EasyScoreDirectorFactory<>(solutionDescriptor, s -> SimpleScore.ZERO,
                EnvironmentMode.PHASE_ASSERT).buildScoreDirector()) {
            scoreDirector.setWorkingSolution(solution);

            var basicVariableState = scoreDirector.getBasicVariableState(delayVariableDescriptor);
            var listVariableState = scoreDirector.getListVariableState(listVariableDescriptor);

            // Neither lookup may hand out the state of the other kind of variable.
            assertThat(basicVariableState.getSourceVariableDescriptor()).isSameAs(delayVariableDescriptor);
            assertThat(listVariableState.getSourceVariableDescriptor()).isSameAs(listVariableDescriptor);
            assertThat(listVariableState).isNotSameAs(basicVariableState);

            // Both are reused, rather than replaced, on every subsequent call ...
            assertThat(scoreDirector.getBasicVariableState(delayVariableDescriptor)).isSameAs(basicVariableState);
            assertThat(scoreDirector.getListVariableState(listVariableDescriptor)).isSameAs(listVariableState);

            // ... including after a working solution reset, which rebuilds the shadow variable session.
            scoreDirector.setWorkingSolution(solution);
            assertThat(scoreDirector.getBasicVariableState(delayVariableDescriptor)).isSameAs(basicVariableState);
            assertThat(scoreDirector.getListVariableState(listVariableDescriptor)).isSameAs(listVariableState);
        }
    }

    @Test
    void multiVariableModelStatesAreCreatedOncePerVariable() {
        // One entity with three basic variables, two of which share the same value type.
        // Each variable owns the inverse relation of its own values,
        // so each needs its own state; sharing one would cross their inverse collections.
        var solutionDescriptor = TestdataMultiVarSolution.buildSolutionDescriptor();
        var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(TestdataMultiVarEntity.class);
        var primaryVariableDescriptor = entityDescriptor.getGenuineVariableDescriptor("primaryValue");
        var secondaryVariableDescriptor = entityDescriptor.getGenuineVariableDescriptor("secondaryValue");
        var tertiaryVariableDescriptor = entityDescriptor.getGenuineVariableDescriptor("tertiaryValueAllowedUnassigned");

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var otherValue = new TestdataOtherValue("o1");
        var entity = new TestdataMultiVarEntity("e1", value1, value2, otherValue);
        var solution = new TestdataMultiVarSolution("s1");
        solution.setValueList(List.of(value1, value2));
        solution.setOtherValueList(List.of(otherValue));
        solution.setMultiVarEntityList(List.of(entity));

        try (var scoreDirector = new EasyScoreDirectorFactory<>(solutionDescriptor, s -> SimpleScore.ZERO,
                EnvironmentMode.PHASE_ASSERT).buildScoreDirector()) {
            scoreDirector.setWorkingSolution(solution);

            var primaryState = scoreDirector.getBasicVariableState(primaryVariableDescriptor);
            var secondaryState = scoreDirector.getBasicVariableState(secondaryVariableDescriptor);
            var tertiaryState = scoreDirector.getBasicVariableState(tertiaryVariableDescriptor);

            // One state per variable, never shared between the variables of the same entity ...
            assertThat(primaryState.getSourceVariableDescriptor()).isSameAs(primaryVariableDescriptor);
            assertThat(secondaryState.getSourceVariableDescriptor()).isSameAs(secondaryVariableDescriptor);
            assertThat(tertiaryState.getSourceVariableDescriptor()).isSameAs(tertiaryVariableDescriptor);
            assertThat(primaryState).isNotSameAs(secondaryState).isNotSameAs(tertiaryState);
            assertThat(secondaryState).isNotSameAs(tertiaryState);

            // ... each is reused on every subsequent call ...
            assertThat(scoreDirector.getBasicVariableState(primaryVariableDescriptor)).isSameAs(primaryState);
            assertThat(scoreDirector.getBasicVariableState(secondaryVariableDescriptor)).isSameAs(secondaryState);
            assertThat(scoreDirector.getBasicVariableState(tertiaryVariableDescriptor)).isSameAs(tertiaryState);

            // ... and each tracks only its own variable's inverse relation,
            // even where two variables point into the same value range.
            assertThat(primaryState.<TestdataMultiVarEntity> getInverseCollection(value1)).containsExactly(entity);
            assertThat(primaryState.<TestdataMultiVarEntity> getInverseCollection(value2)).isEmpty();
            assertThat(secondaryState.<TestdataMultiVarEntity> getInverseCollection(value1)).isEmpty();
            assertThat(secondaryState.<TestdataMultiVarEntity> getInverseCollection(value2)).containsExactly(entity);
            assertThat(tertiaryState.<TestdataMultiVarEntity> getInverseCollection(otherValue)).containsExactly(entity);
        }
    }

    @Test
    void listVariableStateIsFoundWhenAnotherHandlerIsRegisteredFirst() {
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var scoreDirector = basicScoreDirectorMock(solutionDescriptor);
        var solverVariableSupport = new SolverVariableSupport<>(scoreDirector, DefaultTopologicalOrderGraph::new);
        var variableDescriptor = solutionDescriptor.getListVariableDescriptor();

        // A tracker shares the list variable change notification list with the state,
        // and tracking environment modes demand one.
        // Registering it first must not stop the state from being created, nor be mistaken for the state.
        var listVariableTracker = new ListVariableTracker<>(variableDescriptor);
        solverVariableSupport.demand(listVariableTracker.demand());

        var listVariableState = solverVariableSupport.getListVariableState(variableDescriptor);
        assertThat(listVariableState).isNotNull();
        assertThat(listVariableState).isNotSameAs(listVariableTracker);
        assertThat(listVariableState.getSourceVariableDescriptor()).isSameAs(variableDescriptor);
        assertThat(solverVariableSupport.getListVariableState(variableDescriptor)).isSameAs(listVariableState);
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
