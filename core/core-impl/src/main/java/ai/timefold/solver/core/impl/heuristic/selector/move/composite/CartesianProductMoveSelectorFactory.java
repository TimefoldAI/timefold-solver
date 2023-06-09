package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class CartesianProductMoveSelectorFactory<Solution_>
        extends AbstractCompositeMoveSelectorFactory<Solution_, CartesianProductMoveSelectorConfig> {

    public CartesianProductMoveSelectorFactory(CartesianProductMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    public MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        List<MoveSelector<Solution_>> moveSelectorList = buildInnerMoveSelectors(config.getMoveSelectorList(),
                configPolicy, minimumCacheType, randomSelection);
        boolean ignoreEmptyChildIterators_ = Objects.requireNonNullElse(config.getIgnoreEmptyChildIterators(), true);
        return new CartesianProductMoveSelector<>(moveSelectorList, ignoreEmptyChildIterators_, randomSelection);
    }
}
