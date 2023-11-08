package ai.timefold.solver.core.impl.heuristic.selector.list;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.AbstractFromConfigFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.MimicRecordingSubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.MimicReplayingSubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.SubListMimicRecorder;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;

public final class SubListSelectorFactory<Solution_> extends AbstractFromConfigFactory<Solution_, SubListSelectorConfig> {

    private static final int DEFAULT_MINIMUM_SUB_LIST_SIZE = 1;
    private static final int DEFAULT_MAXIMUM_SUB_LIST_SIZE = Integer.MAX_VALUE;

    private SubListSelectorFactory(SubListSelectorConfig config) {
        super(config);
    }

    public static <Solution_> SubListSelectorFactory<Solution_> create(SubListSelectorConfig subListSelectorConfig) {
        return new SubListSelectorFactory<>(subListSelectorConfig);
    }

    public SubListSelector<Solution_> buildSubListSelector(
            HeuristicConfigPolicy<Solution_> configPolicy,
            EntitySelector<Solution_> entitySelector,
            SelectionCacheType minimumCacheType,
            SelectionOrder inheritedSelectionOrder) {
        if (config.getMimicSelectorRef() != null) {
            return buildMimicReplaying(configPolicy);
        }
        if (inheritedSelectionOrder != SelectionOrder.RANDOM) {
            throw new IllegalArgumentException("The subListSelector (" + config
                    + ") has an inheritedSelectionOrder(" + inheritedSelectionOrder
                    + ") which is not supported. SubListSelector only supports random selection order.");
        }

        EntityIndependentValueSelector<Solution_> valueSelector = buildEntityIndependentValueSelector(configPolicy,
                entitySelector.getEntityDescriptor(), minimumCacheType, inheritedSelectionOrder);

        int minimumSubListSize = Objects.requireNonNullElse(config.getMinimumSubListSize(), DEFAULT_MINIMUM_SUB_LIST_SIZE);
        int maximumSubListSize = Objects.requireNonNullElse(config.getMaximumSubListSize(), DEFAULT_MAXIMUM_SUB_LIST_SIZE);
        RandomSubListSelector<Solution_> baseSubListSelector =
                new RandomSubListSelector<>(entitySelector, valueSelector, minimumSubListSize, maximumSubListSize);

        SubListSelector<Solution_> subListSelector =
                applyNearbySelection(configPolicy, minimumCacheType, inheritedSelectionOrder, baseSubListSelector);

        subListSelector = applyMimicRecording(configPolicy, subListSelector);

        return subListSelector;
    }

    SubListSelector<Solution_> buildMimicReplaying(HeuristicConfigPolicy<Solution_> configPolicy) {
        if (config.getId() != null
                || config.getMinimumSubListSize() != null
                || config.getMaximumSubListSize() != null
                || config.getValueSelectorConfig() != null
                || config.getNearbySelectionConfig() != null) {
            throw new IllegalArgumentException("The subListSelectorConfig (" + config
                    + ") with mimicSelectorRef (" + config.getMimicSelectorRef()
                    + ") has another property that is not null.");
        }
        SubListMimicRecorder<Solution_> subListMimicRecorder =
                configPolicy.getSubListMimicRecorder(config.getMimicSelectorRef());
        if (subListMimicRecorder == null) {
            throw new IllegalArgumentException("The subListSelectorConfig (" + config
                    + ") has a mimicSelectorRef (" + config.getMimicSelectorRef()
                    + ") for which no subListSelector with that id exists (in its solver phase).");
        }
        return new MimicReplayingSubListSelector<>(subListMimicRecorder);
    }

    private SubListSelector<Solution_> applyMimicRecording(HeuristicConfigPolicy<Solution_> configPolicy,
            SubListSelector<Solution_> subListSelector) {
        if (config.getId() != null) {
            if (config.getId().isEmpty()) {
                throw new IllegalArgumentException("The subListSelectorConfig (" + config
                        + ") has an empty id (" + config.getId() + ").");
            }
            MimicRecordingSubListSelector<Solution_> mimicRecordingSubListSelector =
                    new MimicRecordingSubListSelector<>(subListSelector);
            configPolicy.addSubListMimicRecorder(config.getId(), mimicRecordingSubListSelector);
            subListSelector = mimicRecordingSubListSelector;
        }
        return subListSelector;
    }

    private SubListSelector<Solution_> applyNearbySelection(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, SelectionOrder resolvedSelectionOrder,
            RandomSubListSelector<Solution_> subListSelector) {
        NearbySelectionConfig nearbySelectionConfig = config.getNearbySelectionConfig();
        if (nearbySelectionConfig == null) {
            return subListSelector;
        }
        return TimefoldSolverEnterpriseService
                .loadOrFail("Nearby selection", "remove nearby selection from solver configuration")
                .applyNearbySelection(config, configPolicy, minimumCacheType, resolvedSelectionOrder, subListSelector);
    }

    private EntityIndependentValueSelector<Solution_> buildEntityIndependentValueSelector(
            HeuristicConfigPolicy<Solution_> configPolicy, EntityDescriptor<Solution_> entityDescriptor,
            SelectionCacheType minimumCacheType, SelectionOrder inheritedSelectionOrder) {
        ValueSelectorConfig valueSelectorConfig =
                Objects.requireNonNullElseGet(config.getValueSelectorConfig(), ValueSelectorConfig::new);
        ValueSelector<Solution_> valueSelector = ValueSelectorFactory
                .<Solution_> create(valueSelectorConfig)
                .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, inheritedSelectionOrder);
        if (!valueSelector.getVariableDescriptor().isListVariable()) {
            throw new IllegalArgumentException("The subListSelector (" + config
                    + ") can only be used when the domain model has a list variable."
                    + " Check your @" + PlanningEntity.class.getSimpleName()
                    + " and make sure it has a @" + PlanningListVariable.class.getSimpleName() + ".");
        }
        if (!(valueSelector instanceof EntityIndependentValueSelector)) {
            throw new IllegalArgumentException("The subListSelector (" + config
                    + ") for a list variable needs to be based on an "
                    + EntityIndependentValueSelector.class.getSimpleName() + " (" + valueSelector + ")."
                    + " Check your @" + ValueRangeProvider.class.getSimpleName() + " annotations.");

        }
        return (EntityIndependentValueSelector<Solution_>) valueSelector;
    }
}
