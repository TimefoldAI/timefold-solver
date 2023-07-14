package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

public class QueuedEntityPlacerFactory<Solution_>
        extends AbstractEntityPlacerFactory<Solution_, QueuedEntityPlacerConfig> {

    public static <Solution_> QueuedEntityPlacerConfig unfoldNew(HeuristicConfigPolicy<Solution_> configPolicy,
            List<MoveSelectorConfig> templateMoveSelectorConfigList) {
        QueuedEntityPlacerConfig config = new QueuedEntityPlacerConfig();
        config.setEntitySelectorConfig(new QueuedEntityPlacerFactory<Solution_>(config)
                .buildEntitySelectorConfig(configPolicy));
        config.setMoveSelectorConfigList(new ArrayList<>(templateMoveSelectorConfigList.size()));
        List<MoveSelectorConfig> leafMoveSelectorConfigList = new ArrayList<>(templateMoveSelectorConfigList.size());
        for (MoveSelectorConfig templateMoveSelectorConfig : templateMoveSelectorConfigList) {
            MoveSelectorConfig moveSelectorConfig = (MoveSelectorConfig) templateMoveSelectorConfig.copyConfig();
            moveSelectorConfig.extractLeafMoveSelectorConfigsIntoList(leafMoveSelectorConfigList);
            config.getMoveSelectorConfigList().add(moveSelectorConfig);
        }
        for (MoveSelectorConfig leafMoveSelectorConfig : leafMoveSelectorConfigList) {
            if (!(leafMoveSelectorConfig instanceof ChangeMoveSelectorConfig)) {
                throw new IllegalStateException("The <constructionHeuristic> contains a moveSelector ("
                        + leafMoveSelectorConfig + ") that isn't a <changeMoveSelector>, a <unionMoveSelector>"
                        + " or a <cartesianProductMoveSelector>.\n"
                        + "Maybe you're using a moveSelector in <constructionHeuristic>"
                        + " that's only supported for <localSearch>.");
            }
            ChangeMoveSelectorConfig changeMoveSelectorConfig = (ChangeMoveSelectorConfig) leafMoveSelectorConfig;
            if (changeMoveSelectorConfig.getEntitySelectorConfig() != null) {
                throw new IllegalStateException("The <constructionHeuristic> contains a changeMoveSelector ("
                        + changeMoveSelectorConfig + ") that contains an entitySelector ("
                        + changeMoveSelectorConfig.getEntitySelectorConfig()
                        + ") without explicitly configuring the <queuedEntityPlacer>.");
            }
            changeMoveSelectorConfig.setEntitySelectorConfig(
                    EntitySelectorConfig.newMimicSelectorConfig(config.getEntitySelectorConfig().getId()));
        }
        return config;
    }

    public QueuedEntityPlacerFactory(QueuedEntityPlacerConfig placerConfig) {
        super(placerConfig);
    }

    @Override
    public QueuedEntityPlacer<Solution_> buildEntityPlacer(HeuristicConfigPolicy<Solution_> configPolicy) {
        var entitySelectorConfig = buildEntitySelectorConfig(configPolicy);
        validateCacheType(entitySelectorConfig);

        var entitySelector = EntitySelectorFactory.<Solution_> create(entitySelectorConfig)
                .buildEntitySelector(configPolicy, SelectionCacheType.PHASE, SelectionOrder.ORIGINAL);

        var moveSelectorConfigList = config.getMoveSelectorConfigList();
        if (ConfigUtils.isEmptyCollection(moveSelectorConfigList)) {
            var entityDescriptor = entitySelector.getEntityDescriptor();
            var variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList();
            var subMoveSelectorConfigList = new ArrayList<MoveSelectorConfig>(variableDescriptorList.size());
            for (var variableDescriptor : variableDescriptorList) {
                subMoveSelectorConfigList
                        .add(buildChangeMoveSelectorConfig(configPolicy, entitySelectorConfig.getId(), variableDescriptor));
            }
            // Default to cartesian product (not a queue) of planning variables.
            var subMoveSelectorConfig =
                    (subMoveSelectorConfigList.size() > 1) ? new CartesianProductMoveSelectorConfig(subMoveSelectorConfigList)
                            : subMoveSelectorConfigList.get(0);
            moveSelectorConfigList = Collections.singletonList(subMoveSelectorConfig);
        }
        var moveSelectorList = new ArrayList<MoveSelector<Solution_>>(moveSelectorConfigList.size());
        for (var moveSelectorConfig : moveSelectorConfigList) {
            var moveSelector = MoveSelectorFactory.<Solution_> create(moveSelectorConfig)
                    .buildMoveSelector(configPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL, false);
            moveSelectorList.add(moveSelector);
        }
        return new QueuedEntityPlacer<>(entitySelector, moveSelectorList);
    }

    public EntitySelectorConfig buildEntitySelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        EntitySelectorConfig entitySelectorConfig_;
        if (config.getEntitySelectorConfig() == null) {
            EntityDescriptor<Solution_> entityDescriptor = getTheOnlyEntityDescriptor(configPolicy.getSolutionDescriptor());
            entitySelectorConfig_ = getDefaultEntitySelectorConfigForEntity(configPolicy, entityDescriptor);
        } else {
            entitySelectorConfig_ = config.getEntitySelectorConfig();
        }
        return entitySelectorConfig_;
    }
}
