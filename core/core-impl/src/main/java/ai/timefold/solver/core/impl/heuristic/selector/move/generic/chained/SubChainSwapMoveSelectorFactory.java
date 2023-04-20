package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.chained.SubChainSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChainSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChainSelectorFactory;

public class SubChainSwapMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, SubChainSwapMoveSelectorConfig> {

    public SubChainSwapMoveSelectorFactory(SubChainSwapMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        EntityDescriptor<Solution_> entityDescriptor = deduceEntityDescriptor(configPolicy, config.getEntityClass());
        SubChainSelectorConfig subChainSelectorConfig =
                Objects.requireNonNullElseGet(config.getSubChainSelectorConfig(), SubChainSelectorConfig::new);
        SubChainSelectorConfig secondarySubChainSelectorConfig =
                Objects.requireNonNullElse(config.getSecondarySubChainSelectorConfig(), subChainSelectorConfig);
        SelectionOrder selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        SubChainSelector<Solution_> leftSubChainSelector =
                SubChainSelectorFactory.<Solution_> create(subChainSelectorConfig)
                        .buildSubChainSelector(configPolicy, entityDescriptor, minimumCacheType, selectionOrder);
        SubChainSelector<Solution_> rightSubChainSelector =
                SubChainSelectorFactory.<Solution_> create(secondarySubChainSelectorConfig)
                        .buildSubChainSelector(configPolicy, entityDescriptor, minimumCacheType, selectionOrder);
        return new SubChainSwapMoveSelector<>(leftSubChainSelector, rightSubChainSelector, randomSelection,
                Objects.requireNonNullElse(config.getSelectReversingMoveToo(), true));
    }
}
