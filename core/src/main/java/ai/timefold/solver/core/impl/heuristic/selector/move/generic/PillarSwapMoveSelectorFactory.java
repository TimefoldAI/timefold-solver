package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
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
        var leftPillarSelectorConfig =
                Objects.requireNonNullElseGet(config.getPillarSelectorConfig(), PillarSelectorConfig::new);
        EntityDescriptor<Solution_> leftEntityDescriptor = null;
        var leftEntitySelectorConfig = leftPillarSelectorConfig.getEntitySelectorConfig();
        if (leftEntitySelectorConfig != null && leftEntitySelectorConfig.getEntityClass() != null) {
            leftEntityDescriptor =
                    configPolicy.getSolutionDescriptor().findEntityDescriptor(leftEntitySelectorConfig.getEntityClass());
        }
        var leftVariableNameIncludeList = config.getVariableNameIncludeList();
        if (leftVariableNameIncludeList == null && leftEntityDescriptor != null
                && leftEntityDescriptor.hasAnyGenuineBasicVariables() && leftEntityDescriptor.hasAnyGenuineListVariables()) {
            // Mixed models filter out the list variable
            leftVariableNameIncludeList = leftEntityDescriptor.getGenuineBasicVariableDescriptorList().stream()
                    .map(GenuineVariableDescriptor::getVariableName)
                    .toList();
        }
        var rightPillarSelectorConfig = config.getSecondaryPillarSelectorConfig();
        if (rightPillarSelectorConfig == null) {
            rightPillarSelectorConfig = leftPillarSelectorConfig;
        }
        EntityDescriptor<Solution_> rightEntityDescriptor = null;
        var rightEntitySelectorConfig = rightPillarSelectorConfig.getEntitySelectorConfig();
        if (rightEntitySelectorConfig != null && rightEntitySelectorConfig.getEntityClass() != null) {
            rightEntityDescriptor =
                    configPolicy.getSolutionDescriptor().findEntityDescriptor(rightEntitySelectorConfig.getEntityClass());
        }
        var rightVariableNameIncludeList = config.getVariableNameIncludeList();
        if (rightVariableNameIncludeList == null && rightEntityDescriptor != null
                && rightEntityDescriptor.hasAnyGenuineBasicVariables() && rightEntityDescriptor.hasAnyGenuineListVariables()) {
            // Mixed models filter out the list variable
            rightVariableNameIncludeList = rightEntityDescriptor.getGenuineBasicVariableDescriptorList().stream()
                    .map(GenuineVariableDescriptor::getVariableName)
                    .toList();
        }
        var leftPillarSelector =
                buildPillarSelector(leftPillarSelectorConfig, configPolicy, leftVariableNameIncludeList, minimumCacheType,
                        randomSelection);
        var rightPillarSelector =
                buildPillarSelector(rightPillarSelectorConfig, configPolicy, rightVariableNameIncludeList, minimumCacheType,
                        randomSelection);

        var variableDescriptorList =
                deduceVariableDescriptorList(leftPillarSelector.getEntityDescriptor(), leftVariableNameIncludeList);
        return new PillarSwapMoveSelector<>(leftPillarSelector, rightPillarSelector, variableDescriptorList, randomSelection);
    }

    private PillarSelector<Solution_> buildPillarSelector(PillarSelectorConfig pillarSelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, List<String> variableNameIncludeList,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        return PillarSelectorFactory.<Solution_> create(pillarSelectorConfig)
                .buildPillarSelector(configPolicy, config.getSubPillarType(),
                        (Class<? extends Comparator<Object>>) config.getSubPillarSequenceComparatorClass(), minimumCacheType,
                        SelectionOrder.fromRandomSelectionBoolean(randomSelection), variableNameIncludeList);
    }

}
