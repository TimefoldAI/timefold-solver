package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwapMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, SwapMoveSelectorConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwapMoveSelectorFactory.class);

    public SwapMoveSelectorFactory(SwapMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        var entitySelectorConfig =
                Objects.requireNonNullElseGet(config.getEntitySelectorConfig(), EntitySelectorConfig::new);
        var secondaryEntitySelectorConfig =
                Objects.requireNonNullElse(config.getSecondaryEntitySelectorConfig(), entitySelectorConfig);
        var selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        var leftEntitySelector =
                EntitySelectorFactory.<Solution_> create(entitySelectorConfig)
                        .buildEntitySelector(configPolicy, minimumCacheType, selectionOrder);
        var rightEntitySelector =
                EntitySelectorFactory.<Solution_> create(secondaryEntitySelectorConfig)
                        .buildEntitySelector(configPolicy, minimumCacheType, selectionOrder);
        var entityDescriptor = leftEntitySelector.getEntityDescriptor();
        var variableDescriptorList = deduceBasicVariableDescriptorList(entityDescriptor, config.getVariableNameIncludeList());

        return new SwapMoveSelector<>(leftEntitySelector, rightEntitySelector, variableDescriptorList,
                randomSelection);
    }

    @Override
    protected MoveSelectorConfig<?> buildUnfoldedMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        var onlyEntityDescriptor = config.getEntitySelectorConfig() == null ? null
                : EntitySelectorFactory.<Solution_> create(config.getEntitySelectorConfig())
                        .extractEntityDescriptor(configPolicy);
        if (config.getSecondaryEntitySelectorConfig() != null) {
            var onlySecondaryEntityDescriptor =
                    EntitySelectorFactory.<Solution_> create(config.getSecondaryEntitySelectorConfig())
                            .extractEntityDescriptor(configPolicy);
            if (onlyEntityDescriptor != onlySecondaryEntityDescriptor) {
                throw new IllegalArgumentException(
                        "The entitySelector (%s)'s entityClass (%s) and secondaryEntitySelectorConfig (%s)'s entityClass (%s) must be the same entity class."
                                .formatted(config.getEntitySelectorConfig(),
                                        onlyEntityDescriptor == null ? null : onlyEntityDescriptor.getEntityClass(),
                                        config.getSecondaryEntitySelectorConfig(), onlySecondaryEntityDescriptor == null ? null
                                                : onlySecondaryEntityDescriptor.getEntityClass()));
            }
        }
        if (onlyEntityDescriptor != null) {
            var variableDescriptorList = onlyEntityDescriptor.getGenuineVariableDescriptorList();
            // If there is a single list variable, unfold to list swap move selector config.
            if (variableDescriptorList.size() == 1 && variableDescriptorList.get(0).isListVariable()) {
                return buildListSwapMoveSelectorConfig(variableDescriptorList.get(0), true);
            }
            // No need for unfolding or deducing
            return null;
        }
        Collection<EntityDescriptor<Solution_>> entityDescriptors =
                configPolicy.getSolutionDescriptor().getGenuineEntityDescriptors();
        return buildUnfoldedMoveSelectorConfig(configPolicy, entityDescriptors);
    }

    protected MoveSelectorConfig<?>
            buildUnfoldedMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy,
                    Collection<EntityDescriptor<Solution_>> entityDescriptors) {
        var moveSelectorConfigList = new ArrayList<MoveSelectorConfig>(entityDescriptors.size());

        // When using a mixed model,
        // we only fetch basic variables to avoiding creating a list move,
        // and delegate it to the ListSwapMoveSelectorFactory.
        // The strategy aims to provide a more normalized move selector collection for mixed models.
        var variableDescriptorList = configPolicy.getSolutionDescriptor().hasBothBasicAndListVariables()
                ? entityDescriptors.iterator().next().getGenuineBasicVariableDescriptorList()
                : entityDescriptors.iterator().next().getGenuineVariableDescriptorList();

        // Only unfold into list swap move selector for the basic scenario with 1 entity and 1 list variable.
        if (entityDescriptors.size() == 1 && variableDescriptorList.size() == 1
                && variableDescriptorList.get(0).isListVariable()) {
            // No childMoveSelectorConfig.inherit() because of unfoldedMoveSelectorConfig.inheritFolded()
            var childMoveSelectorConfig = buildListSwapMoveSelectorConfig(variableDescriptorList.get(0), false);
            moveSelectorConfigList.add(childMoveSelectorConfig);
        } else {
            // More complex scenarios do not support unfolding into list swap => fail fast if there is any list variable.
            for (var entityDescriptor : entityDescriptors) {
                if (!entityDescriptor.hasAnyGenuineBasicVariables()) {
                    // We filter out entities that do not have basic variables (e.g., mixed models)
                    continue;
                }
                // No childMoveSelectorConfig.inherit() because of unfoldedMoveSelectorConfig.inheritFolded()
                var childMoveSelectorConfig = new SwapMoveSelectorConfig();
                var childEntitySelectorConfig = new EntitySelectorConfig(config.getEntitySelectorConfig());
                if (childEntitySelectorConfig.getMimicSelectorRef() == null) {
                    childEntitySelectorConfig.setEntityClass(entityDescriptor.getEntityClass());
                }
                childMoveSelectorConfig.setEntitySelectorConfig(childEntitySelectorConfig);
                if (config.getSecondaryEntitySelectorConfig() != null) {
                    EntitySelectorConfig childSecondaryEntitySelectorConfig =
                            new EntitySelectorConfig(config.getSecondaryEntitySelectorConfig());
                    if (childSecondaryEntitySelectorConfig.getMimicSelectorRef() == null) {
                        childSecondaryEntitySelectorConfig.setEntityClass(entityDescriptor.getEntityClass());
                    }
                    childMoveSelectorConfig.setSecondaryEntitySelectorConfig(childSecondaryEntitySelectorConfig);
                }
                childMoveSelectorConfig.setVariableNameIncludeList(config.getVariableNameIncludeList());
                moveSelectorConfigList.add(childMoveSelectorConfig);
            }
        }
        if (moveSelectorConfigList.isEmpty()) {
            throw new IllegalStateException("The swap move selector cannot be created when there is no basic variables.");
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

    private ListSwapMoveSelectorConfig buildListSwapMoveSelectorConfig(VariableDescriptor<?> variableDescriptor,
            boolean inheritFoldedConfig) {
        LOGGER.warn(
                """
                        The swapMoveSelectorConfig ({}) is being used for a list variable.
                        We are keeping this option through the 1.x release stream for backward compatibility reasons.
                        Please update your solver config to use {} now.""",
                config, ListSwapMoveSelectorConfig.class.getSimpleName());
        var listSwapMoveSelectorConfig = new ListSwapMoveSelectorConfig();
        var childValueSelectorConfig = new ValueSelectorConfig(
                new ValueSelectorConfig(variableDescriptor.getVariableName()));
        listSwapMoveSelectorConfig.setValueSelectorConfig(childValueSelectorConfig);
        if (inheritFoldedConfig) {
            listSwapMoveSelectorConfig.inheritFolded(config);
        }
        return listSwapMoveSelectorConfig;
    }
}
