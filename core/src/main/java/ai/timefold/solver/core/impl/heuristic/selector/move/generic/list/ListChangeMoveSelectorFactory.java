package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

public class ListChangeMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, ListChangeMoveSelectorConfig> {

    public ListChangeMoveSelectorFactory(ListChangeMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        var valueSelectorConfig = checkUnfolded("valueSelectorConfig", config.getValueSelectorConfig());
        var destinationSelectorConfig = checkUnfolded("destinationSelectorConfig", config.getDestinationSelectorConfig());
        var destinationEntitySelectorConfig =
                checkUnfolded("destinationEntitySelectorConfig", destinationSelectorConfig.getEntitySelectorConfig());
        checkUnfolded("destinationValueSelectorConfig", destinationSelectorConfig.getValueSelectorConfig());

        var selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);

        var entityDescriptor = EntitySelectorFactory
                .<Solution_> create(destinationEntitySelectorConfig)
                .extractEntityDescriptor(configPolicy);

        var sourceValueSelector = ValueSelectorFactory
                .<Solution_> create(valueSelectorConfig)
                .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, selectionOrder);

        if (!(sourceValueSelector instanceof EntityIndependentValueSelector<Solution_> castSourceValueSelector)) {
            throw new IllegalArgumentException("""
                    The listChangeMoveSelector (%s) for a list variable needs to be based on an %s (%s).
                    Check your valueSelectorConfig."""
                    .formatted(config, EntityIndependentValueSelector.class.getSimpleName(), sourceValueSelector));
        }

        var destinationSelector = DestinationSelectorFactory
                .<Solution_> create(destinationSelectorConfig)
                .buildDestinationSelector(configPolicy, minimumCacheType, randomSelection);

        return new ListChangeMoveSelector<>(castSourceValueSelector, destinationSelector, randomSelection);
    }

    @Override
    protected MoveSelectorConfig<?> buildUnfoldedMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        var destinationSelectorConfig = config.getDestinationSelectorConfig();
        var destinationEntitySelectorConfig = destinationSelectorConfig == null ? null
                : destinationSelectorConfig.getEntitySelectorConfig();
        var onlyEntityDescriptor = destinationEntitySelectorConfig == null ? null
                : EntitySelectorFactory.<Solution_> create(destinationEntitySelectorConfig)
                        .extractEntityDescriptor(configPolicy);
        var entityDescriptors =
                onlyEntityDescriptor == null ? configPolicy.getSolutionDescriptor().getGenuineEntityDescriptors().stream()
                        // We need to filter the entity that defines the list variable
                        .filter(EntityDescriptor::hasAnyGenuineListVariables)
                        .toList()
                        : Collections.singletonList(onlyEntityDescriptor);

        if (entityDescriptors.isEmpty()) {
            throw new IllegalArgumentException(
                    "The listChangeMoveSelector (%s) cannot unfold because there are no planning list variables."
                            .formatted(config));
        }

        if (entityDescriptors.size() > 1) {
            throw new IllegalArgumentException("""
                    The listChangeMoveSelector (%s) cannot unfold when there are multiple entities (%s).
                    Please use one listChangeMoveSelector per each planning list variable."""
                    .formatted(config, entityDescriptors));
        }
        var entityDescriptor = entityDescriptors.iterator().next();

        var variableDescriptorList = new ArrayList<ListVariableDescriptor<Solution_>>();
        var valueSelectorConfig = config.getValueSelectorConfig();
        var onlyVariableDescriptor = valueSelectorConfig == null ? null
                : ValueSelectorFactory.<Solution_> create(valueSelectorConfig)
                        .extractVariableDescriptor(configPolicy, entityDescriptor);
        var destinationValueSelectorConfig = destinationSelectorConfig == null ? null
                : destinationSelectorConfig.getValueSelectorConfig();
        var onlyDestinationVariableDescriptor =
                destinationValueSelectorConfig == null ? null
                        : ValueSelectorFactory.<Solution_> create(destinationValueSelectorConfig)
                                .extractVariableDescriptor(configPolicy, entityDescriptor);
        if (onlyVariableDescriptor != null && onlyDestinationVariableDescriptor != null) {
            if (!onlyVariableDescriptor.isListVariable()) {
                throw new IllegalArgumentException("""
                        The listChangeMoveSelector (%s) is configured to use a planning variable (%s), \
                        which is not a planning list variable.
                        Either fix your annotations and use a @%s on the variable to make it work with listChangeMoveSelector
                        or use a changeMoveSelector instead."""
                        .formatted(config, onlyVariableDescriptor, PlanningListVariable.class.getSimpleName()));
            }
            if (!onlyDestinationVariableDescriptor.isListVariable()) {
                throw new IllegalArgumentException(
                        "The destinationSelector (%s) is configured to use a planning variable (%s), which is not a planning list variable."
                                .formatted(destinationSelectorConfig, onlyDestinationVariableDescriptor));
            }
            if (onlyVariableDescriptor != onlyDestinationVariableDescriptor) {
                throw new IllegalArgumentException(
                        "The listChangeMoveSelector's valueSelector (%s) and destinationSelector's valueSelector (%s) must be configured for the same planning variable."
                                .formatted(valueSelectorConfig, destinationValueSelectorConfig));
            }
            if (onlyEntityDescriptor != null) {
                // No need for unfolding or deducing
                return null;
            }
            variableDescriptorList.add((ListVariableDescriptor<Solution_>) onlyVariableDescriptor);
        } else {
            variableDescriptorList.addAll(
                    entityDescriptor.getGenuineVariableDescriptorList().stream()
                            .filter(VariableDescriptor::isListVariable)
                            .map(variableDescriptor -> ((ListVariableDescriptor<Solution_>) variableDescriptor))
                            .toList());
        }
        if (variableDescriptorList.size() > 1) {
            throw new IllegalArgumentException(
                    "The listChangeMoveSelector (%s) cannot unfold because there are multiple planning list variables."
                            .formatted(config));
        }
        var listChangeMoveSelectorConfig =
                buildChildMoveSelectorConfig(variableDescriptorList.get(0), valueSelectorConfig, destinationSelectorConfig);
        listChangeMoveSelectorConfig.inheritFolded(config);
        return listChangeMoveSelectorConfig;
    }

    public static ListChangeMoveSelectorConfig buildChildMoveSelectorConfig(
            ListVariableDescriptor<?> variableDescriptor,
            ValueSelectorConfig inheritedValueSelectorConfig,
            DestinationSelectorConfig inheritedDestinationSelectorConfig) {

        var childValueSelectorConfig = new ValueSelectorConfig(inheritedValueSelectorConfig);
        if (childValueSelectorConfig.getMimicSelectorRef() == null) {
            childValueSelectorConfig.setVariableName(variableDescriptor.getVariableName());
        }

        return new ListChangeMoveSelectorConfig()
                .withValueSelectorConfig(childValueSelectorConfig)
                .withDestinationSelectorConfig(new DestinationSelectorConfig(inheritedDestinationSelectorConfig)
                        .withEntitySelectorConfig(
                                Optional.ofNullable(inheritedDestinationSelectorConfig)
                                        .map(DestinationSelectorConfig::getEntitySelectorConfig)
                                        .map(EntitySelectorConfig::new) // use copy constructor if inherited not null
                                        .orElseGet(EntitySelectorConfig::new) // otherwise create new instance
                                        // override entity class (destination entity selector is never replaying)
                                        .withEntityClass(variableDescriptor.getEntityDescriptor().getEntityClass()))
                        .withValueSelectorConfig(
                                Optional.ofNullable(inheritedDestinationSelectorConfig)
                                        .map(DestinationSelectorConfig::getValueSelectorConfig)
                                        .map(ValueSelectorConfig::new) // use copy constructor if inherited not null
                                        .orElseGet(ValueSelectorConfig::new) // otherwise create new instance
                                        // override variable name (destination value selector is never replaying)
                                        .withVariableName(variableDescriptor.getVariableName())));
    }
}
