package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.function.ToLongFunction;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinMoveSelectorConfig;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class RuinMoveSelectorFactory<Solution_> extends AbstractMoveSelectorFactory<Solution_, RuinMoveSelectorConfig> {
    protected final RuinMoveSelectorConfig ruinMoveSelectorConfig;

    public RuinMoveSelectorFactory(RuinMoveSelectorConfig ruinMoveSelectorConfig) {
        super(ruinMoveSelectorConfig);
        this.ruinMoveSelectorConfig = ruinMoveSelectorConfig;
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        var entitySelectorConfig = ruinMoveSelectorConfig.determineEntitySelectorConfig();
        var constructionHeuristicConfig = ruinMoveSelectorConfig.determineConstructionHeuristicConfig();
        ToLongFunction<Long> minimumSelectedSupplier = ruinMoveSelectorConfig::determineMinimumRuinedCount;
        ToLongFunction<Long> maximumSelectedSupplier = ruinMoveSelectorConfig::determineMaximumRuinedCount;

        var entitySelector = EntitySelectorFactory.<Solution_> create(entitySelectorConfig)
                .buildEntitySelector(configPolicy, minimumCacheType,
                        SelectionOrder.fromRandomSelectionBoolean(true));
        var genuineVariableDescriptorList = entitySelector.getEntityDescriptor().getGenuineVariableDescriptorList();
        if (genuineVariableDescriptorList.size() != 1) {
            // TODO: Add field for selecting variable descriptor?
            throw new IllegalArgumentException("Expected exactly one genuine variable");
        }
        var variableDescriptor = genuineVariableDescriptorList.get(0);
        var constructionHeuristicPhaseFactory =
                new DefaultConstructionHeuristicPhaseFactory<Solution_>(constructionHeuristicConfig);
        var constructionHeuristicPhase = constructionHeuristicPhaseFactory.buildRuinPhase(configPolicy);
        return new RuinMoveSelector<>(entitySelector, variableDescriptor, constructionHeuristicPhase,
                minimumSelectedSupplier, maximumSelectedSupplier);
    }
}
