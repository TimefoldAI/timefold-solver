package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;

class ListChangeMoveSelectorFactoryTest {

    @Test
    void noUnfolding() {
        SolutionDescriptor<TestdataListSolution> solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        ListChangeMoveSelectorConfig moveSelectorConfig = new ListChangeMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig("valueList"))
                .withDestinationSelectorConfig(new DestinationSelectorConfig()
                        .withEntitySelectorConfig(new EntitySelectorConfig(TestdataListEntity.class))
                        .withValueSelectorConfig(new ValueSelectorConfig("valueList")));
        MoveSelector<TestdataListSolution> moveSelector =
                MoveSelectorFactory.<TestdataListSolution> create(moveSelectorConfig).buildMoveSelector(
                        buildHeuristicConfigPolicy(solutionDescriptor), SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false);
        assertThat(moveSelector).isInstanceOf(ListChangeMoveSelector.class);
    }

    @Test
    void unfoldedSingleListVariable() {
        SolutionDescriptor<TestdataListSolution> solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        ListChangeMoveSelectorConfig moveSelectorConfig = new ListChangeMoveSelectorConfig();
        MoveSelector<TestdataListSolution> moveSelector =
                MoveSelectorFactory.<TestdataListSolution> create(moveSelectorConfig).buildMoveSelector(
                        buildHeuristicConfigPolicy(solutionDescriptor), SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false);
        assertThat(moveSelector).isInstanceOf(ListChangeMoveSelector.class);
    }

    @Test
    void unfoldedConfigInheritsFromFoldedConfig() {
        SolutionDescriptor<TestdataListSolution> solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();

        SelectionCacheType moveSelectorCacheType = SelectionCacheType.STEP;
        SelectionOrder moveSelectorSelectionOrder = SelectionOrder.ORIGINAL;
        long selectedCountLimit = 200;

        ListChangeMoveSelectorConfig moveSelectorConfig = new ListChangeMoveSelectorConfig()
                .withCacheType(moveSelectorCacheType)
                .withSelectionOrder(moveSelectorSelectionOrder)
                .withSelectedCountLimit(selectedCountLimit);

        ListChangeMoveSelectorFactory<TestdataListSolution> moveSelectorFactory =
                ((ListChangeMoveSelectorFactory<TestdataListSolution>) MoveSelectorFactory
                        .<TestdataListSolution> create(moveSelectorConfig));

        MoveSelectorConfig<?> unfoldedMoveSelectorConfig =
                moveSelectorFactory.buildUnfoldedMoveSelectorConfig(buildHeuristicConfigPolicy(solutionDescriptor));

        assertThat(unfoldedMoveSelectorConfig).isInstanceOf(ListChangeMoveSelectorConfig.class);
        ListChangeMoveSelectorConfig listChangeMoveSelectorConfig = (ListChangeMoveSelectorConfig) unfoldedMoveSelectorConfig;

        assertThat(listChangeMoveSelectorConfig.getValueSelectorConfig().getVariableName()).isEqualTo("valueList");
        assertThat(listChangeMoveSelectorConfig.getCacheType()).isEqualTo(moveSelectorCacheType);
        assertThat(listChangeMoveSelectorConfig.getSelectionOrder()).isEqualTo(moveSelectorSelectionOrder);
        assertThat(listChangeMoveSelectorConfig.getSelectedCountLimit()).isEqualTo(selectedCountLimit);

        DestinationSelectorConfig destinationSelectorConfig = listChangeMoveSelectorConfig.getDestinationSelectorConfig();
        EntitySelectorConfig entitySelectorConfig = destinationSelectorConfig.getEntitySelectorConfig();
        assertThat(entitySelectorConfig.getEntityClass()).isEqualTo(TestdataListEntity.class);
        ValueSelectorConfig valueSelectorConfig = destinationSelectorConfig.getValueSelectorConfig();
        assertThat(valueSelectorConfig.getVariableName()).isEqualTo("valueList");
    }

    @Test
    void unfoldingFailsIfThereIsNoListVariable() {
        ListChangeMoveSelectorConfig config = new ListChangeMoveSelectorConfig();
        ListChangeMoveSelectorFactory<TestdataSolution> moveSelectorFactory = new ListChangeMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME,
                        SelectionOrder.RANDOM, false))
                .withMessageContaining("cannot unfold");
    }

    @Test
    void explicitConfigMustUseListVariable() {
        ListChangeMoveSelectorConfig config = new ListChangeMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig("value"))
                .withDestinationSelectorConfig(new DestinationSelectorConfig()
                        .withEntitySelectorConfig(new EntitySelectorConfig(TestdataEntity.class))
                        .withValueSelectorConfig(new ValueSelectorConfig("value")));

        ListChangeMoveSelectorFactory<TestdataSolution> moveSelectorFactory =
                new ListChangeMoveSelectorFactory<>(config);

        HeuristicConfigPolicy<TestdataSolution> heuristicConfigPolicy =
                buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> moveSelectorFactory.buildMoveSelector(heuristicConfigPolicy, SelectionCacheType.JUST_IN_TIME,
                        SelectionOrder.RANDOM, false))
                .withMessageContaining("not a planning list variable");
    }
}
