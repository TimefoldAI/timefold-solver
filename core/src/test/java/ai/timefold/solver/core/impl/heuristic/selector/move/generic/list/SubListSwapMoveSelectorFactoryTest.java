package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntitySolution;

import org.junit.jupiter.api.Test;

class SubListSwapMoveSelectorFactoryTest {

    @Test
    void buildBaseMoveSelector() {
        var config = new SubListSwapMoveSelectorConfig();
        var factory = new SubListSwapMoveSelectorFactory<TestdataListSolution>(config);

        var heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        var selector =
                (RandomSubListSwapMoveSelector<TestdataListSolution>) factory.buildBaseMoveSelector(heuristicConfigPolicy,
                        SelectionCacheType.JUST_IN_TIME, true);

        assertThat(selector.isNeverEnding()).isTrue();
        assertThat(selector.isSelectReversingMoveToo()).isTrue();
    }

    @Test
    void buildMoveSelectorMultiEntity() {
        var config = new SubListSwapMoveSelectorConfig();
        var factory = new SubListSwapMoveSelectorFactory<TestdataMixedMultiEntitySolution>(config);

        var heuristicConfigPolicy = buildHeuristicConfigPolicy(TestdataMixedMultiEntitySolution.buildSolutionDescriptor());

        var selector =
                (RandomSubListSwapMoveSelector<TestdataMixedMultiEntitySolution>) factory.buildBaseMoveSelector(
                        heuristicConfigPolicy,
                        SelectionCacheType.JUST_IN_TIME, true);

        assertThat(selector.isNeverEnding()).isTrue();
        assertThat(selector.isSelectReversingMoveToo()).isTrue();
    }

    @Test
    void unfoldingFailsIfThereIsNoListVariable() {
        var config = new SubListSwapMoveSelectorConfig();
        var moveSelectorFactory = new SubListSwapMoveSelectorFactory<TestdataSolution>(config);

        var heuristicConfigPolicy = buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME,
                        SelectionOrder.RANDOM, false))
                .withMessageContaining("it cannot be deduced automatically");
    }

    @Test
    void disableSelectReversingMoveToo() {
        var config = new SubListSwapMoveSelectorConfig();
        config.setSelectReversingMoveToo(false);
        var factory = new SubListSwapMoveSelectorFactory<TestdataListSolution>(config);

        var heuristicConfigPolicy = buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        var selector =
                (RandomSubListSwapMoveSelector<TestdataListSolution>) factory.buildBaseMoveSelector(heuristicConfigPolicy,
                        SelectionCacheType.JUST_IN_TIME, true);

        assertThat(selector.isSelectReversingMoveToo()).isFalse();
    }

}
