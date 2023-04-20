package ai.timefold.solver.core.impl.heuristic.selector.move.factory;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class MoveListFactoryFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, MoveListFactoryConfig> {

    public MoveListFactoryFactory(MoveListFactoryConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    public MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        if (config.getMoveListFactoryClass() == null) {
            throw new IllegalArgumentException("The moveListFactoryConfig (" + config
                    + ") lacks a moveListFactoryClass (" + config.getMoveListFactoryClass() + ").");
        }
        MoveListFactory<Solution_> moveListFactory =
                ConfigUtils.newInstance(config, "moveListFactoryClass", config.getMoveListFactoryClass());
        ConfigUtils.applyCustomProperties(moveListFactory, "moveListFactoryClass",
                config.getMoveListFactoryCustomProperties(), "moveListFactoryCustomProperties");
        // MoveListFactoryToMoveSelectorBridge caches by design, so it uses the minimumCacheType
        if (minimumCacheType.compareTo(SelectionCacheType.STEP) < 0) {
            // cacheType upgrades to SelectionCacheType.STEP (without shuffling) because JIT is not supported
            minimumCacheType = SelectionCacheType.STEP;
        }
        return new MoveListFactoryToMoveSelectorBridge<>(moveListFactory, minimumCacheType, randomSelection);
    }

    @Override
    protected boolean isBaseInherentlyCached() {
        return true;
    }
}
