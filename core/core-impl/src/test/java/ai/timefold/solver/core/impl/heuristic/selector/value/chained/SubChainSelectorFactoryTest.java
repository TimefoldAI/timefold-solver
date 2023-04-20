package ai.timefold.solver.core.impl.heuristic.selector.value.chained;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.chained.SubChainSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedEntity;

import org.junit.jupiter.api.Test;

class SubChainSelectorFactoryTest {

    @Test
    void buildSubChainSelector() {
        SubChainSelectorConfig config = new SubChainSelectorConfig();
        config.setMinimumSubChainSize(2);
        config.setMaximumSubChainSize(3);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig("chainedObject");
        config.setValueSelectorConfig(valueSelectorConfig);
        HeuristicConfigPolicy heuristicConfigPolicy = mock(HeuristicConfigPolicy.class);
        EntityDescriptor entityDescriptor = TestdataChainedEntity.buildEntityDescriptor();
        DefaultSubChainSelector subChainSelector =
                (DefaultSubChainSelector) SubChainSelectorFactory.create(config).buildSubChainSelector(heuristicConfigPolicy,
                        entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);
        assertThat(subChainSelector.maximumSubChainSize).isEqualTo(config.getMaximumSubChainSize());
        assertThat(subChainSelector.minimumSubChainSize).isEqualTo(config.getMinimumSubChainSize());
        assertThat(subChainSelector.randomSelection).isTrue();
    }
}
