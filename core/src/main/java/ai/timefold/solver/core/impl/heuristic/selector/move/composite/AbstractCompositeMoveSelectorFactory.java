package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;

abstract class AbstractCompositeMoveSelectorFactory<Solution_, MoveSelectorConfig_ extends MoveSelectorConfig<MoveSelectorConfig_>>
        extends AbstractMoveSelectorFactory<Solution_, MoveSelectorConfig_> {

    public AbstractCompositeMoveSelectorFactory(MoveSelectorConfig_ moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    protected List<MoveSelector<Solution_>> buildInnerMoveSelectors(List<MoveSelectorConfig> innerMoveSelectorList,
            HeuristicConfigPolicy<Solution_> configPolicy, SelectionCacheType minimumCacheType, boolean randomSelection) {
        return innerMoveSelectorList.stream()
                .map(moveSelectorConfig -> {
                    AbstractMoveSelectorFactory<Solution_, ?> moveSelectorFactory =
                            MoveSelectorFactory.create(moveSelectorConfig);
                    SelectionOrder selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
                    return moveSelectorFactory.buildMoveSelector(configPolicy, minimumCacheType, selectionOrder, false);
                }).collect(Collectors.toList());
    }
}
