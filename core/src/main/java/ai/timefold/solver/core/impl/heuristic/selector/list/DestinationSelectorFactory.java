package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.NONE;

import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.ValueRangeRecorderId;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

public final class DestinationSelectorFactory<Solution_> extends AbstractSelectorFactory<Solution_, DestinationSelectorConfig> {

    public static <Solution_> DestinationSelectorFactory<Solution_>
            create(DestinationSelectorConfig destinationSelectorConfig) {
        return new DestinationSelectorFactory<>(destinationSelectorConfig);
    }

    private DestinationSelectorFactory(DestinationSelectorConfig destinationSelectorConfig) {
        super(destinationSelectorConfig);
    }

    public DestinationSelector<Solution_> buildDestinationSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        return buildDestinationSelector(configPolicy, minimumCacheType, randomSelection, null, false);
    }

    public DestinationSelector<Solution_> buildDestinationSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection, String entityValueRangeRecorderId,
            boolean isExhaustiveSearch) {
        var selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        var entitySelectorConfig = Objects.requireNonNull(config.getEntitySelectorConfig()).copyConfig();
        var hasSortManner = configPolicy.getEntitySorterManner() != null
                && configPolicy.getEntitySorterManner() != NONE;
        var entityDescriptor = deduceEntityDescriptor(configPolicy, entitySelectorConfig.getEntityClass());
        var hasSorter = entityDescriptor.getDescendingSorter() != null;
        if (!isExhaustiveSearch && hasSortManner && hasSorter && entitySelectorConfig.getSorterManner() == null) {
            if (entityValueRangeRecorderId == null) {
                // Solution-range model
                entitySelectorConfig.setCacheType(SelectionCacheType.PHASE);
            } else {
                // The entity-range model requires sorting at each step
                // because the list of reachable entities can vary from one entity to another
                entitySelectorConfig.setCacheType(SelectionCacheType.STEP);
            }
            entitySelectorConfig.setSelectionOrder(SelectionOrder.SORTED);
            entitySelectorConfig.setSorterManner(configPolicy.getEntitySorterManner());
        }
        var valueRangeRecorderId = new ValueRangeRecorderId(entityValueRangeRecorderId, false);
        if (isExhaustiveSearch) {
            // The exhaustive search must not set the entity class for the entity selection configuration,
            // or the creation will fail.
            entitySelectorConfig.setEntityClass(null);
        }
        var entitySelector = EntitySelectorFactory.<Solution_> create(entitySelectorConfig).buildEntitySelector(configPolicy,
                minimumCacheType, selectionOrder, valueRangeRecorderId);
        if (isExhaustiveSearch) {
            // By default, exhaustive search will use a replaying entity selector,
            // but we must filter out the entity
            // if the selected value does not fall within the entity's value range
            entitySelector = EntitySelectorFactory.applyEntityValueRangeFilteringForExhaustiveSearch(configPolicy,
                    entitySelector, valueRangeRecorderId, minimumCacheType, selectionOrder);
        }
        var valueSelector = buildIterableValueSelector(configPolicy, entitySelector.getEntityDescriptor(),
                minimumCacheType, selectionOrder, entityValueRangeRecorderId);
        var replayingValueSelector = buildReplayingValueSelector(configPolicy, entityDescriptor, minimumCacheType,
                selectionOrder, entityValueRangeRecorderId);
        var baseDestinationSelector = new ElementDestinationSelector<>(entitySelector, replayingValueSelector, valueSelector,
                selectionOrder.toRandomSelectionBoolean(), isExhaustiveSearch);
        return applyNearbySelection(configPolicy, minimumCacheType, selectionOrder, baseDestinationSelector,
                entityValueRangeRecorderId != null);
    }

    private IterableValueSelector<Solution_> buildIterableValueSelector(
            HeuristicConfigPolicy<Solution_> configPolicy, EntityDescriptor<Solution_> entityDescriptor,
            SelectionCacheType minimumCacheType, SelectionOrder inheritedSelectionOrder, String entityValueRangeRecorderId) {
        // Destination selector does not require asserting both sides,
        // which means checking only if the destination entity accept the selected value
        ValueSelector<Solution_> valueSelector = ValueSelectorFactory
                .<Solution_> create(Objects.requireNonNull(config.getValueSelectorConfig()))
                .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, inheritedSelectionOrder,
                        // Do not override reinitializeVariableFilterEnabled.
                        configPolicy.isReinitializeVariableFilterEnabled(),
                        /*
                         * Filter assigned values (but only if this filtering type is allowed by the configPolicy).
                         *
                         * The destination selector requires the child value selector to only select assigned values.
                         * To guarantee this during CH, where not all values are assigned, the UnassignedValueSelector filter
                         * must be applied.
                         *
                         * In the LS phase, not only is the filter redundant because there are no unassigned values,
                         * but it would also crash if the base value selector inherits random selection order,
                         * because the filter cannot work on a never-ending child value selector.
                         * Therefore, it must not be applied even though it is requested here. This is accomplished by
                         * the configPolicy that only allows this filtering type in the CH phase.
                         */
                        ValueSelectorFactory.ListValueFilteringType.ACCEPT_ASSIGNED,
                        entityValueRangeRecorderId, false);
        return (IterableValueSelector<Solution_>) valueSelector;
    }

    private IterableValueSelector<Solution_> buildReplayingValueSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor, SelectionCacheType minimumCacheType, SelectionOrder selectionOrder,
            String entityValueRangeRecorderId) {
        if (entityValueRangeRecorderId == null) {
            return null;
        }
        var mimicValueSelectorConfig = new ValueSelectorConfig()
                .withMimicSelectorRef(entityValueRangeRecorderId);
        // We set the name for the list variable in case there are multiple variables present
        if (entityDescriptor.hasBothGenuineListAndBasicVariables()) {
            mimicValueSelectorConfig.setVariableName(entityDescriptor.getGenuineListVariableDescriptor().getVariableName());
        }
        return (IterableValueSelector<Solution_>) ValueSelectorFactory
                .<Solution_> create(mimicValueSelectorConfig)
                .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, selectionOrder);
    }

    private DestinationSelector<Solution_> applyNearbySelection(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, SelectionOrder selectionOrder,
            ElementDestinationSelector<Solution_> destinationSelector, boolean enableEntityValueRange) {
        NearbySelectionConfig nearbySelectionConfig = config.getNearbySelectionConfig();
        if (nearbySelectionConfig == null) {
            return destinationSelector;
        }
        // The nearby selector will implement its own logic to filter out unreachable elements.
        // It requires the child selectors to not be FilteringEntityValueRangeSelector or FilteringValueRangeSelector,
        // as it needs to iterate over all available values to construct the distance matrix.
        if (enableEntityValueRange) {
            var entitySelector =
                    EntitySelectorFactory.<Solution_> create(Objects.requireNonNull(config.getEntitySelectorConfig()))
                            .buildEntitySelector(configPolicy, minimumCacheType, selectionOrder);
            var valueSelector = ValueSelectorFactory
                    .<Solution_> create(Objects.requireNonNull(config.getValueSelectorConfig()))
                    .buildValueSelector(configPolicy, entitySelector.getEntityDescriptor(), minimumCacheType,
                            selectionOrder, configPolicy.isReinitializeVariableFilterEnabled(),
                            ValueSelectorFactory.ListValueFilteringType.ACCEPT_ASSIGNED, null, false);
            var updatedDestinationSelector =
                    new ElementDestinationSelector<>(entitySelector, (IterableValueSelector<Solution_>) valueSelector,
                            selectionOrder.toRandomSelectionBoolean());
            return TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.NEARBY_SELECTION)
                    .applyNearbySelection(config, configPolicy, minimumCacheType, selectionOrder,
                            updatedDestinationSelector);
        } else {
            return TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.NEARBY_SELECTION)
                    .applyNearbySelection(config, configPolicy, minimumCacheType, selectionOrder, destinationSelector);
        }
    }

}
