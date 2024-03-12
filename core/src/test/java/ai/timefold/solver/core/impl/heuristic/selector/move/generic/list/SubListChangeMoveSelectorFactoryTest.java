package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.list.RandomSubListSelector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SubListChangeMoveSelectorFactoryTest {

    @Test
    void buildMoveSelector() {
        SubListChangeMoveSelectorConfig config = new SubListChangeMoveSelectorConfig();
        SubListChangeMoveSelectorFactory<TestdataListSolution> moveSelectorFactory =
                new SubListChangeMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataListSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        RandomSubListChangeMoveSelector<TestdataListSolution> selector =
                (RandomSubListChangeMoveSelector<TestdataListSolution>) moveSelectorFactory
                        .buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                                false);

        assertThat(selector.isCountable()).isTrue();
        assertThat(selector.isNeverEnding()).isTrue();
        assertThat(selector.isSelectReversingMoveToo()).isTrue();
    }

    @Test
    void disableSelectReversingMoveToo() {
        SubListChangeMoveSelectorConfig config = new SubListChangeMoveSelectorConfig();
        config.setSelectReversingMoveToo(false);
        SubListChangeMoveSelectorFactory<TestdataListSolution> moveSelectorFactory =
                new SubListChangeMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataListSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        RandomSubListChangeMoveSelector<TestdataListSolution> selector =
                (RandomSubListChangeMoveSelector<TestdataListSolution>) moveSelectorFactory
                        .buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                                false);

        assertThat(selector.isSelectReversingMoveToo()).isFalse();
    }

    @Test
    void unfoldingFailsIfThereIsNoListVariable() {
        SubListChangeMoveSelectorConfig config = new SubListChangeMoveSelectorConfig();
        SubListChangeMoveSelectorFactory<TestdataSolution> moveSelectorFactory = new SubListChangeMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME,
                        SelectionOrder.RANDOM, false))
                .withMessageContaining("cannot unfold");
    }

    @Test
    void explicitConfigMustUseListVariable() {
        SubListChangeMoveSelectorConfig config = new SubListChangeMoveSelectorConfig()
                .withSubListSelectorConfig(new SubListSelectorConfig()
                        .withValueSelectorConfig(new ValueSelectorConfig("value")))
                .withDestinationSelectorConfig(new DestinationSelectorConfig()
                        .withEntitySelectorConfig(new EntitySelectorConfig(TestdataEntity.class))
                        .withValueSelectorConfig(new ValueSelectorConfig("value")));

        SubListChangeMoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new SubListChangeMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME,
                        SelectionOrder.RANDOM, false))
                .withMessageContaining("not a planning list variable");
    }

    static SubListChangeMoveSelectorConfig minimumSize_SubListSelector() {
        SubListChangeMoveSelectorConfig config = new SubListChangeMoveSelectorConfig()
                .withSubListSelectorConfig(new SubListSelectorConfig().withMinimumSubListSize(10));
        config.setMinimumSubListSize(10);
        return config;
    }

    static SubListChangeMoveSelectorConfig maximumSize_SubListSelector() {
        SubListChangeMoveSelectorConfig config = new SubListChangeMoveSelectorConfig()
                .withSubListSelectorConfig(new SubListSelectorConfig().withMaximumSubListSize(10));
        config.setMaximumSubListSize(10);
        return config;
    }

    static Stream<Arguments> wrongConfigurations() {
        return Stream.of(
                arguments(minimumSize_SubListSelector(), "minimumSubListSize", "subListSelector"),
                arguments(maximumSize_SubListSelector(), "maximumSubListSize", "subListSelector"));
    }

    @ParameterizedTest(name = "{1} + {2}")
    @MethodSource("wrongConfigurations")
    void failFast_ifSubListSizeOnBothMoveSelectorAndSubListSelector(
            SubListChangeMoveSelectorConfig config, String propertyName, String childConfigName) {
        SubListChangeMoveSelectorFactory<TestdataListSolution> factory = new SubListChangeMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataListSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> factory.buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME,
                        SelectionOrder.RANDOM, false))
                .withMessageContainingAll(propertyName, childConfigName);
    }

    @Test
    void transferDeprecatedSubListSizeToChildSelector() {
        int minimumSubListSize = 21;
        int maximumSubListSize = 445;
        SubListChangeMoveSelectorConfig config = new SubListChangeMoveSelectorConfig();
        config.setMinimumSubListSize(minimumSubListSize);
        config.setMaximumSubListSize(maximumSubListSize);

        SubListChangeMoveSelectorFactory<TestdataListSolution> factory = new SubListChangeMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataListSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        RandomSubListChangeMoveSelector<TestdataListSolution> moveSelector =
                (RandomSubListChangeMoveSelector<TestdataListSolution>) factory
                        .buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                                false);

        assertThat(((RandomSubListSelector<?>) moveSelector.getSubListSelector()).getMinimumSubListSize())
                .isEqualTo(minimumSubListSize);
        assertThat(((RandomSubListSelector<?>) moveSelector.getSubListSelector()).getMaximumSubListSize())
                .isEqualTo(maximumSubListSize);
    }
}
