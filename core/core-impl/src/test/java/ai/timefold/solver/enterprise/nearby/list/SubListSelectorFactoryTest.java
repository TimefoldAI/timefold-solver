package ai.timefold.solver.enterprise.nearby.list;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SubListSelectorFactoryTest {

    @ParameterizedTest
    @MethodSource("limitedDistributionConfigs")
    void failFast_ifMinimumSubListSizeUsedTogetherWithLimitedDistribution(NearbySelectionConfig nearbySelectionConfig,
            String violatingPropertyName) {
        SubListSelectorConfig config = new SubListSelectorConfig()
                .withMinimumSubListSize(2)
                .withNearbySelectionConfig(nearbySelectionConfig);

        SubListSelectorFactory<TestdataListSolution> selectorFactory = SubListSelectorFactory.create(config);

        HeuristicConfigPolicy<TestdataListSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        ListVariableDescriptor<TestdataListSolution> listVariableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();
        EntitySelector<TestdataListSolution> entitySelector = TestdataListUtils.mockEntitySelector();
        when(entitySelector.getEntityDescriptor()).thenReturn(listVariableDescriptor.getEntityDescriptor());

        assertThatIllegalArgumentException().isThrownBy(
                () -> selectorFactory.buildSubListSelector(heuristicConfigPolicy, entitySelector,
                        SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM))
                .withMessageContaining(violatingPropertyName);
    }

    static Stream<Arguments> limitedDistributionConfigs() {
        return Stream.of(
                arguments(new NearbySelectionConfig().withBlockDistributionSizeRatio(0.5), "blockDistributionSizeRatio"),
                arguments(new NearbySelectionConfig().withBlockDistributionSizeMaximum(10), "blockDistributionSizeMaximum"),
                arguments(new NearbySelectionConfig().withLinearDistributionSizeMaximum(10), "linearDistributionSizeMaximum"),
                arguments(new NearbySelectionConfig().withParabolicDistributionSizeMaximum(10),
                        "parabolicDistributionSizeMaximum"));
    }

    @Test
    void failFast_ifNearbyDoesNotHaveOriginSubListSelector() {
        SubListSelectorConfig subListSelectorConfig = new SubListSelectorConfig()
                .withNearbySelectionConfig(new NearbySelectionConfig()
                        .withOriginEntitySelectorConfig(new EntitySelectorConfig().withMimicSelectorRef("x"))
                        .withNearbyDistanceMeterClass(mock(NearbyDistanceMeter.class).getClass()));

        EntitySelector<TestdataListSolution> entitySelector = TestdataListUtils.mockEntitySelector();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> SubListSelectorFactory.<TestdataListSolution> create(subListSelectorConfig)
                        .buildSubListSelector(buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor()),
                                entitySelector, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM))
                .withMessageContaining("requires an originSubListSelector");
    }
}
