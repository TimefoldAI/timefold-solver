package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.chained.SubChainSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedSolution;

import org.junit.jupiter.api.Test;

class SubChainSwapMoveSelectorFactoryTest {

    @Test
    void buildBaseMoveSelector() {
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig("chainedObject");
        SubChainSelectorConfig leftSubChainSelectorConfig = new SubChainSelectorConfig();
        leftSubChainSelectorConfig.setValueSelectorConfig(valueSelectorConfig);
        SubChainSelectorConfig rightSubChainSelectorConfig = new SubChainSelectorConfig();
        rightSubChainSelectorConfig.setValueSelectorConfig(valueSelectorConfig);
        SubChainSwapMoveSelectorConfig config = new SubChainSwapMoveSelectorConfig();
        config.setSubChainSelectorConfig(leftSubChainSelectorConfig);
        config.setSecondarySubChainSelectorConfig(rightSubChainSelectorConfig);
        SubChainSwapMoveSelectorFactory<TestdataChainedSolution> factory =
                new SubChainSwapMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataChainedSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataChainedSolution.buildSolutionDescriptor());

        SubChainSwapMoveSelector<TestdataChainedSolution> selector = (SubChainSwapMoveSelector<TestdataChainedSolution>) factory
                .buildBaseMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME, true);
        assertThat(selector.leftSubChainSelector).isNotNull();
        assertThat(selector.rightSubChainSelector).isNotNull();
        assertThat(selector.variableDescriptor).isNotNull();
        assertThat(selector.randomSelection).isTrue();
    }
}
