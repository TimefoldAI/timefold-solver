package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.pillar.PillarSelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.pillar.PillarSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

public class PillarChangeMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, PillarChangeMoveSelectorConfig> {

    public PillarChangeMoveSelectorFactory(PillarChangeMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        PillarSelectorConfig pillarSelectorConfig =
                Objects.requireNonNullElseGet(config.getPillarSelectorConfig(), PillarSelectorConfig::new);
        ValueSelectorConfig valueSelectorConfig =
                Objects.requireNonNullElseGet(config.getValueSelectorConfig(), ValueSelectorConfig::new);
        List<String> variableNameIncludeList = config.getValueSelectorConfig() == null
                || config.getValueSelectorConfig().getVariableName() == null ? null
                        : Collections.singletonList(config.getValueSelectorConfig().getVariableName());
        SelectionOrder selectionOrder = SelectionOrder.fromRandomSelectionBoolean(randomSelection);
        PillarSelector<Solution_> pillarSelector = PillarSelectorFactory.<Solution_> create(pillarSelectorConfig)
                .buildPillarSelector(configPolicy, config.getSubPillarType(),
                        (Class<? extends Comparator<Object>>) config.getSubPillarSequenceComparatorClass(),
                        minimumCacheType, selectionOrder, variableNameIncludeList);
        ValueSelector<Solution_> valueSelector = ValueSelectorFactory.<Solution_> create(valueSelectorConfig)
                .buildValueSelector(configPolicy, pillarSelector.getEntityDescriptor(), minimumCacheType, selectionOrder);
        return new PillarChangeMoveSelector<>(pillarSelector, valueSelector, randomSelection);
    }
}
