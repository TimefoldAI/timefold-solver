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
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
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
        SelectionOrder selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        EntitySelector<Solution_> entitySelector = EntitySelectorFactory
                .<Solution_> create(config.getEntitySelectorConfig())
                .buildEntitySelector(configPolicy, minimumCacheType, selectionOrder);
        ValueSelector<Solution_> valueSelector = ValueSelectorFactory
                .<Solution_> create(config.getValueSelectorConfig())
                .buildValueSelector(configPolicy, entitySelector.getEntityDescriptor(), minimumCacheType, selectionOrder);
        return new ChangeMoveSelector<>(entitySelector, valueSelector, randomSelection);
    }

    @Override
    protected MoveSelectorConfig<?> buildUnfoldedMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        Collection<EntityDescriptor<Solution_>> entityDescriptors;
        EntityDescriptor<Solution_> onlyEntityDescriptor = config.getEntitySelectorConfig() == null ? null
                : EntitySelectorFactory.<Solution_> create(config.getEntitySelectorConfig())
                        .extractEntityDescriptor(configPolicy);
        if (onlyEntityDescriptor != null) {
            entityDescriptors = Collections.singletonList(onlyEntityDescriptor);
        } else {
            entityDescriptors = configPolicy.getSolutionDescriptor().getGenuineEntityDescriptors();
        }
        List<GenuineVariableDescriptor<Solution_>> variableDescriptorList = new ArrayList<>();
        for (EntityDescriptor<Solution_> entityDescriptor : entityDescriptors) {
            GenuineVariableDescriptor<Solution_> onlyVariableDescriptor = config.getValueSelectorConfig() == null ? null
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
        return buildUnfoldedMoveSelectorConfig(variableDescriptorList);
    }

    protected MoveSelectorConfig<?> buildUnfoldedMoveSelectorConfig(
            List<GenuineVariableDescriptor<Solution_>> variableDescriptorList) {
        List<MoveSelectorConfig> moveSelectorConfigList = new ArrayList<>(variableDescriptorList.size());
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : variableDescriptorList) {
            if (variableDescriptor.isListVariable()) {
                // No childMoveSelectorConfig.inherit() because of unfoldedMoveSelectorConfig.inheritFolded()
                ListChangeMoveSelectorConfig childMoveSelectorConfig =
                        buildListChangeMoveSelectorConfig((ListVariableDescriptor<?>) variableDescriptor, false);
                moveSelectorConfigList.add(childMoveSelectorConfig);
            } else {
                // No childMoveSelectorConfig.inherit() because of unfoldedMoveSelectorConfig.inheritFolded()
                ChangeMoveSelectorConfig childMoveSelectorConfig = new ChangeMoveSelectorConfig();
                // Different EntitySelector per child because it is a union
                EntitySelectorConfig childEntitySelectorConfig = new EntitySelectorConfig(config.getEntitySelectorConfig());
                if (childEntitySelectorConfig.getMimicSelectorRef() == null) {
                    childEntitySelectorConfig.setEntityClass(variableDescriptor.getEntityDescriptor().getEntityClass());
                }
                childMoveSelectorConfig.setEntitySelectorConfig(childEntitySelectorConfig);
                ValueSelectorConfig childValueSelectorConfig = new ValueSelectorConfig(config.getValueSelectorConfig());
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
        ListChangeMoveSelectorConfig listChangeMoveSelectorConfig = ListChangeMoveSelectorFactory.buildChildMoveSelectorConfig(
                variableDescriptor, config.getValueSelectorConfig(),
                new DestinationSelectorConfig()
                        .withEntitySelectorConfig(config.getEntitySelectorConfig())
                        .withValueSelectorConfig(config.getValueSelectorConfig()));
        if (inheritFoldedConfig) {
            listChangeMoveSelectorConfig.inheritFolded(config);
        }
        return listChangeMoveSelectorConfig;
    }
}
