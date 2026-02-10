package ai.timefold.solver.core.impl.heuristic.selector.move;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.MultistageMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListMultistageMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
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
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListSwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.ListRuinRecreateMoveSelectorFactory;

public interface MoveSelectorFactory<Solution_> {

    static <Solution_> AbstractMoveSelectorFactory<Solution_, ?> create(MoveSelectorConfig<?> moveSelectorConfig) {
        return switch (moveSelectorConfig) {
            case ChangeMoveSelectorConfig changeMoveSelectorConfig -> new ChangeMoveSelectorFactory<>(changeMoveSelectorConfig);
            case ListChangeMoveSelectorConfig listChangeMoveSelectorConfig ->
                new ListChangeMoveSelectorFactory<>(listChangeMoveSelectorConfig, false);
            case SwapMoveSelectorConfig swapMoveSelectorConfig -> new SwapMoveSelectorFactory<>(swapMoveSelectorConfig);
            case ListSwapMoveSelectorConfig listSwapMoveSelectorConfig ->
                new ListSwapMoveSelectorFactory<>(listSwapMoveSelectorConfig);
            case PillarChangeMoveSelectorConfig pillarChangeMoveSelectorConfig ->
                new PillarChangeMoveSelectorFactory<>(pillarChangeMoveSelectorConfig);
            case PillarSwapMoveSelectorConfig pillarSwapMoveSelectorConfig ->
                new PillarSwapMoveSelectorFactory<>(pillarSwapMoveSelectorConfig);
            case SubListChangeMoveSelectorConfig subListChangeMoveSelectorConfig ->
                new SubListChangeMoveSelectorFactory<>(subListChangeMoveSelectorConfig);
            case SubListSwapMoveSelectorConfig subListSwapMoveSelectorConfig ->
                new SubListSwapMoveSelectorFactory<>(subListSwapMoveSelectorConfig);
            case KOptListMoveSelectorConfig kOptListMoveSelectorConfig ->
                new KOptListMoveSelectorFactory<>(kOptListMoveSelectorConfig);
            case RuinRecreateMoveSelectorConfig ruinRecreateMoveSelectorConfig ->
                new RuinRecreateMoveSelectorFactory<>(ruinRecreateMoveSelectorConfig);
            case ListRuinRecreateMoveSelectorConfig listRuinRecreateMoveSelectorConfig ->
                new ListRuinRecreateMoveSelectorFactory<>(listRuinRecreateMoveSelectorConfig);
            case MoveIteratorFactoryConfig moveIteratorFactoryConfig ->
                new MoveIteratorFactoryFactory<>(moveIteratorFactoryConfig);
            case MoveListFactoryConfig moveListFactoryConfig -> new MoveListFactoryFactory<>(moveListFactoryConfig);
            case UnionMoveSelectorConfig unionMoveSelectorConfig -> new UnionMoveSelectorFactory<>(unionMoveSelectorConfig);
            case CartesianProductMoveSelectorConfig cartesianProductMoveSelectorConfig ->
                new CartesianProductMoveSelectorFactory<>(cartesianProductMoveSelectorConfig);
            case MultistageMoveSelectorConfig multistageMoveSelectorConfig -> {
                var enterpriseService = TimefoldSolverEnterpriseService
                        .loadOrFail(TimefoldSolverEnterpriseService.Feature.MULTISTAGE_MOVE);
                yield enterpriseService.buildBasicMultistageMoveSelectorFactory(multistageMoveSelectorConfig);
            }
            case ListMultistageMoveSelectorConfig listMultistageMoveSelectorConfig -> {
                var enterpriseService = TimefoldSolverEnterpriseService
                        .loadOrFail(TimefoldSolverEnterpriseService.Feature.MULTISTAGE_MOVE);
                yield enterpriseService.buildListMultistageMoveSelectorFactory(listMultistageMoveSelectorConfig);
            }
            default -> throw new IllegalArgumentException("Unknown %s type: (%s)."
                    .formatted(MoveSelectorConfig.class.getSimpleName(), moveSelectorConfig.getClass().getName()));
        };
    }

    static <Solution_> AbstractMoveSelectorFactory<Solution_, ?>
            createForExhaustiveSearch(MoveSelectorConfig<?> moveSelectorConfig) {
        if (moveSelectorConfig instanceof ListChangeMoveSelectorConfig listChangeMoveSelectorConfig) {
            // Enable the factory creation for the exhaustive search
            return new ListChangeMoveSelectorFactory<>(listChangeMoveSelectorConfig, true);
        } else {
            // Apply the default method to the remaining move selector configurations
            return create(moveSelectorConfig);
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
