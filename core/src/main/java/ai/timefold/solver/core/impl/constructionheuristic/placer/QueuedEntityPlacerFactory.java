package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;

import org.jspecify.annotations.NonNull;

public class QueuedEntityPlacerFactory<Solution_>
        extends AbstractEntityPlacerFactory<Solution_, QueuedEntityPlacerConfig> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <Solution_> QueuedEntityPlacerConfig unfoldNew(HeuristicConfigPolicy<Solution_> configPolicy,
            List<MoveSelectorConfig> templateMoveSelectorConfigList) {
        var config = new QueuedEntityPlacerConfig();
        var entitySelectorConfig =
                new QueuedEntityPlacerFactory<Solution_>(config).buildEntitySelectorConfig(configPolicy, config);
        config.setEntitySelectorConfig(entitySelectorConfig);
        var moveSelectorConfigList = new ArrayList<MoveSelectorConfig>(templateMoveSelectorConfigList.size());
        config.setMoveSelectorConfigList(moveSelectorConfigList);
        var leafMoveSelectorConfigList = new ArrayList<MoveSelectorConfig>(templateMoveSelectorConfigList.size());
        for (var templateMoveSelectorConfig : templateMoveSelectorConfigList) {
            var moveSelectorConfig = (MoveSelectorConfig) templateMoveSelectorConfig.copyConfig();
            moveSelectorConfig.extractLeafMoveSelectorConfigsIntoList(leafMoveSelectorConfigList);
            moveSelectorConfigList.add(moveSelectorConfig);
        }
        for (var leafMoveSelectorConfig : leafMoveSelectorConfigList) {
            if (!(leafMoveSelectorConfig instanceof ChangeMoveSelectorConfig changeMoveSelectorConfig)) {
                throw new IllegalStateException(
                        """
                                The <constructionHeuristic> contains a moveSelector (%s) that isn't a <changeMoveSelector>, \
                                a <unionMoveSelector> or a <cartesianProductMoveSelector>.
                                Maybe you're using a moveSelector in <constructionHeuristic> that's only supported for <localSearch>."""
                                .formatted(leafMoveSelectorConfig));
            }
            var changeMoveEntitySelectorConfig = changeMoveSelectorConfig.getEntitySelectorConfig();
            if (changeMoveEntitySelectorConfig != null) {
                throw new IllegalStateException(
                        "The <constructionHeuristic> contains a changeMoveSelector (%s) that contains an entitySelector (%s) without explicitly configuring the <queuedEntityPlacer>."
                                .formatted(changeMoveSelectorConfig, changeMoveEntitySelectorConfig));
            }
            changeMoveSelectorConfig.setEntitySelectorConfig(
                    EntitySelectorConfig.newMimicSelectorConfig(entitySelectorConfig.getId()));
        }
        return config;
    }

    public QueuedEntityPlacerFactory(QueuedEntityPlacerConfig placerConfig) {
        super(placerConfig);
    }

    @Override
    public QueuedEntityPlacer<Solution_> buildEntityPlacer(HeuristicConfigPolicy<Solution_> configPolicy) {
        var entitySelectorConfig_ = Objects.requireNonNullElseGet(config.getEntitySelectorConfig(),
                () -> buildEntitySelectorConfig(configPolicy, config));
        var entitySelector = EntitySelectorFactory.<Solution_> create(entitySelectorConfig_).buildEntitySelector(configPolicy,
                SelectionCacheType.PHASE, SelectionOrder.ORIGINAL);

        var moveSelectorConfigList_ =
                Objects.requireNonNullElseGet(config.getMoveSelectorConfigList(), () -> buildMoveSelectorConfig(configPolicy,
                        config, entitySelector.getEntityDescriptor(), entitySelectorConfig_));
        var moveSelectorList = new ArrayList<MoveSelector<Solution_>>(moveSelectorConfigList_.size());
        for (var moveSelectorConfig : moveSelectorConfigList_) {
            var moveSelector = MoveSelectorFactory.<Solution_> create(moveSelectorConfig)
                    .buildMoveSelector(configPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL, false);
            moveSelectorList.add(moveSelector);
        }
        return new QueuedEntityPlacer<>(this, configPolicy, entitySelector, moveSelectorList);
    }

    @SuppressWarnings("rawtypes")
    public static <Solution_> @NonNull List<MoveSelectorConfig> buildMoveSelectorConfig(
            HeuristicConfigPolicy<Solution_> configPolicy, QueuedEntityPlacerConfig config,
            EntityDescriptor<Solution_> entityDescriptor, EntitySelectorConfig entitySelectorConfig_) {
        var moveSelectorConfigList = config.getMoveSelectorConfigList();
        if (!ConfigUtils.isEmptyCollection(moveSelectorConfigList)) {
            return moveSelectorConfigList;
        }
        var variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList().stream()
                .filter(variableDescriptor -> !variableDescriptor.isListVariable())
                .toList();
        var subMoveSelectorConfigList = new ArrayList<MoveSelectorConfig>(variableDescriptorList.size());
        for (var variableDescriptor : variableDescriptorList) {
            subMoveSelectorConfigList
                    .add(buildChangeMoveSelectorConfig(configPolicy, entitySelectorConfig_.getId(), variableDescriptor));
        }
        if (subMoveSelectorConfigList.size() > 1) {
            // Default to cartesian product (not a queue) of planning variables.
            return Collections.singletonList(new CartesianProductMoveSelectorConfig(subMoveSelectorConfigList));
        } else {
            return Collections.singletonList(subMoveSelectorConfigList.get(0));
        }
    }

    public static <Solution_> EntitySelectorConfig buildEntitySelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy,
            QueuedEntityPlacerConfig config) {
        var entitySelectorConfig = config.getEntitySelectorConfig();
        if (entitySelectorConfig == null) {
            var entityDescriptor = getTheOnlyEntityDescriptorWithBasicVariables(configPolicy.getSolutionDescriptor(), config);
            entitySelectorConfig = getDefaultEntitySelectorConfigForEntity(configPolicy, entityDescriptor);
        } else {
            // The default phase configuration generates the entity selector config without an updated version of the configuration policy.
            // We need to ensure that there are no missing sorting settings.
            var entityDescriptor = deduceEntityDescriptor(configPolicy, config, entitySelectorConfig.getEntityClass());
            entitySelectorConfig = deduceEntitySortManner(configPolicy, entityDescriptor, entitySelectorConfig);
        }
        var cacheType = entitySelectorConfig.getCacheType();
        if (cacheType != null && cacheType.compareTo(SelectionCacheType.PHASE) < 0) {
            throw new IllegalArgumentException(
                    "The queuedEntityPlacer (%s) cannot have an entitySelectorConfig (%s) with a cacheType (%s) lower than %s."
                            .formatted(config, entitySelectorConfig, cacheType, SelectionCacheType.PHASE));
        }
        return entitySelectorConfig;
    }
}
