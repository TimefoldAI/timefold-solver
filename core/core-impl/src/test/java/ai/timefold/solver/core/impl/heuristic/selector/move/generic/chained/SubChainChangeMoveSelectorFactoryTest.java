package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedSolution.buildSolutionDescriptor;
import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.chained.SubChainSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedSolution;

import org.junit.jupiter.api.Test;

class SubChainChangeMoveSelectorFactoryTest {

    @Test
    void buildBaseMoveSelector() {
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig("chainedObject");
        SubChainSelectorConfig subChainSelectorConfig = new SubChainSelectorConfig();
        subChainSelectorConfig.setValueSelectorConfig(valueSelectorConfig);

        SubChainChangeMoveSelectorConfig config = new SubChainChangeMoveSelectorConfig();
        config.setSubChainSelectorConfig(subChainSelectorConfig);
        config.setValueSelectorConfig(valueSelectorConfig);
        SubChainChangeMoveSelectorFactory<TestdataChainedSolution> factory =
                new SubChainChangeMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataChainedSolution> heuristicConfigPolicy =
                HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy(buildSolutionDescriptor());

        SubChainChangeMoveSelector<TestdataChainedSolution> selector =
                (SubChainChangeMoveSelector<TestdataChainedSolution>) factory
                        .buildBaseMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME, true);
        assertThat(selector.subChainSelector).isNotNull();
        assertThat(selector.valueSelector).isNotNull();
        assertThat(selector.randomSelection).isTrue();
    }

}
