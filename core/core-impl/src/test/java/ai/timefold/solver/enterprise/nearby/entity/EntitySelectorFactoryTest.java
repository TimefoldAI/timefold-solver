package ai.timefold.solver.enterprise.nearby.entity;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class EntitySelectorFactoryTest {
    @Test
    void failFast_ifNearbyDoesNotHaveOriginEntitySelector() {
        EntitySelectorConfig entitySelectorConfig = new EntitySelectorConfig()
                .withNearbySelectionConfig(new NearbySelectionConfig()
                        .withOriginValueSelectorConfig(new ValueSelectorConfig().withMimicSelectorRef("x"))
                        .withNearbyDistanceMeterClass(NearbyDistanceMeter.class));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> EntitySelectorFactory.<TestdataSolution> create(entitySelectorConfig).buildEntitySelector(
                        buildHeuristicConfigPolicy(), SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM))
                .withMessageContaining("requires an originEntitySelector");
    }

}
