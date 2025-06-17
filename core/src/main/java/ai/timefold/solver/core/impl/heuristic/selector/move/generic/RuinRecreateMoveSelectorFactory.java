package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
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

        var entitySelectorConfig = config.getEntitySelectorConfig();
        if (entitySelectorConfig == null) {
            entitySelectorConfig = new EntitySelectorConfig();
        }
        var ruinRecreateEntitySelector = EntitySelectorFactory.<Solution_> create(entitySelectorConfig)
                .buildEntitySelector(configPolicy, minimumCacheType,
                        SelectionOrder.fromRandomSelectionBoolean(true));
        var genuineVariableDescriptorList =
                ruinRecreateEntitySelector.getEntityDescriptor().getGenuineBasicVariableDescriptorList();
        if (genuineVariableDescriptorList.size() != 1 && config.getVariableName() == null) {
            throw new UnsupportedOperationException(
                    """
                            The entity class %s contains several variables (%s), and it cannot be deduced automatically.
                            Maybe set the property variableName."""
                            .formatted(
                                    ruinRecreateEntitySelector.getEntityDescriptor().getEntityClass().getName(),
                                    genuineVariableDescriptorList.stream().map(GenuineVariableDescriptor::getVariableName)
                                            .toList()));
        }
        var variableDescriptor = genuineVariableDescriptorList.get(0);
        if (genuineVariableDescriptorList.size() > 1) {
            variableDescriptor = genuineVariableDescriptorList.stream()
                    .filter(v -> v.getVariableName().equals(config.getVariableName())).findFirst().orElse(null);
        }
        if (variableDescriptor == null) {
            throw new UnsupportedOperationException("The entity class %s has no variable named %s."
                    .formatted(ruinRecreateEntitySelector.getEntityDescriptor().getEntityClass(), config.getVariableName()));
        }
        var nestedEntitySelectorConfig =
                getDefaultEntitySelectorConfigForEntity(configPolicy, ruinRecreateEntitySelector.getEntityDescriptor());
        var constructionHeuristicPhaseBuilder =
                RuinRecreateConstructionHeuristicPhaseBuilder.create(configPolicy, nestedEntitySelectorConfig);
        return new RuinRecreateMoveSelector<>(ruinRecreateEntitySelector, variableDescriptor, constructionHeuristicPhaseBuilder,
                minimumSelectedSupplier, maximumSelectedSupplier);
    }
}
