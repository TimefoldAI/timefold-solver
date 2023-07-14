package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.config.constructionheuristic.placer.PooledEntityPlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.impl.AbstractFromConfigFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;

public class PooledEntityPlacerFactory<Solution_>
        extends AbstractEntityPlacerFactory<Solution_, PooledEntityPlacerConfig> {

    public static <Solution_> PooledEntityPlacerConfig unfoldNew(HeuristicConfigPolicy<Solution_> configPolicy,
            MoveSelectorConfig templateMoveSelectorConfig) {
        PooledEntityPlacerConfig config = new PooledEntityPlacerConfig();
        List<MoveSelectorConfig> leafMoveSelectorConfigList = new ArrayList<>();
        MoveSelectorConfig moveSelectorConfig = (MoveSelectorConfig) templateMoveSelectorConfig.copyConfig();
        moveSelectorConfig.extractLeafMoveSelectorConfigsIntoList(leafMoveSelectorConfigList);
        config.setMoveSelectorConfig(moveSelectorConfig);

        EntitySelectorConfig entitySelectorConfig = null;
        for (MoveSelectorConfig leafMoveSelectorConfig : leafMoveSelectorConfigList) {
            if (!(leafMoveSelectorConfig instanceof ChangeMoveSelectorConfig)) {
                throw new IllegalStateException("The <constructionHeuristic> contains a moveSelector ("
                        + leafMoveSelectorConfig + ") that isn't a <changeMoveSelector>, a <unionMoveSelector>"
                        + " or a <cartesianProductMoveSelector>.\n"
                        + "Maybe you're using a moveSelector in <constructionHeuristic>"
                        + " that's only supported for <localSearch>.");
            }
            ChangeMoveSelectorConfig changeMoveSelectorConfig =
                    (ChangeMoveSelectorConfig) leafMoveSelectorConfig;
            if (changeMoveSelectorConfig.getEntitySelectorConfig() != null) {
                throw new IllegalStateException("The <constructionHeuristic> contains a changeMoveSelector ("
                        + changeMoveSelectorConfig + ") that contains an entitySelector ("
                        + changeMoveSelectorConfig.getEntitySelectorConfig()
                        + ") without explicitly configuring the <pooledEntityPlacer>.");
            }
            if (entitySelectorConfig == null) {
                EntityDescriptor<Solution_> entityDescriptor = new PooledEntityPlacerFactory<Solution_>(config)
                        .getTheOnlyEntityDescriptor(configPolicy.getSolutionDescriptor());
                entitySelectorConfig =
                        AbstractFromConfigFactory.getDefaultEntitySelectorConfigForEntity(configPolicy, entityDescriptor);
                changeMoveSelectorConfig.setEntitySelectorConfig(entitySelectorConfig);
            }
            changeMoveSelectorConfig.setEntitySelectorConfig(
                    EntitySelectorConfig.newMimicSelectorConfig(entitySelectorConfig.getId()));
        }
        return config;
    }

    public PooledEntityPlacerFactory(PooledEntityPlacerConfig placerConfig) {
        super(placerConfig);
    }

    @Override
    public PooledEntityPlacer<Solution_> buildEntityPlacer(HeuristicConfigPolicy<Solution_> configPolicy) {
        var moveSelectorConfig_ =
                config.getMoveSelectorConfig() == null ? buildMoveSelectorConfig(configPolicy) : config.getMoveSelectorConfig();
        var moveSelector = MoveSelectorFactory.<Solution_> create(moveSelectorConfig_)
                .buildMoveSelector(configPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL, false);
        return new PooledEntityPlacer<>(moveSelector);
    }

    private MoveSelectorConfig<?> buildMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        var entityDescriptor = getTheOnlyEntityDescriptor(configPolicy.getSolutionDescriptor());
        var entitySelectorConfig =
                AbstractFromConfigFactory.getDefaultEntitySelectorConfigForEntity(configPolicy, entityDescriptor);
        validateCacheType(entitySelectorConfig);

        var variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList();
        var subMoveSelectorConfigList = new ArrayList<MoveSelectorConfig>(variableDescriptorList.size());
        for (var variableDescriptor : variableDescriptorList) {
            subMoveSelectorConfigList
                    .add(buildChangeMoveSelectorConfig(configPolicy, entitySelectorConfig.getId(), variableDescriptor));
        }
        // The first entitySelectorConfig must be the mimic recorder, not the mimic replayer
        ((ChangeMoveSelectorConfig) subMoveSelectorConfigList.get(0)).setEntitySelectorConfig(entitySelectorConfig);
        // Default to cartesian product (not a union) of planning variables.
        return subMoveSelectorConfigList.size() > 1 ? new CartesianProductMoveSelectorConfig(subMoveSelectorConfigList)
                : subMoveSelectorConfigList.get(0);
    }
}
