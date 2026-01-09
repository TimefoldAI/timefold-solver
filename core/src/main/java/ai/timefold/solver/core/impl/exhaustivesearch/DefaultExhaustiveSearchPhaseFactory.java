package ai.timefold.solver.core.impl.exhaustivesearch;

import static ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType.STEP;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchType;
import ai.timefold.solver.core.config.exhaustivesearch.NodeExplorationType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.exhaustivesearch.decider.AbstractExhaustiveSearchDecider;
import ai.timefold.solver.core.impl.exhaustivesearch.decider.BasicVariableExhaustiveSearchDecider;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.TrendBasedScoreBounder;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.ManualEntityMimicRecorder;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;
import ai.timefold.solver.core.impl.neighborhood.MoveSelectorBasedMoveRepository;
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
        var exhaustiveSearchType = Objects.requireNonNullElse(
                phaseConfig.getExhaustiveSearchType(),
                ExhaustiveSearchType.BRANCH_AND_BOUND);
        var entitySorterManner = Objects.requireNonNullElse(
                phaseConfig.getEntitySorterManner(),
                exhaustiveSearchType.getDefaultEntitySorterManner());
        var valueSorterManner = Objects.requireNonNullElse(
                phaseConfig.getValueSorterManner(),
                exhaustiveSearchType.getDefaultValueSorterManner());
        var phaseConfigPolicy = solverConfigPolicy.cloneBuilder()
                .withReinitializeVariableFilterEnabled(true)
                .withInitializedChainedValueFilterEnabled(true)
                .withEntitySorterManner(entitySorterManner)
                .withValueSorterManner(valueSorterManner)
                .build();
        var phaseTermination = buildPhaseTermination(phaseConfigPolicy, solverTermination);
        var scoreBounderEnabled = exhaustiveSearchType.isScoreBounderEnabled();
        var nodeExplorationType = getNodeExplorationType(exhaustiveSearchType, phaseConfig);
        var strategy =
                buildExhaustiveSearchStrategy(phaseConfigPolicy, scoreBounderEnabled, bestSolutionRecaller, phaseTermination);
        return new DefaultExhaustiveSearchPhase.Builder<>(phaseIndex, solverConfigPolicy.getLogIndentation(), phaseTermination,
                nodeExplorationType.buildNodeComparator(scoreBounderEnabled), strategy)
                .enableAssertions(phaseConfigPolicy.getEnvironmentMode())
                .build();
    }

    private static NodeExplorationType getNodeExplorationType(ExhaustiveSearchType exhaustiveSearchType,
            ExhaustiveSearchPhaseConfig phaseConfig) {
        NodeExplorationType nodeExplorationType;
        if (exhaustiveSearchType == ExhaustiveSearchType.BRUTE_FORCE) {
            nodeExplorationType = Objects.requireNonNullElse(phaseConfig.getNodeExplorationType(),
                    NodeExplorationType.ORIGINAL_ORDER);
            if (nodeExplorationType != NodeExplorationType.ORIGINAL_ORDER) {
                throw new IllegalArgumentException(
                        "The phaseConfig (%s) has an nodeExplorationType (%s) which is not compatible with its exhaustiveSearchType (%s)."
                                .formatted(phaseConfig, phaseConfig.getNodeExplorationType(),
                                        phaseConfig.getExhaustiveSearchType()));
            }
        } else {
            nodeExplorationType = Objects.requireNonNullElse(phaseConfig.getNodeExplorationType(),
                    NodeExplorationType.DEPTH_FIRST);
        }
        return nodeExplorationType;
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
        var entityDescriptors = solutionDescriptor.getGenuineEntityDescriptors();
        if (entityDescriptors.size() != 1) {
            throw new IllegalArgumentException(
                    "The phaseConfig (%s) has no entitySelector configured and because there are multiple in the entityClassSet (%s), it cannot be deduced automatically."
                            .formatted(phaseConfig, solutionDescriptor.getEntityClassSet()));
        }
        return entityDescriptors.iterator().next();
    }

    private ExhaustiveSearchStrategy<Solution_> buildExhaustiveSearchStrategy(
            HeuristicConfigPolicy<Solution_> phaseConfigPolicy, boolean scoreBounderEnabled,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, PhaseTermination<Solution_> phaseTermination) {
        if (phaseConfigPolicy.getSolutionDescriptor().hasBothBasicAndListVariables()) {
            throw new UnsupportedOperationException("Exhaustive Search does not support mixed models.");
        }
        var entitySelectorConfig = buildEntitySelectorConfig(phaseConfigPolicy);
        var entitySelector =
                EntitySelectorFactory.<Solution_> create(entitySelectorConfig)
                        .buildEntitySelector(phaseConfigPolicy, SelectionCacheType.PHASE, SelectionOrder.ORIGINAL);
        var decider =
                buildDecider(phaseConfigPolicy, entitySelector, bestSolutionRecaller, phaseTermination, scoreBounderEnabled);
        if (entitySelector.getEntityDescriptor().hasAnyGenuineListVariables()) {
            throw new UnsupportedOperationException();
        } else {
            return new BasicVariableExhaustiveSearchStrategy<>(entitySelector, decider);
        }
    }

    private AbstractExhaustiveSearchDecider<Solution_> buildDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            EntitySelector<Solution_> sourceEntitySelector, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            PhaseTermination<Solution_> termination, boolean scoreBounderEnabled) {
        AbstractExhaustiveSearchDecider<Solution_> decider;
        if (sourceEntitySelector.getEntityDescriptor().hasAnyGenuineListVariables()) {
            throw new UnsupportedOperationException();
        } else {
            decider = buildDeciderForBasicVariable(configPolicy, sourceEntitySelector, bestSolutionRecaller, termination,
                    scoreBounderEnabled);
        }
        EnvironmentMode environmentMode = configPolicy.getEnvironmentMode();
        if (environmentMode.isFullyAsserted()) {
            decider.setAssertMoveScoreFromScratch(true);
        }
        if (environmentMode.isIntrusivelyAsserted()) {
            decider.setAssertExpectedUndoMoveScore(true);
        }
        return decider;
    }

    private AbstractExhaustiveSearchDecider<Solution_> buildDeciderForBasicVariable(
            HeuristicConfigPolicy<Solution_> configPolicy,
            EntitySelector<Solution_> sourceEntitySelector, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            PhaseTermination<Solution_> termination, boolean scoreBounderEnabled) {
        var manualEntityMimicRecorder = new ManualEntityMimicRecorder<>(sourceEntitySelector);
        var mimicSelectorId = sourceEntitySelector.getEntityDescriptor().getEntityClass().getName(); // TODO mimicSelectorId must be a field
        configPolicy.addEntityMimicRecorder(mimicSelectorId, manualEntityMimicRecorder);
        var moveSelectorConfig = buildMoveSelectorConfig(configPolicy,
                sourceEntitySelector, mimicSelectorId);
        var moveSelector = MoveSelectorFactory.<Solution_> create(moveSelectorConfig)
                .buildMoveSelector(configPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.ORIGINAL, false);
        var scoreBounder = scoreBounderEnabled
                ? new TrendBasedScoreBounder<>(configPolicy.getScoreDefinition(), configPolicy.getInitializingScoreTrend())
                : null;
        return new BasicVariableExhaustiveSearchDecider<>(configPolicy.getLogIndentation(),
                bestSolutionRecaller, termination, manualEntityMimicRecorder,
                new MoveSelectorBasedMoveRepository<>(moveSelector), scoreBounderEnabled, scoreBounder);
    }

    private MoveSelectorConfig<?> buildMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy,
            EntitySelector<Solution_> entitySelector, String mimicSelectorId) {
        if (phaseConfig.getMoveSelectorConfig() == null) {
            var variableDescriptorList = getGenuineVariableDescriptorList(entitySelector);
            var hasListVariable = variableDescriptorList.stream().anyMatch(VariableDescriptor::isListVariable);
            if (hasListVariable) {
                return buildMoveSelectorConfigForListVariable(configPolicy,
                        (ListVariableDescriptor<Solution_>) variableDescriptorList.get(0));
            } else {
                return buildMoveSelectorConfigForBasicVariable(configPolicy, mimicSelectorId, variableDescriptorList);
            }
        } else {
            return phaseConfig.getMoveSelectorConfig();
            // TODO Fail fast if it does not include all genuineVariableDescriptors as expected by DefaultExhaustiveSearchPhase.fillLayerList()
        }
    }

    private MoveSelectorConfig<?> buildMoveSelectorConfigForBasicVariable(HeuristicConfigPolicy<Solution_> configPolicy,
            String mimicSelectorId, List<GenuineVariableDescriptor<Solution_>> variableDescriptorList) {
        var subMoveSelectorConfigList = new ArrayList<MoveSelectorConfig>(variableDescriptorList.size());
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : variableDescriptorList) {
            var changeMoveSelectorConfig = new ChangeMoveSelectorConfig();
            changeMoveSelectorConfig.setEntitySelectorConfig(
                    EntitySelectorConfig.newMimicSelectorConfig(mimicSelectorId));
            var changeValueSelectorConfig = new ValueSelectorConfig()
                    .withVariableName(variableDescriptor.getVariableName());
            if (ValueSelectorConfig.hasSorter(configPolicy.getValueSorterManner(), variableDescriptor)) {
                changeValueSelectorConfig = changeValueSelectorConfig
                        .withCacheType(
                                variableDescriptor.canExtractValueRangeFromSolution() ? SelectionCacheType.PHASE : STEP)
                        .withSelectionOrder(SelectionOrder.SORTED)
                        .withSorterManner(configPolicy.getValueSorterManner());
            }
            changeMoveSelectorConfig.setValueSelectorConfig(changeValueSelectorConfig);
            subMoveSelectorConfigList.add(changeMoveSelectorConfig);
        }
        if (subMoveSelectorConfigList.size() > 1) {
            return new CartesianProductMoveSelectorConfig(subMoveSelectorConfigList);
        } else {
            return subMoveSelectorConfigList.get(0);
        }
    }

    private MoveSelectorConfig<?> buildMoveSelectorConfigForListVariable(HeuristicConfigPolicy<Solution_> configPolicy,
            ListVariableDescriptor<Solution_> listVariableDescriptor) {
        var changeMoveSelectorConfig = new ListChangeMoveSelectorConfig();
        var changeValueSelectorConfig = new ValueSelectorConfig()
                .withVariableName(listVariableDescriptor.getVariableName());
        if (ValueSelectorConfig.hasSorter(configPolicy.getValueSorterManner(), listVariableDescriptor)) {
            changeValueSelectorConfig = changeValueSelectorConfig
                    .withCacheType(listVariableDescriptor.canExtractValueRangeFromSolution() ? SelectionCacheType.PHASE : STEP)
                    .withSelectionOrder(SelectionOrder.SORTED)
                    .withSorterManner(configPolicy.getValueSorterManner());
        }
        changeMoveSelectorConfig.setValueSelectorConfig(changeValueSelectorConfig);
        changeMoveSelectorConfig.withDestinationSelectorConfig(new DestinationSelectorConfig());
        return changeMoveSelectorConfig;
    }

    private static <Solution_> List<GenuineVariableDescriptor<Solution_>>
            getGenuineVariableDescriptorList(EntitySelector<Solution_> entitySelector) {
        var entityDescriptor = entitySelector.getEntityDescriptor();
        // Keep in sync with DefaultExhaustiveSearchPhase.fillLayerList()
        // which includes all genuineVariableDescriptors
        return entityDescriptor.getGenuineVariableDescriptorList();
    }
}
