package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfValueSelectorForEntity;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockSolverScope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.BailOutUtils;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FilteringValueSelectorTest {

    @Test
    void filterEntityDependent() {
        TestdataEntity entity = new TestdataEntity("e1");
        ValueSelector childValueSelector = SelectorTestUtils.mockValueSelector(
                TestdataEntity.class, "value",
                new TestdataValue("v1"), new TestdataValue("v2"), new TestdataValue("v3"), new TestdataValue("v4"));

        SelectionFilter<TestdataSolution, TestdataValue> filter = (scoreDirector, value) -> !value.getCode().equals("v3");
        ValueSelector valueSelector = new FilteringValueSelector(childValueSelector, filter);

        SolverScope solverScope = mockSolverScope();
        valueSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        valueSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA1);
        assertAllCodesOfValueSelectorForEntity(valueSelector, entity, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA2);
        assertAllCodesOfValueSelectorForEntity(valueSelector, entity, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeA2);

        valueSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        valueSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB1);
        assertAllCodesOfValueSelectorForEntity(valueSelector, entity, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB2);
        assertAllCodesOfValueSelectorForEntity(valueSelector, entity, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB3);
        assertAllCodesOfValueSelectorForEntity(valueSelector, entity, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeB3);

        valueSelector.phaseEnded(phaseScopeB);

        valueSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childValueSelector, 1, 2, 5);
        verify(childValueSelector, times(5)).iterator(entity);
        verify(childValueSelector, times(5)).getSize(entity);
    }

    @ParameterizedTest
    @ValueSource(longs = { Long.MAX_VALUE, -5L })
    @SuppressWarnings("unchecked")
    void unusableNeverEndingSizeStillBailsOut(long size) {
        var nextCount = new AtomicLong();
        var iterator = new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Object next() {
                nextCount.incrementAndGet();
                return new Object();
            }
        };
        var childValueSelector = (ValueSelector<TestdataSolution>) mock(ValueSelector.class);
        when(childValueSelector.isNeverEnding()).thenReturn(true);
        when(childValueSelector.getSize(any())).thenReturn(size);
        when(childValueSelector.iterator(any())).thenReturn(iterator);
        var valueSelector = FilteringValueSelector.of(childValueSelector, (scoreDirector, value) -> false);
        assertThat(valueSelector.iterator(new TestdataEntity("e")).hasNext()).isFalse();
        assertThat(nextCount).hasValue(size < 0 ? BailOutUtils.UNKNOWN_SIZE_BAIL_OUT : BailOutUtils.MAX_BAIL_OUT);
    }
}
