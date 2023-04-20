package ai.timefold.solver.core.impl.heuristic.selector.move.factory;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class MoveIteratorFactoryFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, MoveIteratorFactoryConfig> {

    public MoveIteratorFactoryFactory(MoveIteratorFactoryConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    public MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        if (config.getMoveIteratorFactoryClass() == null) {
            throw new IllegalArgumentException("The moveIteratorFactoryConfig (" + config
                    + ") lacks a moveListFactoryClass (" + config.getMoveIteratorFactoryClass() + ").");
        }
        MoveIteratorFactory moveIteratorFactory = ConfigUtils.newInstance(config,
                "moveIteratorFactoryClass", config.getMoveIteratorFactoryClass());
        ConfigUtils.applyCustomProperties(moveIteratorFactory, "moveIteratorFactoryClass",
                config.getMoveIteratorFactoryCustomProperties(), "moveIteratorFactoryCustomProperties");
        return new MoveIteratorFactoryToMoveSelectorBridge<>(moveIteratorFactory, randomSelection);
    }
}
