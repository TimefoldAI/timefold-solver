package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public final class RuinRecreateMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, RuinRecreateMoveSelectorConfig> {

    private final RuinRecreateMoveSelectorConfig ruinRecreateMoveSelectorConfig;

    public RuinRecreateMoveSelectorFactory(RuinRecreateMoveSelectorConfig ruinRecreateMoveSelectorConfig) {
        super(ruinRecreateMoveSelectorConfig);
        this.ruinRecreateMoveSelectorConfig = ruinRecreateMoveSelectorConfig;
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        CountSupplier minimumSelectedSupplier = ruinRecreateMoveSelectorConfig::determineMinimumRuinedCount;
        CountSupplier maximumSelectedSupplier = ruinRecreateMoveSelectorConfig::determineMaximumRuinedCount;

        var entitySelector = EntitySelectorFactory.<Solution_> create(new EntitySelectorConfig())
                .buildEntitySelector(configPolicy, minimumCacheType,
                        SelectionOrder.fromRandomSelectionBoolean(true));
        var genuineVariableDescriptorList = entitySelector.getEntityDescriptor().getGenuineVariableDescriptorList();
        if (genuineVariableDescriptorList.size() != 1) {
            throw new UnsupportedOperationException(
                    "Ruin and Recreate move selector currently only supports 1 planning variable.");
        }
        var variableDescriptor = genuineVariableDescriptorList.get(0);

        var constructionHeuristicPhaseBuilder = RuinRecreateConstructionHeuristicPhaseBuilder.create(configPolicy);
        return new RuinRecreateMoveSelector<>(entitySelector, variableDescriptor, constructionHeuristicPhaseBuilder,
                minimumSelectedSupplier, maximumSelectedSupplier);
    }
}
