package ai.timefold.solver.core.impl.exhaustivesearch;

import static ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType.STEP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchType;
import ai.timefold.solver.core.config.exhaustivesearch.NodeExplorationType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.exhaustivesearch.decider.ExhaustiveSearchDecider;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.ScoreBounder;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.TrendBasedScoreBounder;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.ManualEntityMimicRecorder;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;
import ai.timefold.solver.core.impl.move.MoveSelectorBasedMoveRepository;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

public class DefaultExhaustiveSearchPhaseFactory<Solution_>
        extends AbstractPhaseFactory<Solution_, ExhaustiveSearchPhaseConfig> {

    public DefaultExhaustiveSearchPhaseFactory(ExhaustiveSearchPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public ExhaustiveSearchPhase<Solution_> buildPhase(int phaseIndex, boolean lastInitializingPhase,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            SolverTermination<Solution_> solverTermination) {
        ExhaustiveSearchType exhaustiveSearchType_ = Objects.requireNonNullElse(
                phaseConfig.getExhaustiveSearchType(),
                ExhaustiveSearchType.BRANCH_AND_BOUND);
        EntitySorterManner entitySorterManner = Objects.requireNonNullElse(
                phaseConfig.getEntitySorterManner(),
                exhaustiveSearchType_.getDefaultEntitySorterManner());
        ValueSorterManner valueSorterManner = Objects.requireNonNullElse(
                phaseConfig.getValueSorterManner(),
                exhaustiveSearchType_.getDefaultValueSorterManner());
        HeuristicConfigPolicy<Solution_> phaseConfigPolicy = solverConfigPolicy.cloneBuilder()
                .withReinitializeVariableFilterEnabled(true)
                .withInitializedChainedValueFilterEnabled(true)
                .withEntitySorterManner(entitySorterManner)
                .withValueSorterManner(valueSorterManner)
                .build();
        PhaseTermination<Solution_> phaseTermination = buildPhaseTermination(phaseConfigPolicy, solverTermination);
        boolean scoreBounderEnabled = exhaustiveSearchType_.isScoreBounderEnabled();
        NodeExplorationType nodeExplorationType_;
        if (exhaustiveSearchType_ == ExhaustiveSearchType.BRUTE_FORCE) {
            nodeExplorationType_ = Objects.requireNonNullElse(phaseConfig.getNodeExplorationType(),
                    NodeExplorationType.ORIGINAL_ORDER);
            if (nodeExplorationType_ != NodeExplorationType.ORIGINAL_ORDER) {
                throw new IllegalArgumentException("The phaseConfig (" + phaseConfig
                        + ") has an nodeExplorationType (" + phaseConfig.getNodeExplorationType()
                        + ") which is not compatible with its exhaustiveSearchType (" + phaseConfig.getExhaustiveSearchType()
                        + ").");
            }
        } else {
            nodeExplorationType_ = Objects.requireNonNullElse(phaseConfig.getNodeExplorationType(),
                    NodeExplorationType.DEPTH_FIRST);
        }
        EntitySelectorConfig entitySelectorConfig_ = buildEntitySelectorConfig(phaseConfigPolicy);
        EntitySelector<Solution_> entitySelector =
                EntitySelectorFactory.<Solution_> create(entitySelectorConfig_)
                        .buildEntitySelector(phaseConfigPolicy, SelectionCacheType.PHASE, SelectionOrder.ORIGINAL);

        return new DefaultExhaustiveSearchPhase.Builder<>(phaseIndex,
                solverConfigPolicy.getLogIndentation(), phaseTermination,
                nodeExplorationType_.buildNodeComparator(scoreBounderEnabled), entitySelector, buildDecider(phaseConfigPolicy,
                        entitySelector, bestSolutionRecaller, phaseTermination, scoreBounderEnabled))
                .enableAssertions(phaseConfigPolicy.getEnvironmentMode())
                .build();
    }

    private EntitySelectorConfig buildEntitySelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        var result = Objects.requireNonNullElseGet(
                phaseConfig.getEntitySelectorConfig(),
                () -> {
                    var entityDescriptor = deduceEntityDescriptor(configPolicy.getSolutionDescriptor());
                    var entitySelectorConfig = new EntitySelectorConfig()
                            .withEntityClass(entityDescriptor.getEntityClass());
                    if (EntitySelectorConfig.hasSorter(configPolicy.getEntitySorterManner(), entityDescriptor)) {
                        entitySelectorConfig = entitySelectorConfig.withCacheType(SelectionCacheType.PHASE)
                                .withSelectionOrder(SelectionOrder.SORTED)
                                .withSorterManner(configPolicy.getEntitySorterManner());
                    }
                    return entitySelectorConfig;
                });
        var cacheType = result.getCacheType();
        if (cacheType != null && cacheType.compareTo(SelectionCacheType.PHASE) < 0) {
            throw new IllegalArgumentException(
                    "The phaseConfig (%s) cannot have an entitySelectorConfig (%s) with a cacheType (%s) lower than %s."
                            .formatted(phaseConfig, result, cacheType, SelectionCacheType.PHASE));
        }
        return result;
    }

    protected EntityDescriptor<Solution_> deduceEntityDescriptor(SolutionDescriptor<Solution_> solutionDescriptor) {
        Collection<EntityDescriptor<Solution_>> entityDescriptors = solutionDescriptor.getGenuineEntityDescriptors();
        if (entityDescriptors.size() != 1) {
            throw new IllegalArgumentException("The phaseConfig (" + phaseConfig
                    + ") has no entitySelector configured"
                    + " and because there are multiple in the entityClassSet (" + solutionDescriptor.getEntityClassSet()
                    + "), it cannot be deduced automatically.");
        }
        return entityDescriptors.iterator().next();
    }

    private ExhaustiveSearchDecider<Solution_> buildDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            EntitySelector<Solution_> sourceEntitySelector, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            PhaseTermination<Solution_> termination, boolean scoreBounderEnabled) {
        ManualEntityMimicRecorder<Solution_> manualEntityMimicRecorder =
                new ManualEntityMimicRecorder<>(sourceEntitySelector);
        String mimicSelectorId = sourceEntitySelector.getEntityDescriptor().getEntityClass().getName(); // TODO mimicSelectorId must be a field
        configPolicy.addEntityMimicRecorder(mimicSelectorId, manualEntityMimicRecorder);
        MoveSelectorConfig<?> moveSelectorConfig_ = buildMoveSelectorConfig(configPolicy,
                sourceEntitySelector, mimicSelectorId);
        MoveSelector<Solution_> moveSelector = MoveSelectorFactory.<Solution_> create(moveSelectorConfig_)
                .buildMoveSelector(configPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL, false);
        ScoreBounder scoreBounder = scoreBounderEnabled
                ? new TrendBasedScoreBounder(configPolicy.getScoreDefinition(), configPolicy.getInitializingScoreTrend())
                : null;
        ExhaustiveSearchDecider<Solution_> decider = new ExhaustiveSearchDecider<>(configPolicy.getLogIndentation(),
                bestSolutionRecaller, termination, manualEntityMimicRecorder,
                new MoveSelectorBasedMoveRepository<>(moveSelector), scoreBounderEnabled, scoreBounder);
        EnvironmentMode environmentMode = configPolicy.getEnvironmentMode();
        if (environmentMode.isFullyAsserted()) {
            decider.setAssertMoveScoreFromScratch(true);
        }
        if (environmentMode.isIntrusivelyAsserted()) {
            decider.setAssertExpectedUndoMoveScore(true);
        }
        return decider;
    }

    private MoveSelectorConfig<?> buildMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy,
            EntitySelector<Solution_> entitySelector, String mimicSelectorId) {
        MoveSelectorConfig<?> moveSelectorConfig_;
        if (phaseConfig.getMoveSelectorConfig() == null) {
            EntityDescriptor<Solution_> entityDescriptor = entitySelector.getEntityDescriptor();
            // Keep in sync with DefaultExhaustiveSearchPhase.fillLayerList()
            // which includes all genuineVariableDescriptors
            List<GenuineVariableDescriptor<Solution_>> variableDescriptorList =
                    entityDescriptor.getGenuineVariableDescriptorList();
            if (entityDescriptor.hasAnyGenuineListVariables()) {
                throw new IllegalArgumentException(
                        "Exhaustive Search does not support list variables (" + variableDescriptorList + ").");
            }
            List<MoveSelectorConfig> subMoveSelectorConfigList = new ArrayList<>(variableDescriptorList.size());
            for (GenuineVariableDescriptor<Solution_> variableDescriptor : variableDescriptorList) {
                ChangeMoveSelectorConfig changeMoveSelectorConfig = new ChangeMoveSelectorConfig();
                changeMoveSelectorConfig.setEntitySelectorConfig(
                        EntitySelectorConfig.newMimicSelectorConfig(mimicSelectorId));
                ValueSelectorConfig changeValueSelectorConfig = new ValueSelectorConfig()
                        .withVariableName(variableDescriptor.getVariableName());
                if (ValueSelectorConfig.hasSorter(configPolicy.getValueSorterManner(), variableDescriptor)) {
                    changeValueSelectorConfig = changeValueSelectorConfig
                            .withCacheType(variableDescriptor.isValueRangeEntityIndependent() ? SelectionCacheType.PHASE : STEP)
                            .withSelectionOrder(SelectionOrder.SORTED)
                            .withSorterManner(configPolicy.getValueSorterManner());
                }
                changeMoveSelectorConfig.setValueSelectorConfig(changeValueSelectorConfig);
                subMoveSelectorConfigList.add(changeMoveSelectorConfig);
            }
            if (subMoveSelectorConfigList.size() > 1) {
                moveSelectorConfig_ = new CartesianProductMoveSelectorConfig(subMoveSelectorConfigList);
            } else {
                moveSelectorConfig_ = subMoveSelectorConfigList.get(0);
            }
        } else {
            moveSelectorConfig_ = phaseConfig.getMoveSelectorConfig();
            // TODO Fail fast if it does not include all genuineVariableDescriptors as expected by DefaultExhaustiveSearchPhase.fillLayerList()
        }
        return moveSelectorConfig_;
    }
}
