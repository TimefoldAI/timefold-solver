package ai.timefold.solver.core.impl.constructionheuristic.placer;

import static ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType.PHASE;
import static ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType.STEP;

import ai.timefold.solver.core.config.constructionheuristic.placer.EntityPlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.AbstractFromConfigFactory;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;

abstract class AbstractEntityPlacerFactory<Solution_, EntityPlacerConfig_ extends EntityPlacerConfig<EntityPlacerConfig_>>
        extends AbstractFromConfigFactory<Solution_, EntityPlacerConfig_> implements EntityPlacerFactory<Solution_> {

    protected AbstractEntityPlacerFactory(EntityPlacerConfig_ placerConfig) {
        super(placerConfig);
    }

    protected ChangeMoveSelectorConfig buildChangeMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy,
            String entitySelectorConfigId, GenuineVariableDescriptor<Solution_> variableDescriptor) {
        ChangeMoveSelectorConfig changeMoveSelectorConfig = new ChangeMoveSelectorConfig();
        changeMoveSelectorConfig.setEntitySelectorConfig(
                EntitySelectorConfig.newMimicSelectorConfig(entitySelectorConfigId));
        ValueSelectorConfig changeValueSelectorConfig = new ValueSelectorConfig()
                .withVariableName(variableDescriptor.getVariableName());
        if (ValueSelectorConfig.hasSorter(configPolicy.getValueSorterManner(), variableDescriptor)) {
            changeValueSelectorConfig = changeValueSelectorConfig
                    .withCacheType(variableDescriptor.isValueRangeEntityIndependent() ? PHASE : STEP)
                    .withSelectionOrder(SelectionOrder.SORTED)
                    .withSorterManner(configPolicy.getValueSorterManner());
        }
        return changeMoveSelectorConfig.withValueSelectorConfig(changeValueSelectorConfig);
    }

    protected void validateCacheType(EntitySelectorConfig entitySelectorConfig) {
        /*
         * Step cache is built after the step is started and all events are triggered.
         * In construction heuristics though, each step corresponds to one entity in the placer iterator.
         * Therefore, the entity placer iterator is called before any step is started.
         * This results in an NPE, because the step cache has yet to be built.
         *
         * The only way to solve this is to call the placer iterator after a step starts.
         * But if we then find out the iterator is empty,
         * we've started a step that never should have been started in the first place.
         *
         * This is a chicken-and-egg problem which we're "solving" by throwing an exception.
         */
        if (entitySelectorConfig.getCacheType() != null
                && entitySelectorConfig.getCacheType().compareTo(SelectionCacheType.PHASE) < 0) {
            throw new IllegalArgumentException(
                    "The entityPlacer (%s) cannot have an entitySelectorConfig (%s) with a cacheType (%s) lower than %s."
                            .formatted(config, entitySelectorConfig, entitySelectorConfig.getCacheType(), PHASE));
        }
    }
}
