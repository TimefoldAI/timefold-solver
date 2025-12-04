package ai.timefold.solver.core.impl.localsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.UnionMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.UnionMoveSelectorFactory;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AcceptorFactory;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForagerFactory;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.neighborhood.MoveSelectorBasedMoveRepository;
import ai.timefold.solver.core.impl.neighborhood.NeighborhoodsBasedMoveRepository;
import ai.timefold.solver.core.impl.neighborhood.NeighborhoodsMoveSelector;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.NeighborhoodProvider;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhood;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodBuilder;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

public class DefaultLocalSearchPhaseFactory<Solution_> extends AbstractPhaseFactory<Solution_, LocalSearchPhaseConfig> {

    public DefaultLocalSearchPhaseFactory(LocalSearchPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public LocalSearchPhase<Solution_> buildPhase(int phaseIndex, boolean lastInitializingPhase,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            SolverTermination<Solution_> solverTermination) {
        var phaseConfigPolicy = solverConfigPolicy.createPhaseConfigPolicy();
        var phaseTermination = buildPhaseTermination(phaseConfigPolicy, solverTermination);
        var decider = buildDecider(phaseConfigPolicy, phaseTermination);
        return new DefaultLocalSearchPhase.Builder<>(phaseIndex, solverConfigPolicy.getLogIndentation(), phaseTermination,
                decider)
                .enableAssertions(phaseConfigPolicy.getEnvironmentMode())
                .build();
    }

    private LocalSearchDecider<Solution_> buildDecider(HeuristicConfigPolicy<Solution_> phaseConfigPolicy,
            PhaseTermination<Solution_> phaseTermination) {
        var neighborhoodProviderClass = phaseConfig.<Solution_> getNeighborhoodProviderClass();
        var neighborhoodsEnabled = neighborhoodProviderClass != null;
        if (neighborhoodsEnabled) {
            phaseConfigPolicy.ensurePreviewFeature(PreviewFeature.NEIGHBORHOODS);
        }
        var moveSelectorConfig = phaseConfig.getMoveSelectorConfig();
        var moveSelectorsEnabled = moveSelectorConfig != null;
        if (moveSelectorsEnabled) {
            if (!neighborhoodsEnabled) {
                return buildMoveSelectorBasedDecider(phaseConfigPolicy, phaseTermination);
            } else {
                return buildMixedDecider(phaseConfigPolicy, phaseTermination, neighborhoodProviderClass);
            }
        } else if (neighborhoodsEnabled) {
            return buildNeighborhoodsBasedDecider(phaseConfigPolicy, phaseTermination, neighborhoodProviderClass);
        } else { // The default branch; for now, it is move selectors.
            return buildMoveSelectorBasedDecider(phaseConfigPolicy, phaseTermination);
        }
    }

    private LocalSearchDecider<Solution_> buildMoveSelectorBasedDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            PhaseTermination<Solution_> termination) {
        var moveRepository = new MoveSelectorBasedMoveRepository<>(buildMoveSelector(configPolicy));
        return buildDecider(moveRepository, configPolicy, termination);
    }

    private LocalSearchDecider<Solution_> buildNeighborhoodsBasedDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            PhaseTermination<Solution_> termination,
            Class<? extends NeighborhoodProvider<Solution_>> neighborhoodProviderClass) {
        return buildDecider(buildNeighborhoodsBasedMoveRepository(configPolicy, termination, neighborhoodProviderClass),
                configPolicy, termination);
    }

    private NeighborhoodsBasedMoveRepository<Solution_> buildNeighborhoodsBasedMoveRepository(
            HeuristicConfigPolicy<Solution_> configPolicy,
            PhaseTermination<Solution_> termination,
            Class<? extends NeighborhoodProvider<Solution_>> neighborhoodProviderClass) {
        var solutionDescriptor = configPolicy.getSolutionDescriptor();
        var solutionMetaModel = solutionDescriptor.getMetaModel();
        if (solutionMetaModel.genuineEntities().size() > 1) {
            throw new UnsupportedOperationException(
                    "Neighborhoods API currently only supports solutions with a single entity class, not multiple.");
        }
        var entityMetaModel = solutionMetaModel.genuineEntities().get(0);
        if (entityMetaModel.genuineVariables().size() > 1) {
            throw new UnsupportedOperationException(
                    "Neighborhoods API currently only supports solutions with a single variable class, not multiple.");
        }

        if (!NeighborhoodProvider.class.isAssignableFrom(neighborhoodProviderClass)) {
            throw new IllegalArgumentException(
                    "The neighborhoodProviderClass (%s) does not implement %s."
                            .formatted(neighborhoodProviderClass, NeighborhoodProvider.class.getSimpleName()));
        }
        var neighborhoodProvider =
                ConfigUtils.newInstance(LocalSearchPhaseConfig.class::getSimpleName, "neighborhoodProviderClass",
                        neighborhoodProviderClass);
        var neighborhoodBuilder = new DefaultNeighborhoodBuilder<>(solutionMetaModel);
        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, configPolicy.getEnvironmentMode());
        var moveDefinitionList = ((DefaultNeighborhood<Solution_>) neighborhoodProvider.defineNeighborhood(neighborhoodBuilder))
                .getMoveDefinitionList();
        return new NeighborhoodsBasedMoveRepository<>(moveStreamFactory, moveDefinitionList,
                pickSelectionOrder() == SelectionOrder.RANDOM);
    }

    private LocalSearchDecider<Solution_> buildMixedDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            PhaseTermination<Solution_> termination,
            Class<? extends NeighborhoodProvider<Solution_>> neighborhoodProviderClass) {
        var neighborhoodsMoveSelector = new NeighborhoodsMoveSelector<>(
                buildNeighborhoodsBasedMoveRepository(configPolicy, termination, neighborhoodProviderClass));
        var legacyMoveSelector = buildMoveSelector(configPolicy);
        if (legacyMoveSelector instanceof UnionMoveSelector<Solution_> unionMoveSelector) {
            if (unionMoveSelector.getSelectorProbabilityWeightFactory() != null) {
                throw new UnsupportedOperationException(
                        "Probability-weighted move selectors are not supported together with the Neighborhoods API.");
            } else {
                // We do not need to worry about probabilities, and therefore we just crack the union open
                // and create a new union including the neighborhoods.
                var moveSelectorList = new ArrayList<>(unionMoveSelector.getChildMoveSelectorList());
                moveSelectorList.add(neighborhoodsMoveSelector);
                var finalMoveSelector =
                        new UnionMoveSelector<>(moveSelectorList, pickSelectionOrder() == SelectionOrder.RANDOM);
                var moveRepository = new MoveSelectorBasedMoveRepository<>(finalMoveSelector);
                return buildDecider(moveRepository, configPolicy, termination);
            }
        } else {
            var unionMoveSelector = new UnionMoveSelector<>(List.of(neighborhoodsMoveSelector, legacyMoveSelector),
                    pickSelectionOrder() == SelectionOrder.RANDOM);
            var moveRepository = new MoveSelectorBasedMoveRepository<>(unionMoveSelector);
            return buildDecider(moveRepository, configPolicy, termination);
        }
    }

    private LocalSearchDecider<Solution_> buildDecider(MoveRepository<Solution_> moveRepository,
            HeuristicConfigPolicy<Solution_> configPolicy, PhaseTermination<Solution_> termination) {
        var acceptor = buildAcceptor(configPolicy, moveRepository instanceof NeighborhoodsBasedMoveRepository<Solution_>);
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

    protected Acceptor<Solution_> buildAcceptor(HeuristicConfigPolicy<Solution_> configPolicy, boolean neighborhoodsEnabled) {
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
            if (neighborhoodsEnabled && localSearchType_ == LocalSearchType.VARIABLE_NEIGHBORHOOD_DESCENT) {
                // Maybe works, but never tested.
                throw new UnsupportedOperationException(
                        "Variable Neighborhood descent is not yet supported with the Neighborhoods API.");
            }
            var acceptorType = switch (localSearchType_) {
                case HILL_CLIMBING, VARIABLE_NEIGHBORHOOD_DESCENT -> AcceptorType.HILL_CLIMBING;
                case TABU_SEARCH -> AcceptorType.ENTITY_TABU;
                case SIMULATED_ANNEALING -> AcceptorType.SIMULATED_ANNEALING;
                case LATE_ACCEPTANCE -> AcceptorType.LATE_ACCEPTANCE;
                case DIVERSIFIED_LATE_ACCEPTANCE -> AcceptorType.DIVERSIFIED_LATE_ACCEPTANCE;
                case GREAT_DELUGE -> AcceptorType.GREAT_DELUGE;
            };
            if (neighborhoodsEnabled && acceptorType.isTabu()) {
                throw new UnsupportedOperationException("Tabu search is not yet supported with the Neighborhoods API.");
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
