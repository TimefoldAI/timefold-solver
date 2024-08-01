package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinMoveSelectorConfig;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

import java.util.function.ToLongFunction;

public class ListRuinMoveSelectorFactory<Solution_> extends AbstractMoveSelectorFactory<Solution_, ListRuinMoveSelectorConfig> {
    protected final ListRuinMoveSelectorConfig ruinMoveSelectorConfig;

    public ListRuinMoveSelectorFactory(ListRuinMoveSelectorConfig ruinMoveSelectorConfig) {
        super(ruinMoveSelectorConfig);
        this.ruinMoveSelectorConfig = ruinMoveSelectorConfig;
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        var valueSelectorConfig = ruinMoveSelectorConfig.determineValueSelectorConfig();
        var constructionHeuristicConfig = ruinMoveSelectorConfig.determineConstructionHeuristicConfig();
        ToLongFunction<Long> minimumSelectedSupplier = ruinMoveSelectorConfig::determineMinimumRuinedCount;
        ToLongFunction<Long> maximumSelectedSupplier = ruinMoveSelectorConfig::determineMaximumRuinedCount;

        this.getTheOnlyEntityDescriptor(configPolicy.getSolutionDescriptor());

        var listVariableDescriptor = configPolicy.getSolutionDescriptor().getListVariableDescriptor();
        var entityDescriptor = listVariableDescriptor.getEntityDescriptor();
        var valueSelector =
                (EntityIndependentValueSelector<Solution_>) ValueSelectorFactory.<Solution_> create(valueSelectorConfig)
                        .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, SelectionOrder.RANDOM,
                                false, ValueSelectorFactory.ListValueFilteringType.ACCEPT_ASSIGNED);
        var entityPlacerConfig =
                DefaultConstructionHeuristicPhaseFactory.buildListVariableQueuedValuePlacerConfig(configPolicy,
                        listVariableDescriptor);
        constructionHeuristicConfig.setEntityPlacerConfig(entityPlacerConfig);
        var constructionHeuristicPhaseFactory =
                new DefaultConstructionHeuristicPhaseFactory<Solution_>(constructionHeuristicConfig);
        var constructionHeuristicPhase = constructionHeuristicPhaseFactory.buildRuinPhase(configPolicy);
        return new ListRuinMoveSelector<>(valueSelector, listVariableDescriptor, constructionHeuristicPhase,
                minimumSelectedSupplier, maximumSelectedSupplier);
    }
}
