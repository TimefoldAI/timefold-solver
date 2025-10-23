package ai.timefold.solver.core.impl.heuristic.selector.move.decorator;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.move.DummyMove;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testutil.CodeAssertable;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SortingMoveSelectorTest {

    @Test
    void cacheTypeSolver() {
        runCacheType(SelectionCacheType.SOLVER, 1);
    }

    @Test
    void cacheTypePhase() {
        runCacheType(SelectionCacheType.PHASE, 2);
    }

    @Test
    void cacheTypeStep() {
        runCacheType(SelectionCacheType.STEP, 5);
    }

    @Test
    void cacheTypeJustInTime() {
        assertThatIllegalArgumentException().isThrownBy(() -> runCacheType(SelectionCacheType.JUST_IN_TIME, 5));
    }

    private static List<DummySorterMoveSelectorConfig> generateConfiguration() {
        return List.of(
                new DummySorterMoveSelectorConfig()
                        .withSorterOrder(SelectionSorterOrder.ASCENDING)
                        .withSorterWeightFactoryClass(TestCodeAssertableComparatorFactory.class),
                new DummySorterMoveSelectorConfig()
                        .withSorterOrder(SelectionSorterOrder.ASCENDING)
                        .withSorterComparatorClass(TestCodeAssertableComparator.class),
                new DummySorterMoveSelectorConfig()
                        .withSorterOrder(SelectionSorterOrder.ASCENDING)
                        .withComparatorFactoryClass(TestCodeAssertableComparatorFactory.class),
                new DummySorterMoveSelectorConfig()
                        .withSorterOrder(SelectionSorterOrder.ASCENDING)
                        .withComparatorClass(TestCodeAssertableComparator.class));
    }

    @ParameterizedTest
    @MethodSource("generateConfiguration")
    void applySorting(DummySorterMoveSelectorConfig moveSelectorConfig) {
        var baseMoveSelector = SelectorTestUtils.mockMoveSelector(
                new DummyMove("jan"), new DummyMove("feb"), new DummyMove("mar"),
                new DummyMove("apr"), new DummyMove("may"), new DummyMove("jun"));
        var moveSelectorFactory = new DummySorterMoveSelectorFactory(moveSelectorConfig, baseMoveSelector);
        var moveSelector =
                moveSelectorFactory.buildBaseMoveSelector(buildHeuristicConfigPolicy(), SelectionCacheType.PHASE, false);

        var scoreDirector = mockScoreDirector(TestdataSolution.buildSolutionDescriptor());
        var solverScope = mock(SolverScope.class);
        when(solverScope.getScoreDirector()).thenReturn(scoreDirector);
        moveSelector.solvingStarted(solverScope);

        var phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScope);

        var stepScopeA = mock(AbstractStepScope.class);
        when(stepScopeA.getPhaseScope()).thenReturn(phaseScope);
        moveSelector.stepStarted(stepScopeA);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
    }

    public void runCacheType(SelectionCacheType cacheType, int timesCalled) {
        MoveSelector childMoveSelector = SelectorTestUtils.mockMoveSelector(
                new DummyMove("jan"), new DummyMove("feb"), new DummyMove("mar"),
                new DummyMove("apr"), new DummyMove("may"), new DummyMove("jun"));

        SelectionSorter<TestdataSolution, DummyMove> sorter = (scoreDirector, selectionList) -> selectionList
                .sort(Comparator.comparing(DummyMove::getCode));
        MoveSelector moveSelector = new SortingMoveSelector(childMoveSelector, cacheType, sorter);

        SolverScope solverScope = mock(SolverScope.class);
        moveSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA2);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeA2);

        moveSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB1);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB2);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB3);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeB3);

        moveSelector.phaseEnded(phaseScopeB);

        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childMoveSelector, 1, 2, 5);
        verify(childMoveSelector, times(timesCalled)).iterator();
        verify(childMoveSelector, times(timesCalled)).getSize();
    }

    private static class DummySorterMoveSelectorConfig extends MoveSelectorConfig<DummySorterMoveSelectorConfig> {

        @Override
        public @NonNull DummySorterMoveSelectorConfig copyConfig() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNearbySelectionConfig() {
            return false;
        }
    }

    private static class DummySorterMoveSelectorFactory
            extends AbstractMoveSelectorFactory<TestdataSolution, DummySorterMoveSelectorConfig> {

        protected final MoveSelector<TestdataSolution> baseMoveSelector;

        DummySorterMoveSelectorFactory(DummySorterMoveSelectorConfig moveSelectorConfig,
                MoveSelector<TestdataSolution> baseMoveSelector) {
            super(moveSelectorConfig);
            this.baseMoveSelector = baseMoveSelector;
        }

        @Override
        protected MoveSelector<TestdataSolution> buildBaseMoveSelector(HeuristicConfigPolicy configPolicy,
                SelectionCacheType minimumCacheType,
                boolean randomSelection) {
            return applySorting(minimumCacheType, SelectionOrder.SORTED, baseMoveSelector);
        }
    }

    public static class TestCodeAssertableComparatorFactory implements SelectionSorterWeightFactory<Object, CodeAssertable> {

        @Override
        public Comparable createSorterWeight(Object o, CodeAssertable selection) {
            return selection.getCode();
        }
    }

    public static class TestCodeAssertableComparator implements Comparator<CodeAssertable> {
        @Override
        public int compare(CodeAssertable v1, CodeAssertable v2) {
            return v1.getCode().compareTo(v2.getCode());
        }
    }

}
