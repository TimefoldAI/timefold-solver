package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfValueSelectorForEntity;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.verifyPhaseLifecycle;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.impl.testdata.util.PlannerAssert;

import org.junit.jupiter.api.Test;

class InitializedValueSelectorTest {

    @Test
    void originalSelectionAllowsUnassigned() {
        EntityDescriptor entityDescriptor = TestdataAllowsUnassignedEntity.buildEntityDescriptor();
        TestdataAllowsUnassignedEntity e1 = new TestdataAllowsUnassignedEntity("e1");
        // This variable is unable to have entities as values,
        // but it's an interesting test for allowsUnassigned=true anyway.
        GenuineVariableDescriptor variableDescriptor = entityDescriptor.getGenuineVariableDescriptor("value");
        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = null;
        TestdataValue v3 = new TestdataValue("v3");
        ValueSelector childValueSelector = SelectorTestUtils.mockValueSelector(variableDescriptor,
                v1, v2, v3);

        ValueSelector valueSelector = new InitializedValueSelector(childValueSelector);
        verify(childValueSelector, times(1)).isNeverEnding();

        SolverScope solverScope = mock(SolverScope.class);
        valueSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        valueSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA1);
        assertAllCodesOfValueSelectorForEntity(valueSelector, e1, PlannerAssert.DO_NOT_ASSERT_SIZE, "v1", null, "v3");
        e1.setValue(v1);
        valueSelector.stepEnded(stepScopeA1);

        valueSelector.phaseEnded(phaseScopeA);

        valueSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childValueSelector, 1, 1, 1);
        verify(childValueSelector, times(1)).iterator(any());
    }

    @Test
    void originalSelectionChained() {
        EntityDescriptor entityDescriptor = TestdataChainedEntity.buildEntityDescriptor();
        GenuineVariableDescriptor variableDescriptor = entityDescriptor.getGenuineVariableDescriptor("chainedObject");
        TestdataChainedAnchor a0 = new TestdataChainedAnchor("a0");
        TestdataChainedEntity a1 = new TestdataChainedEntity("a1");
        TestdataChainedEntity a2 = new TestdataChainedEntity("a2");
        ValueSelector childValueSelector = SelectorTestUtils.mockValueSelector(variableDescriptor,
                a0, a1, a2);

        ValueSelector valueSelector = new InitializedValueSelector(childValueSelector);
        verify(childValueSelector, times(1)).isNeverEnding();

        SolverScope solverScope = mock(SolverScope.class);
        valueSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        valueSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA1);
        assertAllCodesOfValueSelectorForEntity(valueSelector, a1, PlannerAssert.DO_NOT_ASSERT_SIZE, "a0");
        a1.setChainedObject(a0);
        valueSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA2);
        assertAllCodesOfValueSelectorForEntity(valueSelector, a2, PlannerAssert.DO_NOT_ASSERT_SIZE, "a0", "a1");
        a2.setChainedObject(a1);
        valueSelector.stepEnded(stepScopeA2);

        valueSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        valueSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB1);
        assertAllCodesOfValueSelectorForEntity(valueSelector, a1, PlannerAssert.DO_NOT_ASSERT_SIZE, "a0", "a1", "a2");
        valueSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB2);
        assertAllCodesOfValueSelectorForEntity(valueSelector, a2, PlannerAssert.DO_NOT_ASSERT_SIZE, "a0", "a1", "a2");
        valueSelector.stepEnded(stepScopeB2);

        valueSelector.phaseEnded(phaseScopeB);

        valueSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childValueSelector, 1, 2, 4);
        verify(childValueSelector, times(4)).iterator(any());
    }

}
