package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

public class ListSwapMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, ListSwapMoveSelectorConfig> {

    public ListSwapMoveSelectorFactory(ListSwapMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        var valueSelectorConfig =
                Objects.requireNonNullElseGet(config.getValueSelectorConfig(), ValueSelectorConfig::new);
        var secondaryValueSelectorConfig =
                Objects.requireNonNullElseGet(config.getSecondaryValueSelectorConfig(), valueSelectorConfig::copyConfig);
        var selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        var entityDescriptor = getTheOnlyEntityDescriptorWithListVariable(configPolicy.getSolutionDescriptor());
        // When enabling entity value range filtering,
        // the recorder ID from the origin selector is required for FilteringEntityValueRangeSelector and FilteringValueRangeSelector
        // to replay the selected value and return only reachable values.
        // Experiments have shown that if the move configuration includes a custom filter,
        // the advantages of using value-range filtering nodes are outweighed by the cost
        // of applying the filter afterward.
        // The value-range filtering nodes perform lookup operations
        // to ensure entity reachability before move filtering,
        // which can lead to unnecessary overhead if the move can still be filtered out by the root move filter.
        var enableEntityValueRangeFilter =
                // No filtering
                config.getFilterClass() == null
                        // Using entity value-range
                        && !entityDescriptor.getGenuineListVariableDescriptor().canExtractValueRangeFromSolution();
        // A null ID means to turn off the entity value range filtering
        String entityValueRangeRecorderId = null;
        if (enableEntityValueRangeFilter) {
            if (valueSelectorConfig.getId() == null && valueSelectorConfig.getMimicSelectorRef() == null) {
                var variableName = Objects.requireNonNull(valueSelectorConfig.getVariableName());
                // We set the id to make sure the value selector will use the mimic recorder
                entityValueRangeRecorderId = ConfigUtils.addRandomSuffix(variableName, configPolicy.getRandom());
                valueSelectorConfig.setId(entityValueRangeRecorderId);
            } else {
                entityValueRangeRecorderId = valueSelectorConfig.getId() != null ? valueSelectorConfig.getId()
                        : valueSelectorConfig.getMimicSelectorRef();
            }
        }
        var leftValueSelector = buildIterableValueSelector(configPolicy, entityDescriptor, valueSelectorConfig,
                minimumCacheType, selectionOrder, null);
        var rightValueSelector = buildIterableValueSelector(configPolicy, entityDescriptor,
                secondaryValueSelectorConfig, minimumCacheType, selectionOrder, entityValueRangeRecorderId);
        var variableDescriptor = leftValueSelector.getVariableDescriptor();
        // This may be redundant but emphasizes that the ListSwapMove is not designed to swap elements
        // on multiple list variables, unlike the SwapMove, which swaps all (basic) variables between left and right entities.
        if (variableDescriptor != rightValueSelector.getVariableDescriptor()) {
            throw new IllegalStateException(
                    "Impossible state: the leftValueSelector (%s) and the rightValueSelector (%s) have different variable descriptors. This should have failed fast during config unfolding."
                            .formatted(leftValueSelector, rightValueSelector));
        }

        return new ListSwapMoveSelector<>(leftValueSelector, rightValueSelector, randomSelection,
                !enableEntityValueRangeFilter);
    }

    private IterableValueSelector<Solution_> buildIterableValueSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor, ValueSelectorConfig valueSelectorConfig,
            SelectionCacheType minimumCacheType, SelectionOrder inheritedSelectionOrder,
            String entityValueRangeRecorderId) {
        // Swap moves require asserting both sides,
        // which means checking if the left and right entities accept the swapped values
        var valueSelector = ValueSelectorFactory.<Solution_> create(valueSelectorConfig)
                .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, inheritedSelectionOrder,
                        configPolicy.isReinitializeVariableFilterEnabled(), ValueSelectorFactory.ListValueFilteringType.NONE,
                        entityValueRangeRecorderId, true);
        return (IterableValueSelector<Solution_>) valueSelector;
    }

    @Override
    protected MoveSelectorConfig<?> buildUnfoldedMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        var entityDescriptor = getTheOnlyEntityDescriptorWithListVariable(configPolicy.getSolutionDescriptor());
        var onlyVariableDescriptor = config.getValueSelectorConfig() == null ? null
                : ValueSelectorFactory.<Solution_> create(config.getValueSelectorConfig())
                        .extractVariableDescriptor(configPolicy, entityDescriptor);
        if (config.getSecondaryValueSelectorConfig() != null) {
            var onlySecondaryVariableDescriptor =
                    ValueSelectorFactory.<Solution_> create(config.getSecondaryValueSelectorConfig())
                            .extractVariableDescriptor(configPolicy, entityDescriptor);
            if (onlyVariableDescriptor != onlySecondaryVariableDescriptor) {
                throw new IllegalArgumentException(
                        "The valueSelector (%s)'s variableName (%s) and secondaryValueSelectorConfig (%s)'s variableName (%s) must be the same planning list variable."
                                .formatted(config.getValueSelectorConfig(),
                                        onlyVariableDescriptor == null ? null : onlyVariableDescriptor.getVariableName(),
                                        config.getSecondaryValueSelectorConfig(), onlySecondaryVariableDescriptor == null ? null
                                                : onlySecondaryVariableDescriptor.getVariableName()));
            }
        }
        if (onlyVariableDescriptor != null) {
            if (!onlyVariableDescriptor.isListVariable()) {
                throw new IllegalArgumentException(
                        "Impossible state: the listSwapMoveSelector (%s) is configured to use a planning variable (%s), which is not a planning list variable. Either fix your annotations and use a @%s on the variable to make it work with listSwapMoveSelector or use a swapMoveSelector instead."
                                .formatted(config, onlyVariableDescriptor, PlanningListVariable.class.getSimpleName()));
            }
            // No need for unfolding or deducing
            return null;
        }
        var variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList().stream()
                .filter(VariableDescriptor::isListVariable)
                .map(variableDescriptor -> ((ListVariableDescriptor<Solution_>) variableDescriptor))
                .toList();
        if (variableDescriptorList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Impossible state: the listSwapMoveSelector (%s) cannot unfold because there are no planning list variables for the only entity (%s) or no planning list variables at all."
                            .formatted(config, entityDescriptor));
        }
        return buildUnfoldedMoveSelectorConfig(variableDescriptorList);
    }

    protected MoveSelectorConfig<?>
            buildUnfoldedMoveSelectorConfig(List<ListVariableDescriptor<Solution_>> variableDescriptorList) {
        var moveSelectorConfigList = new ArrayList<MoveSelectorConfig>(variableDescriptorList.size());
        for (var variableDescriptor : variableDescriptorList) {
            // No childMoveSelectorConfig.inherit() because of unfoldedMoveSelectorConfig.inheritFolded()
            var childMoveSelectorConfig = new ListSwapMoveSelectorConfig();
            var childValueSelectorConfig = new ValueSelectorConfig(config.getValueSelectorConfig());
            if (childValueSelectorConfig.getMimicSelectorRef() == null) {
                childValueSelectorConfig.setVariableName(variableDescriptor.getVariableName());
            }
            childMoveSelectorConfig.setValueSelectorConfig(childValueSelectorConfig);
            if (config.getSecondaryValueSelectorConfig() != null) {
                var childSecondaryValueSelectorConfig = new ValueSelectorConfig(config.getSecondaryValueSelectorConfig());
                if (childSecondaryValueSelectorConfig.getMimicSelectorRef() == null) {
                    childSecondaryValueSelectorConfig.setVariableName(variableDescriptor.getVariableName());
                }
                childMoveSelectorConfig.setSecondaryValueSelectorConfig(childSecondaryValueSelectorConfig);
            }
            moveSelectorConfigList.add(childMoveSelectorConfig);
        }

        MoveSelectorConfig<?> unfoldedMoveSelectorConfig;
        if (moveSelectorConfigList.size() == 1) {
            unfoldedMoveSelectorConfig = moveSelectorConfigList.get(0);
        } else {
            unfoldedMoveSelectorConfig = new UnionMoveSelectorConfig(moveSelectorConfigList);
        }
        unfoldedMoveSelectorConfig.inheritFolded(config);
        return unfoldedMoveSelectorConfig;
    }
}
