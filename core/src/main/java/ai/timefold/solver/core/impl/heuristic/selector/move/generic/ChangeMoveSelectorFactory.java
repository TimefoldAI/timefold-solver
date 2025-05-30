package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, ChangeMoveSelectorConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeMoveSelectorFactory.class);

    public ChangeMoveSelectorFactory(ChangeMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        checkUnfolded("entitySelectorConfig", config.getEntitySelectorConfig());
        checkUnfolded("valueSelectorConfig", config.getValueSelectorConfig());
        var selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        var entitySelector = EntitySelectorFactory
                .<Solution_> create(config.getEntitySelectorConfig())
                .buildEntitySelector(configPolicy, minimumCacheType, selectionOrder);
        var valueSelector = ValueSelectorFactory
                .<Solution_> create(config.getValueSelectorConfig())
                .buildValueSelector(configPolicy, entitySelector.getEntityDescriptor(), minimumCacheType, selectionOrder);
        return new ChangeMoveSelector<>(entitySelector, valueSelector, randomSelection);
    }

    @Override
    protected MoveSelectorConfig<?> buildUnfoldedMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        Collection<EntityDescriptor<Solution_>> entityDescriptors;
        var onlyEntityDescriptor = config.getEntitySelectorConfig() == null ? null
                : EntitySelectorFactory.<Solution_> create(config.getEntitySelectorConfig())
                        .extractEntityDescriptor(configPolicy);
        if (onlyEntityDescriptor != null) {
            entityDescriptors = Collections.singletonList(onlyEntityDescriptor);
        } else {
            entityDescriptors = configPolicy.getSolutionDescriptor().getGenuineEntityDescriptors();
        }
        var variableDescriptorList = new ArrayList<GenuineVariableDescriptor<Solution_>>();
        for (EntityDescriptor<Solution_> entityDescriptor : entityDescriptors) {
            var onlyVariableDescriptor = config.getValueSelectorConfig() == null ? null
                    : ValueSelectorFactory.<Solution_> create(config.getValueSelectorConfig())
                            .extractVariableDescriptor(configPolicy, entityDescriptor);
            if (onlyVariableDescriptor != null) {
                if (onlyEntityDescriptor != null) {
                    if (onlyVariableDescriptor.isListVariable()) {
                        return buildListChangeMoveSelectorConfig((ListVariableDescriptor<?>) onlyVariableDescriptor, true);
                    }
                    // No need for unfolding or deducing
                    return null;
                }
                variableDescriptorList.add(onlyVariableDescriptor);
            } else {
                variableDescriptorList.addAll(entityDescriptor.getGenuineVariableDescriptorList());
            }
        }
        return buildUnfoldedMoveSelectorConfig(configPolicy, variableDescriptorList);
    }

    protected MoveSelectorConfig<?> buildUnfoldedMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy,
            List<GenuineVariableDescriptor<Solution_>> variableDescriptorList) {
        var moveSelectorConfigList = new ArrayList<MoveSelectorConfig>(variableDescriptorList.size());
        for (var variableDescriptor : variableDescriptorList) {
            if (variableDescriptor.isListVariable()) {
                if (configPolicy.getSolutionDescriptor().hasBothBasicAndListVariables()) {
                    // When using a mixed model,
                    // we do not create a list move
                    // and delegate it to the ListChangeMoveSelectorFactory.
                    // The strategy aims to provide a more normalized move selector collection for mixed models.
                    continue;
                }
                // No childMoveSelectorConfig.inherit() because of unfoldedMoveSelectorConfig.inheritFolded()
                var childMoveSelectorConfig =
                        buildListChangeMoveSelectorConfig((ListVariableDescriptor<?>) variableDescriptor, false);
                moveSelectorConfigList.add(childMoveSelectorConfig);
            } else {
                // No childMoveSelectorConfig.inherit() because of unfoldedMoveSelectorConfig.inheritFolded()
                var childMoveSelectorConfig = new ChangeMoveSelectorConfig();
                // Different EntitySelector per child because it is a union
                var childEntitySelectorConfig = new EntitySelectorConfig(config.getEntitySelectorConfig());
                if (childEntitySelectorConfig.getMimicSelectorRef() == null) {
                    childEntitySelectorConfig.setEntityClass(variableDescriptor.getEntityDescriptor().getEntityClass());
                }
                childMoveSelectorConfig.setEntitySelectorConfig(childEntitySelectorConfig);
                var childValueSelectorConfig = new ValueSelectorConfig(config.getValueSelectorConfig());
                if (childValueSelectorConfig.getMimicSelectorRef() == null) {
                    childValueSelectorConfig.setVariableName(variableDescriptor.getVariableName());
                }
                childMoveSelectorConfig.setValueSelectorConfig(childValueSelectorConfig);
                moveSelectorConfigList.add(childMoveSelectorConfig);
            }
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

    private ListChangeMoveSelectorConfig buildListChangeMoveSelectorConfig(ListVariableDescriptor<?> variableDescriptor,
            boolean inheritFoldedConfig) {
        LOGGER.warn(
                """
                        The changeMoveSelectorConfig ({}) is being used for a list variable.
                        We are keeping this option through the 1.x release stream for backward compatibility reasons.
                        Please update your solver config to use {} now.""",
                config, ListChangeMoveSelectorConfig.class.getSimpleName());
        var listChangeMoveSelectorConfig = ListChangeMoveSelectorFactory.buildChildMoveSelectorConfig(variableDescriptor,
                config.getValueSelectorConfig(), createDestinationSelectorConfig());
        if (inheritFoldedConfig) {
            listChangeMoveSelectorConfig.inheritFolded(config);
        }
        return listChangeMoveSelectorConfig;
    }

    private DestinationSelectorConfig createDestinationSelectorConfig() {
        var entitySelectorConfig = config.getEntitySelectorConfig();
        var valueSelectorConfig = config.getValueSelectorConfig();
        if (entitySelectorConfig == null) {
            if (valueSelectorConfig == null) {
                return new DestinationSelectorConfig();
            } else {
                return new DestinationSelectorConfig()
                        .withValueSelectorConfig(valueSelectorConfig);
            }
        } else {
            if (valueSelectorConfig == null) {
                return new DestinationSelectorConfig()
                        .withEntitySelectorConfig(entitySelectorConfig);
            } else {
                return new DestinationSelectorConfig()
                        .withEntitySelectorConfig(entitySelectorConfig)
                        .withValueSelectorConfig(valueSelectorConfig);
            }
        }
    }

}
