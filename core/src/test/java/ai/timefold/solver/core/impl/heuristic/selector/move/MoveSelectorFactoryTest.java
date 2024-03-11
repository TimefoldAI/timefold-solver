package ai.timefold.solver.core.impl.heuristic.selector.move;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Comparator;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.move.DummyMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.CachingMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.FilteringMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.ProbabilityMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.ShufflingMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.SortingMoveSelector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class MoveSelectorFactoryTest {

    @Test
    void phaseOriginal() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        moveSelectorConfig.setCacheType(SelectionCacheType.PHASE);
        moveSelectorConfig.setSelectionOrder(SelectionOrder.ORIGINAL);
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new AssertingMoveSelectorFactory(moveSelectorConfig, baseMoveSelector, SelectionCacheType.PHASE, false);
        MoveSelector<TestdataSolution> moveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector)
                .isInstanceOf(CachingMoveSelector.class)
                .isNotInstanceOf(ShufflingMoveSelector.class);
        assertThat(moveSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
        assertThat(((CachingMoveSelector<TestdataSolution>) moveSelector).getChildMoveSelector())
                .isSameAs(baseMoveSelector);
    }

    @Test
    void stepOriginal() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        moveSelectorConfig.setCacheType(SelectionCacheType.STEP);
        moveSelectorConfig.setSelectionOrder(SelectionOrder.ORIGINAL);
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new AssertingMoveSelectorFactory(moveSelectorConfig, baseMoveSelector, SelectionCacheType.STEP, false);
        MoveSelector<TestdataSolution> moveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector)
                .isInstanceOf(CachingMoveSelector.class)
                .isNotInstanceOf(ShufflingMoveSelector.class);
        assertThat(moveSelector.getCacheType()).isEqualTo(SelectionCacheType.STEP);
        assertThat(((CachingMoveSelector<TestdataSolution>) moveSelector).getChildMoveSelector())
                .isSameAs(baseMoveSelector);
    }

    @Test
    void justInTimeOriginal() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new AssertingMoveSelectorFactory(moveSelectorConfig, baseMoveSelector, SelectionCacheType.JUST_IN_TIME, false);
        moveSelectorConfig.setCacheType(SelectionCacheType.JUST_IN_TIME);
        moveSelectorConfig.setSelectionOrder(SelectionOrder.ORIGINAL);
        MoveSelector<TestdataSolution> moveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector).isSameAs(baseMoveSelector);
        assertThat(moveSelector.getCacheType()).isEqualTo(SelectionCacheType.JUST_IN_TIME);
    }

    @Test
    void phaseRandom() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new AssertingMoveSelectorFactory(moveSelectorConfig, baseMoveSelector, SelectionCacheType.PHASE, false);
        moveSelectorConfig.setCacheType(SelectionCacheType.PHASE);
        moveSelectorConfig.setSelectionOrder(SelectionOrder.RANDOM);
        MoveSelector<TestdataSolution> moveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector)
                .isInstanceOf(CachingMoveSelector.class)
                .isNotInstanceOf(ShufflingMoveSelector.class);
        assertThat(moveSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
        assertThat(((CachingMoveSelector<TestdataSolution>) moveSelector).getChildMoveSelector())
                .isSameAs(baseMoveSelector);
    }

    @Test
    void stepRandom() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new AssertingMoveSelectorFactory(moveSelectorConfig, baseMoveSelector, SelectionCacheType.STEP, false);
        moveSelectorConfig.setCacheType(SelectionCacheType.STEP);
        moveSelectorConfig.setSelectionOrder(SelectionOrder.RANDOM);
        MoveSelector<TestdataSolution> moveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector)
                .isInstanceOf(CachingMoveSelector.class)
                .isNotInstanceOf(ShufflingMoveSelector.class);
        assertThat(moveSelector.getCacheType()).isEqualTo(SelectionCacheType.STEP);
        assertThat(((CachingMoveSelector<TestdataSolution>) moveSelector).getChildMoveSelector())
                .isSameAs(baseMoveSelector);
    }

    @Test
    void justInTimeRandom() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new AssertingMoveSelectorFactory(moveSelectorConfig, baseMoveSelector, SelectionCacheType.JUST_IN_TIME, true);
        moveSelectorConfig.setCacheType(SelectionCacheType.JUST_IN_TIME);
        moveSelectorConfig.setSelectionOrder(SelectionOrder.RANDOM);
        MoveSelector<TestdataSolution> moveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector).isSameAs(baseMoveSelector);
        assertThat(moveSelector.getCacheType()).isEqualTo(SelectionCacheType.JUST_IN_TIME);
    }

    @Test
    void phaseShuffled() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new AssertingMoveSelectorFactory(moveSelectorConfig, baseMoveSelector, SelectionCacheType.PHASE, false);
        moveSelectorConfig.setCacheType(SelectionCacheType.PHASE);
        moveSelectorConfig.setSelectionOrder(SelectionOrder.SHUFFLED);
        MoveSelector<TestdataSolution> moveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector)
                .isInstanceOf(ShufflingMoveSelector.class);
        assertThat(moveSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
        assertThat(((ShufflingMoveSelector<TestdataSolution>) moveSelector).getChildMoveSelector())
                .isSameAs(baseMoveSelector);
    }

    @Test
    void stepShuffled() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new AssertingMoveSelectorFactory(moveSelectorConfig, baseMoveSelector, SelectionCacheType.STEP, false);
        moveSelectorConfig.setCacheType(SelectionCacheType.STEP);
        moveSelectorConfig.setSelectionOrder(SelectionOrder.SHUFFLED);
        MoveSelector<TestdataSolution> moveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector)
                .isInstanceOf(ShufflingMoveSelector.class);
        assertThat(moveSelector.getCacheType()).isEqualTo(SelectionCacheType.STEP);
        assertThat(((ShufflingMoveSelector) moveSelector).getChildMoveSelector()).isSameAs(baseMoveSelector);
    }

    @Test
    void justInTimeShuffled() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        moveSelectorConfig.setCacheType(SelectionCacheType.JUST_IN_TIME);
        moveSelectorConfig.setSelectionOrder(SelectionOrder.SHUFFLED);
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory = new DummyMoveSelectorFactory(moveSelectorConfig,
                baseMoveSelector);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                        SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false));
    }

    @Test
    void validateSorting_incompatibleSelectionOrder() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        moveSelectorConfig.setSorterOrder(SelectionSorterOrder.ASCENDING);

        DummyMoveSelectorFactory moveSelectorFactory = new DummyMoveSelectorFactory(moveSelectorConfig, baseMoveSelector);
        assertThatIllegalArgumentException().isThrownBy(() -> moveSelectorFactory.validateSorting(SelectionOrder.RANDOM))
                .withMessageContaining("that is not " + SelectionOrder.SORTED);
    }

    @Test
    void applySorting_withoutAnySortingClass() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        moveSelectorConfig.setSorterOrder(SelectionSorterOrder.ASCENDING);

        DummyMoveSelectorFactory moveSelectorFactory = new DummyMoveSelectorFactory(moveSelectorConfig, baseMoveSelector);
        assertThatIllegalArgumentException().isThrownBy(
                () -> moveSelectorFactory.applySorting(SelectionCacheType.PHASE, SelectionOrder.SORTED, baseMoveSelector))
                .withMessageContaining("The moveSelectorConfig")
                .withMessageContaining("needs");
    }

    @Test
    void applySorting_withSorterComparatorClass() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        moveSelectorConfig.setSorterOrder(SelectionSorterOrder.ASCENDING);
        moveSelectorConfig.setSorterComparatorClass(DummyComparator.class);

        DummyMoveSelectorFactory moveSelectorFactory = new DummyMoveSelectorFactory(moveSelectorConfig, baseMoveSelector);
        MoveSelector<TestdataSolution> sortingMoveSelector =
                moveSelectorFactory.applySorting(SelectionCacheType.PHASE, SelectionOrder.SORTED, baseMoveSelector);
        assertThat(sortingMoveSelector).isExactlyInstanceOf(SortingMoveSelector.class);
    }

    @Test
    void applyProbability_withProbabilityWeightFactoryClass() {
        final MoveSelector<TestdataSolution> baseMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        moveSelectorConfig.setCacheType(SelectionCacheType.PHASE);
        moveSelectorConfig.setProbabilityWeightFactoryClass(DummySelectionProbabilityWeightFactory.class);

        DummyMoveSelectorFactory moveSelectorFactory = new DummyMoveSelectorFactory(moveSelectorConfig, baseMoveSelector);
        MoveSelector<TestdataSolution> sortingMoveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.PHASE, SelectionOrder.PROBABILISTIC, false);
        assertThat(sortingMoveSelector).isExactlyInstanceOf(ProbabilityMoveSelector.class);
    }

    @Test
    void applyFilter_nonMovableMoves() {
        Move<TestdataSolution> notDoableMove = new Move<>() {
            @Override
            public boolean isMoveDoable(ScoreDirector<TestdataSolution> scoreDirector) {
                return false;
            }

            @Override
            public Move<TestdataSolution> doMove(ScoreDirector<TestdataSolution> scoreDirector) {
                return null;
            }
        };
        final MoveSelector<TestdataSolution> baseMoveSelector =
                SelectorTestUtils.mockMoveSelector(DummyMove.class, notDoableMove);
        DummyMoveSelectorConfig moveSelectorConfig = new DummyMoveSelectorConfig();
        MoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new DummyMoveSelectorFactory(moveSelectorConfig, baseMoveSelector);
        MoveSelector<TestdataSolution> moveSelector = moveSelectorFactory.buildMoveSelector(buildHeuristicConfigPolicy(),
                SelectionCacheType.PHASE, SelectionOrder.RANDOM, true);
        assertThat(moveSelector)
                .isInstanceOf(FilteringMoveSelector.class);
        assertThat(moveSelector.iterator().hasNext()).isFalse();
    }

    static class DummyMoveSelectorConfig extends MoveSelectorConfig<DummyMoveSelectorConfig> {

        @Override
        public DummyMoveSelectorConfig copyConfig() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNearbySelectionConfig() {
            return false;
        }
    }

    static class DummyMoveSelectorFactory extends AbstractMoveSelectorFactory<TestdataSolution, DummyMoveSelectorConfig> {

        protected final MoveSelector<TestdataSolution> baseMoveSelector;

        DummyMoveSelectorFactory(DummyMoveSelectorConfig moveSelectorConfig,
                MoveSelector<TestdataSolution> baseMoveSelector) {
            super(moveSelectorConfig);
            this.baseMoveSelector = baseMoveSelector;
        }

        @Override
        protected MoveSelector<TestdataSolution> buildBaseMoveSelector(HeuristicConfigPolicy configPolicy,
                SelectionCacheType minimumCacheType,
                boolean randomSelection) {
            return baseMoveSelector;
        }
    }

    static class AssertingMoveSelectorFactory extends DummyMoveSelectorFactory {

        private final SelectionCacheType expectedMinimumCacheType;
        private final boolean expectedRandomSelection;

        AssertingMoveSelectorFactory(DummyMoveSelectorConfig moveSelectorConfig,
                MoveSelector<TestdataSolution> baseMoveSelector, SelectionCacheType expectedMinimumCacheType,
                boolean expectedRandomSelection) {
            super(moveSelectorConfig, baseMoveSelector);
            this.expectedMinimumCacheType = expectedMinimumCacheType;
            this.expectedRandomSelection = expectedRandomSelection;
        }

        @Override
        protected MoveSelector<TestdataSolution> buildBaseMoveSelector(HeuristicConfigPolicy configPolicy,
                SelectionCacheType minimumCacheType, boolean randomSelection) {
            assertThat(minimumCacheType).isEqualTo(expectedMinimumCacheType);
            assertThat(randomSelection).isEqualTo(expectedRandomSelection);
            return baseMoveSelector;
        }
    }

    public static class DummyComparator implements Comparator<Object> {

        @Override
        public int compare(Object o, Object t1) {
            return 0;
        }
    }

    public static class DummySelectionProbabilityWeightFactory
            implements SelectionProbabilityWeightFactory<TestdataSolution, MoveSelector<TestdataSolution>> {

        @Override
        public double createProbabilityWeight(ScoreDirector<TestdataSolution> scoreDirector,
                MoveSelector<TestdataSolution> selection) {
            return 0.0;
        }
    }
}
