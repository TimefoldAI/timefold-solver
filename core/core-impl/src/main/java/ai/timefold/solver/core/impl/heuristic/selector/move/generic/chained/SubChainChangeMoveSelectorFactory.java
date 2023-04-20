package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.chained.SubChainSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChainSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChainSelectorFactory;

public class SubChainChangeMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, SubChainChangeMoveSelectorConfig> {

    public SubChainChangeMoveSelectorFactory(SubChainChangeMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        EntityDescriptor<Solution_> entityDescriptor = deduceEntityDescriptor(configPolicy, config.getEntityClass());
        SubChainSelectorConfig subChainSelectorConfig =
                Objects.requireNonNullElseGet(config.getSubChainSelectorConfig(), SubChainSelectorConfig::new);
        ValueSelectorConfig valueSelectorConfig =
                Objects.requireNonNullElseGet(config.getValueSelectorConfig(), ValueSelectorConfig::new);
        SelectionOrder selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        SubChainSelector<Solution_> subChainSelector =
                SubChainSelectorFactory.<Solution_> create(subChainSelectorConfig)
                        .buildSubChainSelector(configPolicy, entityDescriptor, minimumCacheType, selectionOrder);
        ValueSelector<Solution_> valueSelector = ValueSelectorFactory.<Solution_> create(valueSelectorConfig)
                .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, selectionOrder);
        if (!(valueSelector instanceof EntityIndependentValueSelector)) {
            throw new IllegalArgumentException("The moveSelectorConfig (" + config
                    + ") needs to be based on an "
                    + EntityIndependentValueSelector.class.getSimpleName() + " (" + valueSelector + ")."
                    + " Check your @" + ValueRangeProvider.class.getSimpleName() + " annotations.");
        }
        return new SubChainChangeMoveSelector<>(subChainSelector,
                (EntityIndependentValueSelector<Solution_>) valueSelector, randomSelection,
                Objects.requireNonNullElse(config.getSelectReversingMoveToo(), true));
    }
}
