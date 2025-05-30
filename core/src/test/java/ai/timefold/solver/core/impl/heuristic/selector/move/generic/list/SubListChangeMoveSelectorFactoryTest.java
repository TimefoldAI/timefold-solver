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
import ai.timefold.solver.core.impl.heuristic.selector.list.RandomSubListSelector;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntitySolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SubListChangeMoveSelectorFactoryTest {

    @Test
    void buildMoveSelector() {
        var config = new SubListChangeMoveSelectorConfig();
        var moveSelectorFactory = new SubListChangeMoveSelectorFactory<TestdataListSolution>(config);

        var heuristicConfigPolicy = buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        var selector = (RandomSubListChangeMoveSelector<TestdataListSolution>) moveSelectorFactory
                .buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false);

        assertThat(selector.isCountable()).isTrue();
        assertThat(selector.isNeverEnding()).isTrue();
        assertThat(selector.isSelectReversingMoveToo()).isTrue();
    }

    @Test
    void buildMoveSelectorMultiEntity() {
        var config = new SubListChangeMoveSelectorConfig();
        var moveSelectorFactory = new SubListChangeMoveSelectorFactory<TestdataMixedMultiEntitySolution>(config);

        var heuristicConfigPolicy = buildHeuristicConfigPolicy(TestdataMixedMultiEntitySolution.buildSolutionDescriptor());

        var selector = (RandomSubListChangeMoveSelector<TestdataMixedMultiEntitySolution>) moveSelectorFactory
                .buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false);

        assertThat(selector.isCountable()).isTrue();
        assertThat(selector.isNeverEnding()).isTrue();
        assertThat(selector.isSelectReversingMoveToo()).isTrue();
    }

    @Test
    void disableSelectReversingMoveToo() {
        var config = new SubListChangeMoveSelectorConfig();
        config.setSelectReversingMoveToo(false);
        var moveSelectorFactory = new SubListChangeMoveSelectorFactory<TestdataListSolution>(config);

        var heuristicConfigPolicy = buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        var selector = (RandomSubListChangeMoveSelector<TestdataListSolution>) moveSelectorFactory
                .buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false);

        assertThat(selector.isSelectReversingMoveToo()).isFalse();
    }

    @Test
    void unfoldingFailsIfThereIsNoListVariable() {
        var config = new SubListChangeMoveSelectorConfig();
        var moveSelectorFactory = new SubListChangeMoveSelectorFactory<TestdataSolution>(config);

        var heuristicConfigPolicy = buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME,
                        SelectionOrder.RANDOM, false))
                .withMessageContaining("it cannot be deduced automatically");
    }

    @Test
    void explicitConfigMustUseListVariable() {
        var config = new SubListChangeMoveSelectorConfig()
                .withSubListSelectorConfig(new SubListSelectorConfig()
                        .withValueSelectorConfig(new ValueSelectorConfig("value")))
                .withDestinationSelectorConfig(new DestinationSelectorConfig()
                        .withEntitySelectorConfig(new EntitySelectorConfig(TestdataEntity.class))
                        .withValueSelectorConfig(new ValueSelectorConfig("value")));

        var moveSelectorFactory = new SubListChangeMoveSelectorFactory<TestdataSolution>(config);

        var heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME,
                        SelectionOrder.RANDOM, false))
                .withMessageContaining("not a planning list variable");
    }

    static SubListChangeMoveSelectorConfig minimumSize_SubListSelector() {
        var config = new SubListChangeMoveSelectorConfig()
                .withSubListSelectorConfig(new SubListSelectorConfig().withMinimumSubListSize(10));
        config.setMinimumSubListSize(10);
        return config;
    }

    static SubListChangeMoveSelectorConfig maximumSize_SubListSelector() {
        var config = new SubListChangeMoveSelectorConfig()
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
        var factory = new SubListChangeMoveSelectorFactory<TestdataListSolution>(config);

        var heuristicConfigPolicy = buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> factory.buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME,
                        SelectionOrder.RANDOM, false))
                .withMessageContainingAll(propertyName, childConfigName);
    }

    @Test
    void transferDeprecatedSubListSizeToChildSelector() {
        var minimumSubListSize = 21;
        var maximumSubListSize = 445;
        var config = new SubListChangeMoveSelectorConfig();
        config.setMinimumSubListSize(minimumSubListSize);
        config.setMaximumSubListSize(maximumSubListSize);

        var factory = new SubListChangeMoveSelectorFactory<TestdataListSolution>(config);

        var heuristicConfigPolicy = buildHeuristicConfigPolicy(TestdataListSolution.buildSolutionDescriptor());

        var moveSelector = (RandomSubListChangeMoveSelector<TestdataListSolution>) factory
                .buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false);

        assertThat(((RandomSubListSelector<?>) moveSelector.getSubListSelector()).getMinimumSubListSize())
                .isEqualTo(minimumSubListSize);
        assertThat(((RandomSubListSelector<?>) moveSelector.getSubListSelector()).getMaximumSubListSize())
                .isEqualTo(maximumSubListSize);
    }
}
