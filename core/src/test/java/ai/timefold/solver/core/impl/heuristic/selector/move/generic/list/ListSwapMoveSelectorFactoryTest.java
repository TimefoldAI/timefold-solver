package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;

class ListSwapMoveSelectorFactoryTest {

    @Test
    void noUnfolding() {
        SolutionDescriptor<TestdataListSolution> solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        ListSwapMoveSelectorConfig moveSelectorConfig = new ListSwapMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig("valueList"));
        MoveSelector<TestdataListSolution> moveSelector =
                MoveSelectorFactory.<TestdataListSolution> create(moveSelectorConfig).buildMoveSelector(
                        buildHeuristicConfigPolicy(solutionDescriptor), SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false);
        assertThat(moveSelector).isInstanceOf(ListSwapMoveSelector.class);
    }

    @Test
    void unfoldedSingleListVariable() {
        SolutionDescriptor<TestdataListSolution> solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        ListSwapMoveSelectorConfig moveSelectorConfig = new ListSwapMoveSelectorConfig();
        MoveSelector<TestdataListSolution> moveSelector =
                MoveSelectorFactory.<TestdataListSolution> create(moveSelectorConfig).buildMoveSelector(
                        buildHeuristicConfigPolicy(solutionDescriptor), SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false);
        assertThat(moveSelector).isInstanceOf(ListSwapMoveSelector.class);
    }

    @Test
    void unfoldingFailsIfThereIsNoListVariable() {
        ListSwapMoveSelectorConfig config = new ListSwapMoveSelectorConfig();
        ListSwapMoveSelectorFactory<TestdataSolution> moveSelectorFactory = new ListSwapMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(heuristicConfigPolicy,
                        SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false))
                .withMessageContaining("cannot unfold");
    }

    @Test
    void explicitConfigMustUseListVariable() {
        ListSwapMoveSelectorConfig config = new ListSwapMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig("value"));

        ListSwapMoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new ListSwapMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(heuristicConfigPolicy,
                        SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false))
                .withMessageContaining("not a planning list variable");
    }

    // TODO test or remove secondary value selector config
}
