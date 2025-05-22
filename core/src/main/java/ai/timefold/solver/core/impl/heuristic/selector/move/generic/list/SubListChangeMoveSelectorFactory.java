package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

public class SubListChangeMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, SubListChangeMoveSelectorConfig> {

    public SubListChangeMoveSelectorFactory(SubListChangeMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        var subListSelectorConfig = checkUnfolded("subListSelectorConfig", config.getSubListSelectorConfig());
        var destinationSelectorConfig = checkUnfolded("destinationSelectorConfig", config.getDestinationSelectorConfig());
        if (!randomSelection) {
            throw new IllegalArgumentException("The subListChangeMoveSelector (%s) only supports random selection order."
                    .formatted(config));
        }
        var selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        var entitySelector = EntitySelectorFactory
                .<Solution_> create(destinationSelectorConfig.getEntitySelectorConfig())
                .buildEntitySelector(configPolicy, minimumCacheType, selectionOrder);
        var subListSelector = SubListSelectorFactory
                .<Solution_> create(subListSelectorConfig)
                .buildSubListSelector(configPolicy, entitySelector, minimumCacheType, selectionOrder);
        var destinationSelector = DestinationSelectorFactory
                .<Solution_> create(destinationSelectorConfig)
                .buildDestinationSelector(configPolicy, minimumCacheType, randomSelection);
        var selectReversingMoveToo = Objects.requireNonNullElse(config.getSelectReversingMoveToo(), true);
        return new RandomSubListChangeMoveSelector<>(subListSelector, destinationSelector, selectReversingMoveToo);
    }

    @Override
    protected MoveSelectorConfig<?> buildUnfoldedMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        var destinationSelectorConfig = config.getDestinationSelectorConfig();
        var destinationEntitySelectorConfig = destinationSelectorConfig == null ? null
                : destinationSelectorConfig.getEntitySelectorConfig();
        Collection<EntityDescriptor<Solution_>> entityDescriptors;
        var onlyEntityDescriptor = destinationEntitySelectorConfig == null ? null
                : EntitySelectorFactory.<Solution_> create(destinationEntitySelectorConfig)
                        .extractEntityDescriptor(configPolicy);
        if (onlyEntityDescriptor != null) {
            entityDescriptors = Collections.singletonList(onlyEntityDescriptor);
        } else {
            // We select a single entity since there is only one descriptor that includes a list variable
            var onlyEntityDescriptorWithListVariable =
                    getTheOnlyEntityDescriptorWithListVariable(configPolicy.getSolutionDescriptor());
            entityDescriptors = new ArrayList<>();
            if (onlyEntityDescriptorWithListVariable != null) {
                entityDescriptors.add(onlyEntityDescriptorWithListVariable);
            }
        }
        if (entityDescriptors.isEmpty()) {
            throw new IllegalArgumentException(
                    "The subListChangeMoveSelector (%s) cannot unfold because there are no planning list variables."
                            .formatted(config));
        }
        var entityDescriptor = entityDescriptors.iterator().next();

        var subListSelectorConfig = config.getSubListSelectorConfig();
        var subListValueSelectorConfig = subListSelectorConfig == null ? null
                : subListSelectorConfig.getValueSelectorConfig();
        var variableDescriptorList = new ArrayList<ListVariableDescriptor<Solution_>>();
        var onlySubListVariableDescriptor = subListValueSelectorConfig == null ? null
                : ValueSelectorFactory.<Solution_> create(subListValueSelectorConfig)
                        .extractVariableDescriptor(configPolicy, entityDescriptor);
        var destinationValueSelectorConfig = destinationSelectorConfig == null ? null
                : destinationSelectorConfig.getValueSelectorConfig();
        var onlyDestinationVariableDescriptor = destinationValueSelectorConfig == null ? null
                : ValueSelectorFactory.<Solution_> create(destinationValueSelectorConfig)
                        .extractVariableDescriptor(configPolicy, entityDescriptor);
        if (onlySubListVariableDescriptor != null && onlyDestinationVariableDescriptor != null) {
            if (!onlySubListVariableDescriptor.isListVariable()) {
                throw new IllegalArgumentException(
                        "The subListChangeMoveSelector (%s) is configured to use a planning variable (%s), which is not a planning list variable."
                                .formatted(config, onlySubListVariableDescriptor));
            }
            if (!onlyDestinationVariableDescriptor.isListVariable()) {
                throw new IllegalArgumentException(
                        "The subListChangeMoveSelector (%s) is configured to use a planning variable (%s), which is not a planning list variable."
                                .formatted(config, onlyDestinationVariableDescriptor));
            }
            if (onlySubListVariableDescriptor != onlyDestinationVariableDescriptor) {
                throw new IllegalArgumentException(
                        "The subListSelector's valueSelector (%s) and destinationSelector's valueSelector (%s) must be configured for the same planning variable."
                                .formatted(subListValueSelectorConfig, destinationEntitySelectorConfig));
            }
            if (onlyEntityDescriptor != null) {
                // No need for unfolding or deducing
                return null;
            }
            variableDescriptorList.add((ListVariableDescriptor<Solution_>) onlySubListVariableDescriptor);
        } else {
            variableDescriptorList.addAll(
                    entityDescriptor.getGenuineVariableDescriptorList().stream()
                            .filter(VariableDescriptor::isListVariable)
                            .map(variableDescriptor -> ((ListVariableDescriptor<Solution_>) variableDescriptor))
                            .toList());
        }
        if (variableDescriptorList.size() > 1) {
            throw new IllegalArgumentException(
                    "The subListChangeMoveSelector (%s) cannot unfold because there are multiple planning list variables."
                            .formatted(config));
        }
        return buildChildMoveSelectorConfig(variableDescriptorList.get(0));
    }

    private SubListChangeMoveSelectorConfig buildChildMoveSelectorConfig(ListVariableDescriptor<?> variableDescriptor) {
        var subListSelectorConfig = config.getSubListSelectorConfig();
        var destinationSelectorConfig = config.getDestinationSelectorConfig();
        var subListChangeMoveSelectorConfig = config.copyConfig()
                .withSubListSelectorConfig(new SubListSelectorConfig(subListSelectorConfig)
                        .withValueSelectorConfig(Optional.ofNullable(subListSelectorConfig)
                                .map(SubListSelectorConfig::getValueSelectorConfig)
                                .map(ValueSelectorConfig::new) // use copy constructor if inherited not null
                                .orElseGet(ValueSelectorConfig::new)))
                .withDestinationSelectorConfig(new DestinationSelectorConfig(destinationSelectorConfig)
                        .withEntitySelectorConfig(
                                Optional.ofNullable(destinationSelectorConfig)
                                        .map(DestinationSelectorConfig::getEntitySelectorConfig)
                                        .map(EntitySelectorConfig::new) // use copy constructor if inherited not null
                                        .orElseGet(EntitySelectorConfig::new) // otherwise create new instance
                                        // override entity class (destination entity selector is never replaying)
                                        .withEntityClass(variableDescriptor.getEntityDescriptor().getEntityClass()))
                        .withValueSelectorConfig(
                                Optional.ofNullable(destinationSelectorConfig)
                                        .map(DestinationSelectorConfig::getValueSelectorConfig)
                                        .map(ValueSelectorConfig::new) // use copy constructor if inherited not null
                                        .orElseGet(ValueSelectorConfig::new) // otherwise create new instance
                                        // override variable name (destination value selector is never replaying)
                                        .withVariableName(variableDescriptor.getVariableName())));

        subListSelectorConfig = Objects.requireNonNull(subListChangeMoveSelectorConfig.getSubListSelectorConfig());
        SubListConfigUtil.transferDeprecatedMinimumSubListSize(
                subListChangeMoveSelectorConfig,
                SubListChangeMoveSelectorConfig::getMinimumSubListSize,
                "subListSelector",
                subListSelectorConfig);
        SubListConfigUtil.transferDeprecatedMaximumSubListSize(
                subListChangeMoveSelectorConfig,
                SubListChangeMoveSelectorConfig::getMaximumSubListSize,
                "subListSelector",
                subListSelectorConfig);
        if (subListSelectorConfig.getMimicSelectorRef() == null) {
            Objects.requireNonNull(subListSelectorConfig.getValueSelectorConfig())
                    .setVariableName(variableDescriptor.getVariableName());
        }
        return subListChangeMoveSelectorConfig;
    }
}
