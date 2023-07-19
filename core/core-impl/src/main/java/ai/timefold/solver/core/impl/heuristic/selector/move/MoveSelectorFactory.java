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
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.KOptMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
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

public interface MoveSelectorFactory<Solution_> {

    static <Solution_> AbstractMoveSelectorFactory<Solution_, ?> create(MoveSelectorConfig<?> moveSelectorConfig) {
        if (ChangeMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new ChangeMoveSelectorFactory<>((ChangeMoveSelectorConfig) moveSelectorConfig);
        } else if (SwapMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new SwapMoveSelectorFactory<>((SwapMoveSelectorConfig) moveSelectorConfig);
        } else if (ListChangeMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new ListChangeMoveSelectorFactory<>((ListChangeMoveSelectorConfig) moveSelectorConfig);
        } else if (ListSwapMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new ListSwapMoveSelectorFactory<>((ListSwapMoveSelectorConfig) moveSelectorConfig);
        } else if (PillarChangeMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new PillarChangeMoveSelectorFactory<>((PillarChangeMoveSelectorConfig) moveSelectorConfig);
        } else if (PillarSwapMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new PillarSwapMoveSelectorFactory<>((PillarSwapMoveSelectorConfig) moveSelectorConfig);
        } else if (UnionMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new UnionMoveSelectorFactory<>((UnionMoveSelectorConfig) moveSelectorConfig);
        } else if (CartesianProductMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new CartesianProductMoveSelectorFactory<>((CartesianProductMoveSelectorConfig) moveSelectorConfig);
        } else if (SubListChangeMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new SubListChangeMoveSelectorFactory<>((SubListChangeMoveSelectorConfig) moveSelectorConfig);
        } else if (SubListSwapMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new SubListSwapMoveSelectorFactory<>((SubListSwapMoveSelectorConfig) moveSelectorConfig);
        } else if (SubChainChangeMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new SubChainChangeMoveSelectorFactory<>((SubChainChangeMoveSelectorConfig) moveSelectorConfig);
        } else if (SubChainSwapMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new SubChainSwapMoveSelectorFactory<>((SubChainSwapMoveSelectorConfig) moveSelectorConfig);
        } else if (TailChainSwapMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new TailChainSwapMoveSelectorFactory<>((TailChainSwapMoveSelectorConfig) moveSelectorConfig);
        } else if (MoveIteratorFactoryConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new MoveIteratorFactoryFactory<>((MoveIteratorFactoryConfig) moveSelectorConfig);
        } else if (MoveListFactoryConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new MoveListFactoryFactory<>((MoveListFactoryConfig) moveSelectorConfig);
        } else if (KOptMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new KOptMoveSelectorFactory<>((KOptMoveSelectorConfig) moveSelectorConfig);
        } else if (KOptListMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
            return new KOptListMoveSelectorFactory<>((KOptListMoveSelectorConfig) moveSelectorConfig);
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
