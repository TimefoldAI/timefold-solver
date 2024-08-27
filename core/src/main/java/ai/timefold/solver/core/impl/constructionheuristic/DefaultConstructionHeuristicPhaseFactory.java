package ai.timefold.solver.core.impl.constructionheuristic;

import java.util.Objects;
import java.util.Optional;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.constructionheuristic.decider.forager.ConstructionHeuristicForagerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.EntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.PooledEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase.DefaultConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.decider.forager.ConstructionHeuristicForager;
import ai.timefold.solver.core.impl.constructionheuristic.decider.forager.ConstructionHeuristicForagerFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacerFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.PooledEntityPlacerFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedEntityPlacerFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedValuePlacerFactory;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public class DefaultConstructionHeuristicPhaseFactory<Solution_>
        extends AbstractPhaseFactory<Solution_, ConstructionHeuristicPhaseConfig> {

    public DefaultConstructionHeuristicPhaseFactory(ConstructionHeuristicPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    public final DefaultConstructionHeuristicPhaseBuilder<Solution_> getBuilder(int phaseIndex,
            boolean triggerFirstInitializedSolutionEvent, HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            Termination<Solution_> solverTermination) {
        var constructionHeuristicType_ = Objects.requireNonNullElse(phaseConfig.getConstructionHeuristicType(),
                ConstructionHeuristicType.ALLOCATE_ENTITY_FROM_QUEUE);
        var entitySorterManner = Objects.requireNonNullElse(phaseConfig.getEntitySorterManner(),
                constructionHeuristicType_.getDefaultEntitySorterManner());
        var valueSorterManner = Objects.requireNonNullElse(phaseConfig.getValueSorterManner(),
                constructionHeuristicType_.getDefaultValueSorterManner());
        var phaseConfigPolicy = solverConfigPolicy.cloneBuilder()
                .withReinitializeVariableFilterEnabled(true)
                .withInitializedChainedValueFilterEnabled(true)
                .withUnassignedValuesAllowed(true)
                .withEntitySorterManner(entitySorterManner)
                .withValueSorterManner(valueSorterManner)
                .build();
        var entityPlacerConfig_ = getValidEntityPlacerConfig()
                .orElseGet(() -> buildDefaultEntityPlacerConfig(phaseConfigPolicy, constructionHeuristicType_));
        var entityPlacer = EntityPlacerFactory.<Solution_> create(entityPlacerConfig_)
                .buildEntityPlacer(phaseConfigPolicy);
        return createBuilder(phaseConfigPolicy, solverTermination, phaseIndex, triggerFirstInitializedSolutionEvent,
                entityPlacer);
    }

    protected DefaultConstructionHeuristicPhaseBuilder<Solution_> createBuilder(
            HeuristicConfigPolicy<Solution_> phaseConfigPolicy, Termination<Solution_> solverTermination, int phaseIndex,
            boolean triggerFirstInitializedSolutionEvent, EntityPlacer<Solution_> entityPlacer) {
        var phaseTermination = buildPhaseTermination(phaseConfigPolicy, solverTermination);
        var builder = new DefaultConstructionHeuristicPhaseBuilder<>(phaseIndex, triggerFirstInitializedSolutionEvent,
                phaseConfigPolicy.getLogIndentation(), phaseTermination, entityPlacer,
                buildDecider(phaseConfigPolicy, phaseTermination));
        var environmentMode = phaseConfigPolicy.getEnvironmentMode();
        if (environmentMode.isNonIntrusiveFullAsserted()) {
            builder.setAssertStepScoreFromScratch(true);
        }
        if (environmentMode.isIntrusiveFastAsserted()) {
            builder.setAssertExpectedStepScore(true);
            builder.setAssertShadowVariablesAreNotStaleAfterStep(true);
        }
        return builder;
    }

    @Override
    public ConstructionHeuristicPhase<Solution_> buildPhase(int phaseIndex, boolean triggerFirstInitializedSolutionEvent,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            Termination<Solution_> solverTermination) {
        return getBuilder(phaseIndex, triggerFirstInitializedSolutionEvent, solverConfigPolicy, solverTermination)
                .build();
    }

    private Optional<EntityPlacerConfig<?>> getValidEntityPlacerConfig() {
        EntityPlacerConfig<?> entityPlacerConfig = phaseConfig.getEntityPlacerConfig();
        if (entityPlacerConfig == null) {
            return Optional.empty();
        }
        if (phaseConfig.getConstructionHeuristicType() != null) {
            throw new IllegalArgumentException(
                    "The constructionHeuristicType (" + phaseConfig.getConstructionHeuristicType()
                            + ") must not be configured if the entityPlacerConfig (" + entityPlacerConfig
                            + ") is explicitly configured.");
        }
        if (phaseConfig.getMoveSelectorConfigList() != null) {
            throw new IllegalArgumentException("The moveSelectorConfigList (" + phaseConfig.getMoveSelectorConfigList()
                    + ") cannot be configured if the entityPlacerConfig (" + entityPlacerConfig
                    + ") is explicitly configured.");
        }
        return Optional.of(entityPlacerConfig);
    }

    private EntityPlacerConfig<?> buildDefaultEntityPlacerConfig(HeuristicConfigPolicy<Solution_> configPolicy,
            ConstructionHeuristicType constructionHeuristicType) {
        return findValidListVariableDescriptor(configPolicy.getSolutionDescriptor())
                .map(listVariableDescriptor -> buildListVariableQueuedValuePlacerConfig(configPolicy, listVariableDescriptor))
                .orElseGet(() -> buildUnfoldedEntityPlacerConfig(configPolicy, constructionHeuristicType));
    }

    private Optional<ListVariableDescriptor<?>>
            findValidListVariableDescriptor(SolutionDescriptor<Solution_> solutionDescriptor) {
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor == null) {
            return Optional.empty();
        }
        failIfConfigured(phaseConfig.getConstructionHeuristicType(), "constructionHeuristicType");
        failIfConfigured(phaseConfig.getEntityPlacerConfig(), "entityPlacerConfig");
        failIfConfigured(phaseConfig.getMoveSelectorConfigList(), "moveSelectorConfigList");
        return Optional.of(listVariableDescriptor);
    }

    private static void failIfConfigured(Object configValue, String configName) {
        if (configValue != null) {
            throw new IllegalArgumentException("Construction Heuristic phase with a list variable does not support "
                    + configName + " configuration. Remove the " + configName + " (" + configValue + ") from the config.");
        }
    }

    @SuppressWarnings("rawtypes")
    public static EntityPlacerConfig buildListVariableQueuedValuePlacerConfig(HeuristicConfigPolicy<?> configPolicy,
            ListVariableDescriptor<?> variableDescriptor) {
        var mimicSelectorId = variableDescriptor.getVariableName();

        // Prepare recording ValueSelector config.
        var mimicRecordingValueSelectorConfig = new ValueSelectorConfig(variableDescriptor.getVariableName())
                .withId(mimicSelectorId);
        if (ValueSelectorConfig.hasSorter(configPolicy.getValueSorterManner(), variableDescriptor)) {
            mimicRecordingValueSelectorConfig = mimicRecordingValueSelectorConfig.withCacheType(SelectionCacheType.PHASE)
                    .withSelectionOrder(SelectionOrder.SORTED)
                    .withSorterManner(configPolicy.getValueSorterManner());
        }
        // Prepare replaying ValueSelector config.
        var mimicReplayingValueSelectorConfig = new ValueSelectorConfig()
                .withMimicSelectorRef(mimicSelectorId);

        // ListChangeMoveSelector uses the replaying ValueSelector.
        var listChangeMoveSelectorConfig = new ListChangeMoveSelectorConfig()
                .withValueSelectorConfig(mimicReplayingValueSelectorConfig);

        // Finally, QueuedValuePlacer uses the recording ValueSelector and a ListChangeMoveSelector.
        // The ListChangeMoveSelector's replaying ValueSelector mimics the QueuedValuePlacer's recording ValueSelector.
        return new QueuedValuePlacerConfig()
                .withValueSelectorConfig(mimicRecordingValueSelectorConfig)
                .withMoveSelectorConfig(listChangeMoveSelectorConfig);
    }

    protected ConstructionHeuristicDecider<Solution_> buildDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            Termination<Solution_> termination) {
        var forager = buildForager(configPolicy);
        var moveThreadCount = configPolicy.getMoveThreadCount();
        var decider = (moveThreadCount == null)
                ? new ConstructionHeuristicDecider<>(configPolicy.getLogIndentation(), termination, forager)
                : TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.MULTITHREADED_SOLVING)
                        .buildConstructionHeuristic(termination, forager, configPolicy);
        decider.enableAssertions(configPolicy.getEnvironmentMode());
        return decider;
    }

    protected ConstructionHeuristicForager<Solution_> buildForager(HeuristicConfigPolicy<Solution_> configPolicy) {
        var foragerConfig_ =
                Objects.requireNonNullElseGet(phaseConfig.getForagerConfig(), ConstructionHeuristicForagerConfig::new);
        return ConstructionHeuristicForagerFactory.<Solution_> create(foragerConfig_).buildForager(configPolicy);
    }

    private EntityPlacerConfig<?> buildUnfoldedEntityPlacerConfig(HeuristicConfigPolicy<Solution_> phaseConfigPolicy,
            ConstructionHeuristicType constructionHeuristicType) {
        return switch (constructionHeuristicType) {
            case FIRST_FIT, FIRST_FIT_DECREASING, WEAKEST_FIT, WEAKEST_FIT_DECREASING, STRONGEST_FIT, STRONGEST_FIT_DECREASING,
                    ALLOCATE_ENTITY_FROM_QUEUE -> {
                if (!ConfigUtils.isEmptyCollection(phaseConfig.getMoveSelectorConfigList())) {
                    yield QueuedEntityPlacerFactory.unfoldNew(phaseConfigPolicy, phaseConfig.getMoveSelectorConfigList());
                }
                yield new QueuedEntityPlacerConfig();
            }
            case ALLOCATE_TO_VALUE_FROM_QUEUE -> {
                if (!ConfigUtils.isEmptyCollection(phaseConfig.getMoveSelectorConfigList())) {
                    yield QueuedValuePlacerFactory.unfoldNew(checkSingleMoveSelectorConfig());
                }
                yield new QueuedValuePlacerConfig();
            }
            case CHEAPEST_INSERTION, ALLOCATE_FROM_POOL -> {
                if (!ConfigUtils.isEmptyCollection(phaseConfig.getMoveSelectorConfigList())) {
                    yield PooledEntityPlacerFactory.unfoldNew(phaseConfigPolicy, checkSingleMoveSelectorConfig());
                }
                yield new PooledEntityPlacerConfig();
            }
        };
    }

    private MoveSelectorConfig<?> checkSingleMoveSelectorConfig() {
        if (phaseConfig.getMoveSelectorConfigList().size() != 1) {
            throw new IllegalArgumentException("For the constructionHeuristicType ("
                    + phaseConfig.getConstructionHeuristicType() + "), the moveSelectorConfigList ("
                    + phaseConfig.getMoveSelectorConfigList()
                    + ") must be a singleton. Use a single " + UnionMoveSelectorConfig.class.getSimpleName()
                    + " or " + CartesianProductMoveSelectorConfig.class.getSimpleName()
                    + " element to nest multiple MoveSelectors.");
        }

        return phaseConfig.getMoveSelectorConfigList().get(0);
    }
}
