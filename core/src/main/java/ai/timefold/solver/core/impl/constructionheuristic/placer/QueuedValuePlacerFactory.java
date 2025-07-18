package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

public class QueuedValuePlacerFactory<Solution_>
        extends AbstractEntityPlacerFactory<Solution_, QueuedValuePlacerConfig> {

    public static QueuedValuePlacerConfig unfoldNew(MoveSelectorConfig templateMoveSelectorConfig) {
        throw new UnsupportedOperationException(
                "The <constructionHeuristic> contains a moveSelector (%s) and the <queuedValuePlacer> does not support unfolding those yet."
                        .formatted(templateMoveSelectorConfig));
    }

    public QueuedValuePlacerFactory(QueuedValuePlacerConfig placerConfig) {
        super(placerConfig);
    }

    @Override
    public QueuedValuePlacer<Solution_> buildEntityPlacer(HeuristicConfigPolicy<Solution_> configPolicy) {
        EntityDescriptor<Solution_> entityDescriptor = deduceEntityDescriptor(configPolicy, config.getEntityClass());
        ValueSelectorConfig valueSelectorConfig_ = buildValueSelectorConfig(configPolicy, entityDescriptor);
        // TODO improve the ValueSelectorFactory API (avoid the boolean flags).
        ValueSelector<Solution_> valueSelector = ValueSelectorFactory.<Solution_> create(valueSelectorConfig_)
                .buildValueSelector(configPolicy, entityDescriptor, SelectionCacheType.PHASE, SelectionOrder.ORIGINAL,
                        false, // override applyReinitializeVariableFiltering
                        ValueSelectorFactory.ListValueFilteringType.ACCEPT_UNASSIGNED);

        MoveSelectorConfig<?> moveSelectorConfig_ = config.getMoveSelectorConfig() == null
                ? buildChangeMoveSelectorConfig(configPolicy, valueSelectorConfig_.getId(),
                        valueSelector.getVariableDescriptor())
                : config.getMoveSelectorConfig();

        MoveSelector<Solution_> moveSelector = MoveSelectorFactory.<Solution_> create(moveSelectorConfig_)
                .buildMoveSelector(configPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL, false);
        if (!(valueSelector instanceof IterableValueSelector<Solution_> iterableValueSelector)) {
            throw new IllegalArgumentException(
                    "The queuedValuePlacer (%s) needs to be based on an %s (%s). Check your @%s annotations.".formatted(this,
                            IterableValueSelector.class.getSimpleName(), valueSelector,
                            ValueRangeProvider.class.getSimpleName()));

        }
        return new QueuedValuePlacer<>(this, configPolicy, iterableValueSelector, moveSelector);
    }

    private ValueSelectorConfig buildValueSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor) {
        var result = Objects.requireNonNullElseGet(config.getValueSelectorConfig(),
                () -> {
                    var entityClass = entityDescriptor.getEntityClass();
                    var variableDescriptor = getTheOnlyVariableDescriptor(entityDescriptor);
                    var valueSelectorConfig = new ValueSelectorConfig()
                            .withId(entityClass.getName() + "." + variableDescriptor.getVariableName())
                            .withVariableName(variableDescriptor.getVariableName());
                    if (ValueSelectorConfig.hasSorter(configPolicy.getValueSorterManner(), variableDescriptor)) {
                        valueSelectorConfig = valueSelectorConfig.withCacheType(SelectionCacheType.PHASE)
                                .withSelectionOrder(SelectionOrder.SORTED)
                                .withSorterManner(configPolicy.getValueSorterManner());
                    }
                    return valueSelectorConfig;
                });
        var cacheType = result.getCacheType();
        if (cacheType != null && cacheType.compareTo(SelectionCacheType.PHASE) < 0) {
            throw new IllegalArgumentException(
                    "The queuedValuePlacer (%s) cannot have a valueSelectorConfig (%s) with a cacheType (%s) lower than %s."
                            .formatted(this, result, cacheType, SelectionCacheType.PHASE));
        }
        return result;
    }

    @Override
    protected ChangeMoveSelectorConfig buildChangeMoveSelectorConfig(
            HeuristicConfigPolicy<Solution_> configPolicy, String valueSelectorConfigId,
            GenuineVariableDescriptor<Solution_> variableDescriptor) {
        ChangeMoveSelectorConfig changeMoveSelectorConfig = new ChangeMoveSelectorConfig();
        EntityDescriptor<Solution_> entityDescriptor = variableDescriptor.getEntityDescriptor();
        EntitySelectorConfig changeEntitySelectorConfig = new EntitySelectorConfig()
                .withEntityClass(entityDescriptor.getEntityClass());
        if (configPolicy.getEntitySorterManner() != null
                && EntitySelectorConfig.hasSorter(configPolicy.getEntitySorterManner(), entityDescriptor)) {
            changeEntitySelectorConfig = changeEntitySelectorConfig.withCacheType(SelectionCacheType.PHASE)
                    .withSelectionOrder(SelectionOrder.SORTED)
                    .withSorterManner(configPolicy.getEntitySorterManner());
        }
        ValueSelectorConfig changeValueSelectorConfig = new ValueSelectorConfig()
                .withVariableName(variableDescriptor.getVariableName())
                .withMimicSelectorRef(valueSelectorConfigId);
        return changeMoveSelectorConfig.withEntitySelectorConfig(changeEntitySelectorConfig)
                .withValueSelectorConfig(changeValueSelectorConfig);
    }
}
