package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;

class DestinationSelectorFactoryTest {

    @Test
    void failFast_ifNearbyDoesNotHaveOriginSubListOrValueSelector() {
        DestinationSelectorConfig destinationSelectorConfig = new DestinationSelectorConfig()
                .withEntitySelectorConfig(new EntitySelectorConfig())
                .withValueSelectorConfig(new ValueSelectorConfig())
                .withNearbySelectionConfig(new NearbySelectionConfig()
                        .withOriginEntitySelectorConfig(new EntitySelectorConfig().withMimicSelectorRef("x"))
                        .withNearbyDistanceMeterClass(mock(NearbyDistanceMeter.class).getClass()));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> DestinationSelectorFactory.<TestdataListSolution> create(destinationSelectorConfig)
                        .buildDestinationSelector(buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor()),
                                SelectionCacheType.JUST_IN_TIME, true))
                .withMessageContaining("requires an originSubListSelector or an originValueSelector");
    }
}
