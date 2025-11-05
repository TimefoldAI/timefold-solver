package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfValueSelector;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.selector.common.TestdataObjectSorter;
import ai.timefold.solver.core.impl.heuristic.selector.value.FromEntityPropertyValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.ManualValueMimicRecorder;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.MimicReplayingValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.junit.jupiter.api.Test;

class FilteringValueRangeSelectorTest {

    @Test
    void originalSelectionCacheTypeSolver() {
        runOriginalSelection(SelectionCacheType.SOLVER);
    }

    @Test
    void originalSelectionCacheTypePhase() {
        runOriginalSelection(SelectionCacheType.PHASE);
    }

    @Test
    void originalSelectionCacheTypeStep() {
        runOriginalSelection(SelectionCacheType.STEP);
    }

    public void runOriginalSelection(SelectionCacheType cacheType) {
        var valueRangeDescriptor = TestdataListEntityProvidingEntity.buildVariableDescriptorForValueList();
        var fromEntityPropertySelector =
                new FromEntityPropertyValueSelector<>(
                        valueRangeDescriptor.getValueRangeDescriptor(), new TestdataObjectSorter<>(), false);
        var iterableValueSelector = new IterableFromEntityPropertyValueSelector<>(fromEntityPropertySelector, cacheType, false);
        var mimicRecorder = new ManualValueMimicRecorder<>(iterableValueSelector);
        var replayingValueSelector = new MimicReplayingValueSelector<>(mimicRecorder);
        var valueSelector = new FilteringValueRangeSelector<>(iterableValueSelector, replayingValueSelector,
                false, false);

        var solution = new TestdataListEntityProvidingSolution();
        var jan = new TestdataListEntityProvidingValue("jan");
        var feb = new TestdataListEntityProvidingValue("feb");
        var mar = new TestdataListEntityProvidingValue("mar");
        var apr = new TestdataListEntityProvidingValue("apr");
        var may = new TestdataListEntityProvidingValue("may");
        var jun = new TestdataListEntityProvidingValue("jun");
        var firstEntity = new TestdataListEntityProvidingEntity("e1", List.of(jan, feb, mar));
        var secondEntity = new TestdataListEntityProvidingEntity("e2", List.of(apr, may, jun));
        solution.setEntityList(List.of(firstEntity, secondEntity));

        var solverScope = mock(SolverScope.class);
        InnerScoreDirector<?, ?> scoreDirector = mock(InnerScoreDirector.class);
        doReturn(scoreDirector).when(solverScope).getScoreDirector();
        doReturn(solution).when(scoreDirector).getWorkingSolution();
        doReturn(ValueRangeManager.of(TestdataListEntityProvidingSolution.buildSolutionDescriptor(), solution))
                .when(scoreDirector)
                .getValueRangeManager();
        var listVariableSupply = mock(ListVariableStateSupply.class);
        doReturn(listVariableSupply).when(scoreDirector).getListVariableStateSupply(any());
        doReturn(TestdataListEntityProvidingEntity.buildVariableDescriptorForValueList()).when(listVariableSupply)
                .getSourceVariableDescriptor();
        valueSelector.solvingStarted(solverScope);

        var phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        when(phaseScopeA.getScoreDirector()).thenReturn(scoreDirector);
        valueSelector.phaseStarted(phaseScopeA);

        var stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA1);
        mimicRecorder.setRecordedValue(jan);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "mar");
        mimicRecorder.setRecordedValue(feb);
        assertAllCodesOfValueSelector(valueSelector, 6, "jan", "mar");
        mimicRecorder.setRecordedValue(mar);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "jan");
        mimicRecorder.setRecordedValue(apr);
        assertAllCodesOfValueSelector(valueSelector, 6, "jun", "may");
        mimicRecorder.setRecordedValue(may);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "jun");
        mimicRecorder.setRecordedValue(jun);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "may");
        valueSelector.stepEnded(stepScopeA1);

        var stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA2);
        mimicRecorder.setRecordedValue(jan);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "mar");
        mimicRecorder.setRecordedValue(feb);
        assertAllCodesOfValueSelector(valueSelector, 6, "jan", "mar");
        mimicRecorder.setRecordedValue(mar);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "jan");
        mimicRecorder.setRecordedValue(apr);
        assertAllCodesOfValueSelector(valueSelector, 6, "jun", "may");
        mimicRecorder.setRecordedValue(may);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "jun");
        mimicRecorder.setRecordedValue(jun);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "may");
        valueSelector.stepEnded(stepScopeA2);

        valueSelector.phaseEnded(phaseScopeA);

        var phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        when(phaseScopeB.getScoreDirector()).thenReturn(scoreDirector);

        valueSelector.phaseStarted(phaseScopeB);

        var stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB1);
        mimicRecorder.setRecordedValue(jan);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "mar");
        mimicRecorder.setRecordedValue(feb);
        assertAllCodesOfValueSelector(valueSelector, 6, "jan", "mar");
        mimicRecorder.setRecordedValue(mar);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "jan");
        mimicRecorder.setRecordedValue(apr);
        assertAllCodesOfValueSelector(valueSelector, 6, "jun", "may");
        mimicRecorder.setRecordedValue(may);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "jun");
        mimicRecorder.setRecordedValue(jun);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "may");
        valueSelector.stepEnded(stepScopeB1);

        var stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB2);
        mimicRecorder.setRecordedValue(jan);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "mar");
        mimicRecorder.setRecordedValue(feb);
        assertAllCodesOfValueSelector(valueSelector, 6, "jan", "mar");
        mimicRecorder.setRecordedValue(mar);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "jan");
        mimicRecorder.setRecordedValue(apr);
        assertAllCodesOfValueSelector(valueSelector, 6, "jun", "may");
        mimicRecorder.setRecordedValue(may);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "jun");
        mimicRecorder.setRecordedValue(jun);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "may");
        valueSelector.stepEnded(stepScopeB2);

        var stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB3);
        mimicRecorder.setRecordedValue(jan);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "mar");
        mimicRecorder.setRecordedValue(feb);
        assertAllCodesOfValueSelector(valueSelector, 6, "jan", "mar");
        mimicRecorder.setRecordedValue(mar);
        assertAllCodesOfValueSelector(valueSelector, 6, "feb", "jan");
        mimicRecorder.setRecordedValue(apr);
        assertAllCodesOfValueSelector(valueSelector, 6, "jun", "may");
        mimicRecorder.setRecordedValue(may);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "jun");
        mimicRecorder.setRecordedValue(jun);
        assertAllCodesOfValueSelector(valueSelector, 6, "apr", "may");
        valueSelector.stepEnded(stepScopeB3);

        valueSelector.phaseEnded(phaseScopeB);

        valueSelector.solvingEnded(solverScope);
    }
}
