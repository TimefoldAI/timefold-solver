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
            throw new IllegalArgumentException("The listChangeMoveSelector (" + config
                    + ") for a list variable needs to be based on an "
                    + EntityIndependentValueSelector.class.getSimpleName() + " (" + sourceValueSelector + ")."
                    + " Check your valueSelectorConfig.");
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
        var onlyEntityDescriptor = destinationSelectorConfig == null ? null
                : destinationEntitySelectorConfig == null ? null
                        : EntitySelectorFactory.<Solution_> create(destinationEntitySelectorConfig)
                                .extractEntityDescriptor(configPolicy);
        var entityDescriptors =
                onlyEntityDescriptor == null ? configPolicy.getSolutionDescriptor().getGenuineEntityDescriptors()
                        : Collections.singletonList(onlyEntityDescriptor);
        if (entityDescriptors.size() > 1) {
            throw new IllegalArgumentException("The listChangeMoveSelector (" + config
                    + ") cannot unfold when there are multiple entities (" + entityDescriptors + ")."
                    + " Please use one listChangeMoveSelector per each planning list variable.");
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
                destinationSelectorConfig == null ? null
                        : destinationValueSelectorConfig == null ? null
                                : ValueSelectorFactory.<Solution_> create(destinationValueSelectorConfig)
                                        .extractVariableDescriptor(configPolicy, entityDescriptor);
        if (onlyVariableDescriptor != null && onlyDestinationVariableDescriptor != null) {
            if (!onlyVariableDescriptor.isListVariable()) {
                throw new IllegalArgumentException("The listChangeMoveSelector (" + config
                        + ") is configured to use a planning variable (" + onlyVariableDescriptor
                        + "), which is not a planning list variable."
                        + " Either fix your annotations and use a @" + PlanningListVariable.class.getSimpleName()
                        + " on the variable to make it work with listChangeMoveSelector"
                        + " or use a changeMoveSelector instead.");
            }
            if (!onlyDestinationVariableDescriptor.isListVariable()) {
                throw new IllegalArgumentException("The destinationSelector (" + destinationSelectorConfig
                        + ") is configured to use a planning variable (" + onlyDestinationVariableDescriptor
                        + "), which is not a planning list variable.");
            }
            if (onlyVariableDescriptor != onlyDestinationVariableDescriptor) {
                throw new IllegalArgumentException("The listChangeMoveSelector's valueSelector ("
                        + valueSelectorConfig
                        + ") and destinationSelector's valueSelector ("
                        + destinationValueSelectorConfig
                        + ") must be configured for the same planning variable.");
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
        if (variableDescriptorList.isEmpty()) {
            throw new IllegalArgumentException("The listChangeMoveSelector (" + config
                    + ") cannot unfold because there are no planning list variables.");
        }
        if (variableDescriptorList.size() > 1) {
            throw new IllegalArgumentException("The listChangeMoveSelector (" + config
                    + ") cannot unfold because there are multiple planning list variables.");
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
