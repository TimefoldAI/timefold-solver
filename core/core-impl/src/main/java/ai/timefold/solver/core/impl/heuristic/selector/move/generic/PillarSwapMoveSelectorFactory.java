package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.pillar.PillarSelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.pillar.PillarSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class PillarSwapMoveSelectorFactory<Solution_>
        extends AbstractMoveSelectorFactory<Solution_, PillarSwapMoveSelectorConfig> {

    public PillarSwapMoveSelectorFactory(PillarSwapMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        PillarSelectorConfig leftPillarSelectorConfig =
                Objects.requireNonNullElseGet(config.getPillarSelectorConfig(), PillarSelectorConfig::new);
        PillarSelectorConfig rightPillarSelectorConfig =
                Objects.requireNonNullElse(config.getSecondaryPillarSelectorConfig(), leftPillarSelectorConfig);
        PillarSelector<Solution_> leftPillarSelector =
                buildPillarSelector(leftPillarSelectorConfig, configPolicy, minimumCacheType, randomSelection);
        PillarSelector<Solution_> rightPillarSelector =
                buildPillarSelector(rightPillarSelectorConfig, configPolicy, minimumCacheType, randomSelection);

        List<GenuineVariableDescriptor<Solution_>> variableDescriptorList =
                deduceVariableDescriptorList(leftPillarSelector.getEntityDescriptor(), config.getVariableNameIncludeList());
        return new PillarSwapMoveSelector<>(leftPillarSelector, rightPillarSelector, variableDescriptorList, randomSelection);
    }

    private PillarSelector<Solution_> buildPillarSelector(PillarSelectorConfig pillarSelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, SelectionCacheType minimumCacheType, boolean randomSelection) {
        return PillarSelectorFactory.<Solution_> create(pillarSelectorConfig)
                .buildPillarSelector(configPolicy, config.getSubPillarType(),
                        (Class<? extends Comparator<Object>>) config.getSubPillarSequenceComparatorClass(), minimumCacheType,
                        SelectionOrder.fromRandomSelectionBoolean(randomSelection),
                        config.getVariableNameIncludeList());
    }

}
