package ai.timefold.solver.enterprise.nearby.value;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;

class ValueSelectorFactoryTest {

    @Test
    void failFast_ifNearbyDoesNotHaveOriginEntityOrValueSelector() {
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withNearbySelectionConfig(new NearbySelectionConfig()
                        .withOriginSubListSelectorConfig(new SubListSelectorConfig().withMimicSelectorRef("x"))
                        .withNearbyDistanceMeterClass(mock(NearbyDistanceMeter.class).getClass()));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> ValueSelectorFactory.<TestdataListSolution> create(valueSelectorConfig)
                        .buildValueSelector(buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor()),
                                TestdataListEntity.buildEntityDescriptor(),
                                SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM))
                .withMessageContaining("requires an originEntitySelector or an originValueSelector");
    }

}
