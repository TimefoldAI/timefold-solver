package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DECREASING_DIFFICULTY;
import static ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.NONE;

import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
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
        return buildDestinationSelector(configPolicy, minimumCacheType, randomSelection, null);
    }

    public DestinationSelector<Solution_> buildDestinationSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection, String entityValueRangeRecorderId) {
        var selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        var entitySelectorConfig = Objects.requireNonNull(config.getEntitySelectorConfig()).copyConfig();
        var hasSortManner = configPolicy.getEntitySorterManner() != null
                && configPolicy.getEntitySorterManner() != NONE;
        var entityDescriptor = deduceEntityDescriptor(configPolicy, entitySelectorConfig.getEntityClass());
        var hasDifficultySorter = entityDescriptor.getDecreasingDifficultySorter() != null;
        var isEntityRangeSortingValid =
                // no entity value range, so we accept any sorting manner
                entityValueRangeRecorderId == null
                        // the entity value range is specified
                        // we only accept DECREASING_DIFFICULTY,
                        // indicating that the configuration requires sorting
                        || hasSortManner && configPolicy.getEntitySorterManner() == DECREASING_DIFFICULTY;

        if (hasSortManner && hasDifficultySorter && isEntityRangeSortingValid
                && entitySelectorConfig.getSorterManner() == null) {
            entitySelectorConfig.setCacheType(SelectionCacheType.PHASE);
            entitySelectorConfig.setSelectionOrder(SelectionOrder.SORTED);
            entitySelectorConfig.setSorterManner(configPolicy.getEntitySorterManner());
        }
        if (entityValueRangeRecorderId != null && entitySelectorConfig.getSelectionOrder() != null
                && entitySelectorConfig.getSelectionOrder() == SelectionOrder.SORTED) {
            // Sorting entities is not permitted when using an entity value range,
            // as the list of reachable entities is only generated after selecting a value.
            // This process prevents sorting and caching at the phase level.
            throw new IllegalStateException("""
                    The destination selector cannot to sort the entity list when an entity value range is used.
                    Maybe remove the setting "EntitySorterManner" from the phase config.
                    Maybe remove the entity selector sorting settings from the destination config.""");
        }
        var entitySelector = EntitySelectorFactory.<Solution_> create(entitySelectorConfig)
                .buildEntitySelector(configPolicy, minimumCacheType, selectionOrder,
                        new ValueRangeRecorderId(entityValueRangeRecorderId, false));
        var valueSelector = buildIterableValueSelector(configPolicy, entitySelector.getEntityDescriptor(),
                minimumCacheType, selectionOrder, entityValueRangeRecorderId);
        var baseDestinationSelector =
                new ElementDestinationSelector<>(entitySelector, valueSelector, selectionOrder.toRandomSelectionBoolean());
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
