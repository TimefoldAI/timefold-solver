package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Comparator;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.UnionMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.multientity.TestdataHerdEntity;
import ai.timefold.solver.core.impl.testdata.domain.multientity.TestdataMultiEntitySolution;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultiVarSolution;

import org.junit.jupiter.api.Test;

class ChangeMoveSelectorFactoryTest {

    @Test
    void deducibleMultiVar() {
        SolutionDescriptor solutionDescriptor = TestdataMultiVarSolution.buildSolutionDescriptor();
        ChangeMoveSelectorConfig moveSelectorConfig = new ChangeMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig("secondaryValue"));
        MoveSelector moveSelector =
                MoveSelectorFactory.create(moveSelectorConfig).buildMoveSelector(buildHeuristicConfigPolicy(solutionDescriptor),
                        SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector)
                .isInstanceOf(ChangeMoveSelector.class);
    }

    @Test
    void undeducibleMultiVar() {
        SolutionDescriptor solutionDescriptor = TestdataMultiVarSolution.buildSolutionDescriptor();
        ChangeMoveSelectorConfig moveSelectorConfig = new ChangeMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig("nonExistingValue"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MoveSelectorFactory.create(moveSelectorConfig).buildMoveSelector(
                        buildHeuristicConfigPolicy(solutionDescriptor), SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false));
    }

    @Test
    void unfoldedMultiVar() {
        SolutionDescriptor solutionDescriptor = TestdataMultiVarSolution.buildSolutionDescriptor();
        ChangeMoveSelectorConfig moveSelectorConfig = new ChangeMoveSelectorConfig();
        MoveSelector moveSelector =
                MoveSelectorFactory.create(moveSelectorConfig).buildMoveSelector(
                        buildHeuristicConfigPolicy(solutionDescriptor), SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false);
        assertThat(moveSelector)
                .isInstanceOf(UnionMoveSelector.class);
        assertThat(((UnionMoveSelector) moveSelector).getChildMoveSelectorList()).hasSize(3);
    }

    @Test
    void deducibleMultiEntity() {
        SolutionDescriptor solutionDescriptor = TestdataMultiEntitySolution.buildSolutionDescriptor();
        ChangeMoveSelectorConfig moveSelectorConfig = new ChangeMoveSelectorConfig()
                .withEntitySelectorConfig(new EntitySelectorConfig(TestdataHerdEntity.class));
        MoveSelector moveSelector =
                MoveSelectorFactory.create(moveSelectorConfig).buildMoveSelector(buildHeuristicConfigPolicy(solutionDescriptor),
                        SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector)
                .isInstanceOf(ChangeMoveSelector.class);
    }

    @Test
    void undeducibleMultiEntity() {
        SolutionDescriptor solutionDescriptor = TestdataMultiEntitySolution.buildSolutionDescriptor();
        ChangeMoveSelectorConfig moveSelectorConfig = new ChangeMoveSelectorConfig()
                .withEntitySelectorConfig(new EntitySelectorConfig(TestdataEntity.class));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MoveSelectorFactory.create(moveSelectorConfig).buildMoveSelector(
                        buildHeuristicConfigPolicy(solutionDescriptor), SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                        false));
    }

    @Test
    void unfoldedMultiEntity() {
        SolutionDescriptor solutionDescriptor = TestdataMultiEntitySolution.buildSolutionDescriptor();
        ChangeMoveSelectorConfig moveSelectorConfig = new ChangeMoveSelectorConfig();
        MoveSelector moveSelector =
                MoveSelectorFactory.create(moveSelectorConfig).buildMoveSelector(buildHeuristicConfigPolicy(solutionDescriptor),
                        SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM, false);
        assertThat(moveSelector)
                .isInstanceOf(UnionMoveSelector.class);
        assertThat(((UnionMoveSelector) moveSelector).getChildMoveSelectorList()).hasSize(2);
    }

    // ************************************************************************
    // List variable compatibility section
    // ************************************************************************

    @Test
    void unfoldEmptyIntoListChangeMoveSelectorConfig() {
        SolutionDescriptor<TestdataListSolution> solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        ChangeMoveSelectorConfig moveSelectorConfig = new ChangeMoveSelectorConfig();
        MoveSelector<TestdataListSolution> moveSelector =
                MoveSelectorFactory.<TestdataListSolution> create(moveSelectorConfig)
                        .buildMoveSelector(buildHeuristicConfigPolicy(solutionDescriptor), SelectionCacheType.JUST_IN_TIME,
                                SelectionOrder.RANDOM, false);
        assertThat(moveSelector).isInstanceOf(ListChangeMoveSelector.class);
    }

    @Test
    void unfoldConfiguredIntoListChangeMoveSelectorConfig() {
        SolutionDescriptor<TestdataListSolution> solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();

        SelectionCacheType moveSelectorCacheType = SelectionCacheType.PHASE;
        SelectionOrder moveSelectorSelectionOrder = SelectionOrder.ORIGINAL;
        long selectedCountLimit = 200;
        ChangeMoveSelectorConfig moveSelectorConfig = new ChangeMoveSelectorConfig()
                .withEntitySelectorConfig(new EntitySelectorConfig(TestdataListEntity.class)
                        .withSorterComparatorClass(DummyEntityComparator.class))
                .withValueSelectorConfig(new ValueSelectorConfig("valueList"))
                .withCacheType(moveSelectorCacheType)
                .withSelectionOrder(moveSelectorSelectionOrder)
                .withSelectedCountLimit(selectedCountLimit);

        ChangeMoveSelectorFactory<TestdataListSolution> changeMoveSelectorFactory =
                (ChangeMoveSelectorFactory<TestdataListSolution>) MoveSelectorFactory
                        .<TestdataListSolution> create(moveSelectorConfig);

        MoveSelectorConfig<?> unfoldedMoveSelectorConfig =
                changeMoveSelectorFactory.buildUnfoldedMoveSelectorConfig(buildHeuristicConfigPolicy(solutionDescriptor));

        assertThat(unfoldedMoveSelectorConfig).isExactlyInstanceOf(ListChangeMoveSelectorConfig.class);
        ListChangeMoveSelectorConfig listChangeMoveSelectorConfig = (ListChangeMoveSelectorConfig) unfoldedMoveSelectorConfig;

        assertThat(listChangeMoveSelectorConfig.getValueSelectorConfig().getVariableName()).isEqualTo("valueList");
        assertThat(listChangeMoveSelectorConfig.getCacheType()).isEqualTo(moveSelectorCacheType);
        assertThat(listChangeMoveSelectorConfig.getSelectionOrder()).isEqualTo(moveSelectorSelectionOrder);
        assertThat(listChangeMoveSelectorConfig.getSelectedCountLimit()).isEqualTo(selectedCountLimit);

        DestinationSelectorConfig destinationSelectorConfig = listChangeMoveSelectorConfig.getDestinationSelectorConfig();
        EntitySelectorConfig entitySelectorConfig = destinationSelectorConfig.getEntitySelectorConfig();
        assertThat(entitySelectorConfig.getEntityClass()).isEqualTo(TestdataListEntity.class);
        assertThat(entitySelectorConfig.getSorterComparatorClass()).isEqualTo(DummyEntityComparator.class);
        ValueSelectorConfig valueSelectorConfig = destinationSelectorConfig.getValueSelectorConfig();
        assertThat(valueSelectorConfig.getVariableName()).isEqualTo("valueList");
    }

    static class DummyEntityComparator implements Comparator<TestdataListEntity> {
        @Override
        public int compare(TestdataListEntity e1, TestdataListEntity e2) {
            return 0;
        }
    }
}
