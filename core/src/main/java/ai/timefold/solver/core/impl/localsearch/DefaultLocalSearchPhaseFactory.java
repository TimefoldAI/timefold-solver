package ai.timefold.solver.core.impl.localsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.AcceptorType;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchPickEarlyType;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.UnionMoveSelectorFactory;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AcceptorFactory;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForagerFactory;
import ai.timefold.solver.core.impl.move.MoveRepository;
import ai.timefold.solver.core.impl.move.MoveSelectorBasedMoveRepository;
import ai.timefold.solver.core.impl.move.MoveStreamsBasedMoveRepository;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProviders;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

public class DefaultLocalSearchPhaseFactory<Solution_> extends AbstractPhaseFactory<Solution_, LocalSearchPhaseConfig> {

    public DefaultLocalSearchPhaseFactory(LocalSearchPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public LocalSearchPhase<Solution_> buildPhase(int phaseIndex, boolean lastInitializingPhase,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            SolverTermination<Solution_> solverTermination) {
        var moveProviderClass = phaseConfig.<Solution_> getMoveProvidersClass();
        var moveStreamsEnabled = moveProviderClass != null;
        var moveSelectorConfig = phaseConfig.getMoveSelectorConfig();
        var moveSelectorsEnabled = moveSelectorConfig != null;
        if (moveSelectorsEnabled && moveStreamsEnabled) {
            throw new UnsupportedOperationException("""
                    The solver configuration enabled both move selectors and Move Streams.
                    These are mutually exclusive features, please pick one or the other.""");
        }
        var phaseConfigPolicy = solverConfigPolicy.createPhaseConfigPolicy();
        var phaseTermination = buildPhaseTermination(phaseConfigPolicy, solverTermination);
        var decider = moveStreamsEnabled
                ? buildMoveStreamsBasedDecider(phaseConfigPolicy, phaseTermination, moveProviderClass)
                : buildMoveSelectorBasedDecider(phaseConfigPolicy, phaseTermination);
        return new DefaultLocalSearchPhase.Builder<>(phaseIndex, solverConfigPolicy.getLogIndentation(), phaseTermination,
                decider)
                .enableAssertions(phaseConfigPolicy.getEnvironmentMode())
                .build();
    }

    private LocalSearchDecider<Solution_> buildMoveSelectorBasedDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            PhaseTermination<Solution_> termination) {
        var moveRepository = new MoveSelectorBasedMoveRepository<>(buildMoveSelector(configPolicy));
        return buildDecider(moveRepository, configPolicy, termination);
    }

    private LocalSearchDecider<Solution_> buildMoveStreamsBasedDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            PhaseTermination<Solution_> termination, Class<? extends MoveProviders<Solution_>> moveProvidersClass) {
        configPolicy.ensurePreviewFeature(PreviewFeature.MOVE_STREAMS);

        var solutionDescriptor = configPolicy.getSolutionDescriptor();
        var solutionMetaModel = solutionDescriptor.getMetaModel();
        if (solutionMetaModel.genuineEntities().size() > 1) {
            throw new UnsupportedOperationException(
                    "Move Streams currently only support solutions with a single entity class, not multiple.");
        }
        var entityMetaModel = solutionMetaModel.genuineEntities().get(0);
        if (entityMetaModel.genuineVariables().size() > 1) {
            throw new UnsupportedOperationException(
                    "Move Streams currently only support solutions with a single variable class, not multiple.");
        }
        var variableMetaModel = entityMetaModel.genuineVariables().get(0);
        if (variableMetaModel instanceof PlanningVariableMetaModel<Solution_, ?, ?> planningVariableMetaModel
                && planningVariableMetaModel.isChained()) {
            throw new UnsupportedOperationException("""
                    Move Streams don't support solutions with chained variables.
                    Convert your model to use @%s instead."""
                    .formatted(PlanningListVariable.class.getSimpleName()));
        }
        var entitiesWithPinningFilters = solutionDescriptor.getEntityDescriptors().stream()
                .filter(EntityDescriptor::hasPinningFilter)
                .toList();
        if (!entitiesWithPinningFilters.isEmpty()) {
            throw new UnsupportedOperationException("""
                    %s is deprecated and Move Streams do not support it.
                    Convert your entities (%s) to use @%s instead."""
                    .formatted(PinningFilter.class.getSimpleName(), entitiesWithPinningFilters,
                            PlanningPin.class.getSimpleName()));
        }

        if (!MoveProviders.class.isAssignableFrom(moveProvidersClass)) {
            throw new IllegalArgumentException(
                    "The moveProvidersClass (%s) does not implement %s."
                            .formatted(moveProvidersClass, MoveProviders.class.getSimpleName()));
        }
        var moveProviders =
                ConfigUtils.newInstance(LocalSearchPhaseConfig.class::getSimpleName, "moveProvidersClass", moveProvidersClass);
        var moveProviderList = moveProviders.defineMoves(solutionMetaModel);
        if (moveProviderList.size() != 1) {
            throw new IllegalArgumentException(
                    "The moveProvidersClass (%s) must define exactly one MoveProvider, not %s."
                            .formatted(moveProvidersClass, moveProviderList.size()));
        }
        var moveProvider = moveProviderList.get(0);
        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor);
        var moveProducer = moveProvider.apply(moveStreamFactory);
        var moveRepository = new MoveStreamsBasedMoveRepository<>(moveStreamFactory, moveProducer,
                pickSelectionOrder() == SelectionOrder.RANDOM);

        return buildDecider(moveRepository, configPolicy, termination);
    }

    private LocalSearchDecider<Solution_> buildDecider(MoveRepository<Solution_> moveRepository,
            HeuristicConfigPolicy<Solution_> configPolicy, PhaseTermination<Solution_> termination) {
        var acceptor = buildAcceptor(configPolicy, moveRepository instanceof MoveStreamsBasedMoveRepository<Solution_>);
        var forager = buildForager(configPolicy);
        if (moveRepository.isNeverEnding() && !forager.supportsNeverEndingMoveSelector()) {
            throw new IllegalStateException("""
                    The move repository (%s) is neverEnding (%s), but the forager (%s) does not support it.
                    Maybe configure the <forager> with an <acceptedCountLimit>."""
                    .formatted(moveRepository, moveRepository.isNeverEnding(), forager));
        }
        var moveThreadCount = configPolicy.getMoveThreadCount();
        var environmentMode = configPolicy.getEnvironmentMode();
        var decider = moveThreadCount == null
                ? new LocalSearchDecider<>(configPolicy.getLogIndentation(), termination, moveRepository, acceptor, forager)
                : TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.MULTITHREADED_SOLVING)
                        .buildLocalSearch(moveThreadCount, termination, moveRepository, acceptor, forager, environmentMode,
                                configPolicy);
        decider.enableAssertions(environmentMode);
        return decider;
    }

    protected Acceptor<Solution_> buildAcceptor(HeuristicConfigPolicy<Solution_> configPolicy, boolean moveStreamsEnabled) {
        var acceptorConfig = phaseConfig.getAcceptorConfig();
        var localSearchType = phaseConfig.getLocalSearchType();
        if (acceptorConfig != null) {
            if (localSearchType != null) {
                throw new IllegalArgumentException(
                        "The localSearchType (%s) must not be configured if the acceptorConfig (%s) is explicitly configured."
                                .formatted(localSearchType, acceptorConfig));
            }
            return buildAcceptor(acceptorConfig, configPolicy);
        } else {
            var localSearchType_ = Objects.requireNonNullElse(localSearchType, LocalSearchType.LATE_ACCEPTANCE);
            var acceptorConfig_ = new LocalSearchAcceptorConfig();
            if (moveStreamsEnabled && localSearchType_ == LocalSearchType.VARIABLE_NEIGHBORHOOD_DESCENT) {
                // Maybe works, but never tested.
                throw new UnsupportedOperationException(
                        "Variable Neighborhood descent is not yet supported with Move Streams.");
            }
            var acceptorType = switch (localSearchType_) {
                case HILL_CLIMBING, VARIABLE_NEIGHBORHOOD_DESCENT -> AcceptorType.HILL_CLIMBING;
                case TABU_SEARCH -> AcceptorType.ENTITY_TABU;
                case SIMULATED_ANNEALING -> AcceptorType.SIMULATED_ANNEALING;
                case LATE_ACCEPTANCE -> AcceptorType.LATE_ACCEPTANCE;
                case DIVERSIFIED_LATE_ACCEPTANCE -> AcceptorType.DIVERSIFIED_LATE_ACCEPTANCE;
                case GREAT_DELUGE -> AcceptorType.GREAT_DELUGE;
            };
            if (moveStreamsEnabled && acceptorType.isTabu()) {
                throw new UnsupportedOperationException("Tabu search is not yet supported with Move Streams.");
            }
            acceptorConfig_.setAcceptorTypeList(Collections.singletonList(acceptorType));
            return buildAcceptor(acceptorConfig_, configPolicy);
        }
    }

    private Acceptor<Solution_> buildAcceptor(LocalSearchAcceptorConfig acceptorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy) {
        return AcceptorFactory.<Solution_> create(acceptorConfig)
                .buildAcceptor(configPolicy);
    }

    protected LocalSearchForager<Solution_> buildForager(HeuristicConfigPolicy<Solution_> configPolicy) {
        LocalSearchForagerConfig foragerConfig_;
        if (phaseConfig.getForagerConfig() != null) {
            if (phaseConfig.getLocalSearchType() != null) {
                throw new IllegalArgumentException(
                        "The localSearchType (%s) must not be configured if the foragerConfig (%s) is explicitly configured."
                                .formatted(phaseConfig.getLocalSearchType(), phaseConfig.getForagerConfig()));
            }
            foragerConfig_ = phaseConfig.getForagerConfig();
        } else {
            var localSearchType_ =
                    Objects.requireNonNullElse(phaseConfig.getLocalSearchType(), LocalSearchType.LATE_ACCEPTANCE);
            foragerConfig_ = new LocalSearchForagerConfig();
            switch (localSearchType_) {
                case HILL_CLIMBING:
                    foragerConfig_.setAcceptedCountLimit(1);
                    break;
                case TABU_SEARCH:
                    // Slow stepping algorithm
                    foragerConfig_.setAcceptedCountLimit(1000);
                    break;
                case SIMULATED_ANNEALING:
                case LATE_ACCEPTANCE:
                case DIVERSIFIED_LATE_ACCEPTANCE:
                case GREAT_DELUGE:
                    // Fast stepping algorithm
                    foragerConfig_.setAcceptedCountLimit(1);
                    break;
                case VARIABLE_NEIGHBORHOOD_DESCENT:
                    foragerConfig_.setPickEarlyType(LocalSearchPickEarlyType.FIRST_LAST_STEP_SCORE_IMPROVING);
                    break;
                default:
                    throw new IllegalStateException("The localSearchType (%s) is not implemented."
                            .formatted(localSearchType_));
            }
        }
        return LocalSearchForagerFactory.<Solution_> create(foragerConfig_).buildForager();
    }

    @SuppressWarnings("rawtypes")
    private MoveSelector<Solution_> buildMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy) {
        MoveSelector<Solution_> moveSelector;
        var defaultCacheType = SelectionCacheType.JUST_IN_TIME;
        var defaultSelectionOrder = pickSelectionOrder();

        var moveSelectorConfig = phaseConfig.getMoveSelectorConfig();
        if (moveSelectorConfig == null) {
            moveSelector = new UnionMoveSelectorFactory<Solution_>(determineDefaultMoveSelectorConfig(configPolicy))
                    .buildMoveSelector(configPolicy, defaultCacheType, defaultSelectionOrder, true);
        } else {
            AbstractMoveSelectorFactory<Solution_, ?> moveSelectorFactory = MoveSelectorFactory.create(moveSelectorConfig);

            if (configPolicy.getNearbyDistanceMeterClass() != null
                    && NearbyAutoConfigurationEnabled.class.isAssignableFrom(moveSelectorConfig.getClass())
                    && !UnionMoveSelectorConfig.class.isAssignableFrom(moveSelectorConfig.getClass())) {
                // The move selector config is not a composite selector, but it accepts Nearby autoconfiguration.
                // We create a new UnionMoveSelectorConfig with the existing selector to enable Nearby autoconfiguration.
                var moveSelectorCopy = (MoveSelectorConfig) moveSelectorConfig.copyConfig();
                var updatedConfig = new UnionMoveSelectorConfig()
                        .withMoveSelectors(moveSelectorCopy);
                moveSelectorFactory = MoveSelectorFactory.create(updatedConfig);
            }
            moveSelector = moveSelectorFactory.buildMoveSelector(configPolicy, defaultCacheType, defaultSelectionOrder, true);
        }
        return moveSelector;
    }

    private SelectionOrder pickSelectionOrder() {
        return phaseConfig.getLocalSearchType() == LocalSearchType.VARIABLE_NEIGHBORHOOD_DESCENT ? SelectionOrder.ORIGINAL
                : SelectionOrder.RANDOM;
    }

    private UnionMoveSelectorConfig determineDefaultMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        var solutionDescriptor = configPolicy.getSolutionDescriptor();
        if (solutionDescriptor.hasBothBasicAndListVariables()) {
            var moveSelectorList = new ArrayList<MoveSelectorConfig>();
            // Specific basic variable moves
            moveSelectorList.addAll(List.of(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig()));
            // Specific list variable moves
            moveSelectorList.addAll(List.of(new ListChangeMoveSelectorConfig(), new ListSwapMoveSelectorConfig(),
                    new KOptListMoveSelectorConfig()));
            return new UnionMoveSelectorConfig().withMoveSelectorList(moveSelectorList);
        } else if (solutionDescriptor.hasListVariable()) {
            // We only have the one list variable.
            return new UnionMoveSelectorConfig().withMoveSelectors(new ListChangeMoveSelectorConfig(),
                    new ListSwapMoveSelectorConfig(), new KOptListMoveSelectorConfig());
        } else {
            // We only have basic variables.
            var basicVariableDescriptorList = solutionDescriptor.getBasicVariableDescriptorList();
            if (solutionDescriptor.hasChainedVariable() && basicVariableDescriptorList.size() == 1) {
                // if there is only one chained variable, we add TailChainSwapMoveSelectorConfig
                return new UnionMoveSelectorConfig()
                        .withMoveSelectors(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig(),
                                new TailChainSwapMoveSelectorConfig());
            } else {
                // Basic variables or a mixed model with basic and chained variables
                return new UnionMoveSelectorConfig().withMoveSelectors(new ChangeMoveSelectorConfig(),
                        new SwapMoveSelectorConfig());
            }
        }
    }
}
