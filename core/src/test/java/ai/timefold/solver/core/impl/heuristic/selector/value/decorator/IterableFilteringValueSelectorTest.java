package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfValueSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockSolverScope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.util.BailOutUtils;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IterableFilteringValueSelectorTest {

    @Test
    void filter() {
        IterableValueSelector childValueSelector = SelectorTestUtils.mockIterableValueSelector(
                TestdataEntity.class, "value",
                new TestdataValue("v1"), new TestdataValue("v2"), new TestdataValue("v3"), new TestdataValue("v4"));

        SelectionFilter<TestdataSolution, TestdataValue> filter = (scoreDirector, value) -> !value.getCode().equals("v3");
        IterableValueSelector valueSelector = new IterableFilteringValueSelector(childValueSelector, filter);

        var solverScope = mockSolverScope();
        valueSelector.solvingStarted(solverScope);

        var phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        valueSelector.phaseStarted(phaseScopeA);

        var stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA1);
        assertAllCodesOfValueSelector(valueSelector, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeA1);

        var stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA2);
        assertAllCodesOfValueSelector(valueSelector, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeA2);

        valueSelector.phaseEnded(phaseScopeA);

        var phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        valueSelector.phaseStarted(phaseScopeB);

        var stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB1);
        assertAllCodesOfValueSelector(valueSelector, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeB1);

        var stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB2);
        assertAllCodesOfValueSelector(valueSelector, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeB2);

        var stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB3);
        assertAllCodesOfValueSelector(valueSelector, 4L, "v1", "v2", "v4");
        valueSelector.stepEnded(stepScopeB3);

        valueSelector.phaseEnded(phaseScopeB);

        valueSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childValueSelector, 1, 2, 5);
        verify(childValueSelector, times(5)).iterator();
        verify(childValueSelector, times(5)).getSize();
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
        var childValueSelector = (IterableValueSelector<TestdataSolution>) mock(IterableValueSelector.class);
        when(childValueSelector.isNeverEnding()).thenReturn(true);
        when(childValueSelector.getSize()).thenReturn(size);
        when(childValueSelector.iterator()).thenReturn(iterator);
        var valueSelector =
                (IterableValueSelector<TestdataSolution>) FilteringValueSelector.of(childValueSelector,
                        (scoreDirector, value) -> false);
        assertThat(valueSelector.iterator().hasNext()).isFalse();
        assertThat(nextCount).hasValue(BailOutUtils.UNKNOWN_SIZE_BAIL_OUT);
    }
}
