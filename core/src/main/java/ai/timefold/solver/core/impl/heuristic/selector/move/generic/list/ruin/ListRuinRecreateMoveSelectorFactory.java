package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.CountSupplier;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

public final class ListRuinRecreateMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, ListRuinRecreateMoveSelectorConfig> {

    private final ListRuinRecreateMoveSelectorConfig ruinMoveSelectorConfig;

    public ListRuinRecreateMoveSelectorFactory(ListRuinRecreateMoveSelectorConfig ruinMoveSelectorConfig) {
        super(ruinMoveSelectorConfig);
        this.ruinMoveSelectorConfig = ruinMoveSelectorConfig;
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        CountSupplier minimumSelectedSupplier = ruinMoveSelectorConfig::determineMinimumRuinedCount;
        CountSupplier maximumSelectedSupplier = ruinMoveSelectorConfig::determineMaximumRuinedCount;

        this.getTheOnlyEntityDescriptor(configPolicy.getSolutionDescriptor());

        var listVariableDescriptor = configPolicy.getSolutionDescriptor().getListVariableDescriptor();
        var entityDescriptor = listVariableDescriptor.getEntityDescriptor();
        var valueSelector =
                (EntityIndependentValueSelector<Solution_>) ValueSelectorFactory.<Solution_> create(new ValueSelectorConfig())
                        .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, SelectionOrder.RANDOM,
                                false, ValueSelectorFactory.ListValueFilteringType.ACCEPT_ASSIGNED);
        var entityPlacerConfig = DefaultConstructionHeuristicPhaseFactory.buildListVariableQueuedValuePlacerConfig(configPolicy,
                listVariableDescriptor);

        var constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig()
                .withEntityPlacerConfig(entityPlacerConfig);
        var constructionHeuristicPhaseBuilder =
                RuinRecreateConstructionHeuristicPhaseBuilder.create(configPolicy, constructionHeuristicPhaseConfig);
        return new ListRuinRecreateMoveSelector<>(valueSelector, listVariableDescriptor, constructionHeuristicPhaseBuilder,
                minimumSelectedSupplier, maximumSelectedSupplier);
    }
}
