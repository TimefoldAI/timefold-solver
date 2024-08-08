package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.function.ToLongFunction;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinMoveSelectorConfig;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public final class RuinMoveSelectorFactory<Solution_> extends AbstractMoveSelectorFactory<Solution_, RuinMoveSelectorConfig> {

    private final RuinMoveSelectorConfig ruinMoveSelectorConfig;

    public RuinMoveSelectorFactory(RuinMoveSelectorConfig ruinMoveSelectorConfig) {
        super(ruinMoveSelectorConfig);
        this.ruinMoveSelectorConfig = ruinMoveSelectorConfig;
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        ToLongFunction<Long> minimumSelectedSupplier = ruinMoveSelectorConfig::determineMinimumRuinedCount;
        ToLongFunction<Long> maximumSelectedSupplier = ruinMoveSelectorConfig::determineMaximumRuinedCount;

        var entitySelector = EntitySelectorFactory.<Solution_> create(new EntitySelectorConfig())
                .buildEntitySelector(configPolicy, minimumCacheType,
                        SelectionOrder.fromRandomSelectionBoolean(true));
        var genuineVariableDescriptorList = entitySelector.getEntityDescriptor().getGenuineVariableDescriptorList();
        if (genuineVariableDescriptorList.size() != 1) {
            throw new UnsupportedOperationException(
                    "Ruin and Recreate move selector currently only supports 1 planning variable.");
        }
        var variableDescriptor = genuineVariableDescriptorList.get(0);

        var constructionHeuristicConfig = new ConstructionHeuristicPhaseConfig();
        var constructionHeuristicPhaseFactory =
                new DefaultConstructionHeuristicPhaseFactory<Solution_>(constructionHeuristicConfig);
        var constructionHeuristicPhase = constructionHeuristicPhaseFactory.getRuinPhaseBuilder(configPolicy);
        return new RuinMoveSelector<>(entitySelector, variableDescriptor, constructionHeuristicPhase,
                minimumSelectedSupplier, maximumSelectedSupplier);
    }
}
