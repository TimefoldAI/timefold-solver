package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfIterator;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfValueSelector;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.common.TestdataObjectSorter;
import ai.timefold.solver.core.impl.heuristic.selector.value.FromEntityPropertyValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class IterableFromEntityPropertyValueSelectorTest {

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
        var valueRangeDescriptor = TestdataEntityProvidingEntity.buildVariableDescriptorForValue();
        var fromEntityPropertySelector =
                new FromEntityPropertyValueSelector<>(
                        valueRangeDescriptor.getValueRangeDescriptor(), new TestdataObjectSorter<>(), false);
        var valueSelector = new IterableFromEntityPropertyValueSelector<>(fromEntityPropertySelector, cacheType, false);

        var solution = new TestdataEntityProvidingSolution();
        var firstEntity = new TestdataEntityProvidingEntity();
        firstEntity.setValueRange(List.of(new TestdataValue("jan"), new TestdataValue("feb"), new TestdataValue("mar")));
        var secondEntity = new TestdataEntityProvidingEntity();
        secondEntity.setValueRange(List.of(new TestdataValue("apr"), new TestdataValue("may"), new TestdataValue("jun")));
        solution.setEntityList(List.of(firstEntity, secondEntity));
        var solverScope = mock(SolverScope.class);
        InnerScoreDirector<?, ?> scoreDirector = mock(InnerScoreDirector.class);
        doReturn(scoreDirector).when(solverScope).getScoreDirector();
        doReturn(solution).when(scoreDirector).getWorkingSolution();
        doReturn(ValueRangeManager.of(TestdataEntityProvidingSolution.buildSolutionDescriptor(), solution)).when(scoreDirector)
                .getValueRangeManager();
        valueSelector.solvingStarted(solverScope);

        var phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        when(phaseScopeA.getScoreDirector()).thenReturn(scoreDirector);
        valueSelector.phaseStarted(phaseScopeA);

        var stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA1);
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        assertAllCodesOfIterator(valueSelector.iterator(firstEntity), "feb", "jan", "mar");
        assertAllCodesOfIterator(valueSelector.iterator(secondEntity), "apr", "jun", "may");
        valueSelector.stepEnded(stepScopeA1);

        var stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA2);
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        assertAllCodesOfIterator(valueSelector.iterator(firstEntity), "feb", "jan", "mar");
        assertAllCodesOfIterator(valueSelector.iterator(secondEntity), "apr", "jun", "may");
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
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        assertAllCodesOfIterator(valueSelector.iterator(firstEntity), "feb", "jan", "mar");
        assertAllCodesOfIterator(valueSelector.iterator(secondEntity), "apr", "jun", "may");
        valueSelector.stepEnded(stepScopeB1);

        var stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB2);
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        assertAllCodesOfIterator(valueSelector.iterator(firstEntity), "feb", "jan", "mar");
        assertAllCodesOfIterator(valueSelector.iterator(secondEntity), "apr", "jun", "may");
        valueSelector.stepEnded(stepScopeB2);

        var stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB3);
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        assertAllCodesOfIterator(valueSelector.iterator(firstEntity), "feb", "jan", "mar");
        assertAllCodesOfIterator(valueSelector.iterator(secondEntity), "apr", "jun", "may");
        valueSelector.stepEnded(stepScopeB3);

        valueSelector.phaseEnded(phaseScopeB);

        valueSelector.solvingEnded(solverScope);
    }
}
