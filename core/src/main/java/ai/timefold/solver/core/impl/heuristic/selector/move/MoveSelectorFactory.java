package ai.timefold.solver.core.impl.heuristic.selector.move;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.KOptMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.CartesianProductMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.UnionMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactoryFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactoryFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.PillarChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.PillarSwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.KOptMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListSwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.ListRuinRecreateMoveSelectorFactory;

public interface MoveSelectorFactory<Solution_> {

    static <Solution_> AbstractMoveSelectorFactory<Solution_, ?> create(MoveSelectorConfig<?> moveSelectorConfig) {
        if (moveSelectorConfig instanceof ChangeMoveSelectorConfig changeMoveSelectorConfig) {
            return new ChangeMoveSelectorFactory<>(changeMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof ListChangeMoveSelectorConfig listChangeMoveSelectorConfig) {
            return new ListChangeMoveSelectorFactory<>(listChangeMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof SwapMoveSelectorConfig swapMoveSelectorConfig) {
            return new SwapMoveSelectorFactory<>(swapMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof ListSwapMoveSelectorConfig listSwapMoveSelectorConfig) {
            return new ListSwapMoveSelectorFactory<>(listSwapMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof PillarChangeMoveSelectorConfig pillarChangeMoveSelectorConfig) {
            return new PillarChangeMoveSelectorFactory<>(pillarChangeMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof PillarSwapMoveSelectorConfig pillarSwapMoveSelectorConfig) {
            return new PillarSwapMoveSelectorFactory<>(pillarSwapMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof SubChainChangeMoveSelectorConfig subChainChangeMoveSelectorConfig) {
            return new SubChainChangeMoveSelectorFactory<>(subChainChangeMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof SubListChangeMoveSelectorConfig subListChangeMoveSelectorConfig) {
            return new SubListChangeMoveSelectorFactory<>(subListChangeMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof SubChainSwapMoveSelectorConfig subChainSwapMoveSelectorConfig) {
            return new SubChainSwapMoveSelectorFactory<>(subChainSwapMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof SubListSwapMoveSelectorConfig subListSwapMoveSelectorConfig) {
            return new SubListSwapMoveSelectorFactory<>(subListSwapMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof TailChainSwapMoveSelectorConfig tailChainSwapMoveSelectorConfig) {
            return new TailChainSwapMoveSelectorFactory<>(tailChainSwapMoveSelectorConfig);
        } else if (KOptMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new KOptMoveSelectorFactory<>((KOptMoveSelectorConfig) moveSelectorConfig);
        } else if (KOptListMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new KOptListMoveSelectorFactory<>((KOptListMoveSelectorConfig) moveSelectorConfig);
        } else if (moveSelectorConfig instanceof RuinRecreateMoveSelectorConfig ruinRecreateMoveSelectorConfig) {
            return new RuinRecreateMoveSelectorFactory<>(ruinRecreateMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof ListRuinRecreateMoveSelectorConfig listRuinRecreateMoveSelectorConfig) {
            return new ListRuinRecreateMoveSelectorFactory<>(listRuinRecreateMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof MoveIteratorFactoryConfig moveIteratorFactoryConfig) {
            return new MoveIteratorFactoryFactory<>(moveIteratorFactoryConfig);
        } else if (moveSelectorConfig instanceof MoveListFactoryConfig moveListFactoryConfig) {
            return new MoveListFactoryFactory<>(moveListFactoryConfig);
        } else if (moveSelectorConfig instanceof UnionMoveSelectorConfig unionMoveSelectorConfig) {
            return new UnionMoveSelectorFactory<>(unionMoveSelectorConfig);
        } else if (moveSelectorConfig instanceof CartesianProductMoveSelectorConfig cartesianProductMoveSelectorConfig) {
            return new CartesianProductMoveSelectorFactory<>(cartesianProductMoveSelectorConfig);
        } else {
            throw new IllegalArgumentException(String.format("Unknown %s type: (%s).",
                    MoveSelectorConfig.class.getSimpleName(), moveSelectorConfig.getClass().getName()));
        }
    }

    /**
     * Builds {@link MoveSelector} from the {@link MoveSelectorConfig} and provided parameters.
     *
     * @param configPolicy never null
     * @param minimumCacheType never null, If caching is used (different from {@link SelectionCacheType#JUST_IN_TIME}),
     *        then it should be at least this {@link SelectionCacheType} because an ancestor already uses such caching
     *        and less would be pointless.
     * @param inheritedSelectionOrder never null
     * @param skipNonDoableMoves
     * @return never null
     */
    MoveSelector<Solution_> buildMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, SelectionOrder inheritedSelectionOrder, boolean skipNonDoableMoves);
}
