package ai.timefold.solver.core.impl.localsearch;

import java.util.Collections;
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
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
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
        return new DefaultLocalSearchPhase.Builder<>(phaseIndex, solverConfigPolicy.getLogIndentation(),
                phaseTermination, buildDecider(phaseConfigPolicy, phaseTermination))
                .enableAssertions(phaseConfigPolicy.getEnvironmentMode())
                .build();
    }

    private LocalSearchDecider<Solution_> buildDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            PhaseTermination<Solution_> termination) {
        var moveSelector = buildMoveSelector(configPolicy);
        var acceptor = buildAcceptor(configPolicy);
        var forager = buildForager(configPolicy);
        if (moveSelector.isNeverEnding() && !forager.supportsNeverEndingMoveSelector()) {
            throw new IllegalStateException("The moveSelector (" + moveSelector
                    + ") has neverEnding (" + moveSelector.isNeverEnding()
                    + "), but the forager (" + forager
                    + ") does not support it.\n"
                    + "Maybe configure the <forager> with an <acceptedCountLimit>.");
        }
        var moveThreadCount = configPolicy.getMoveThreadCount();
        var environmentMode = configPolicy.getEnvironmentMode();
        LocalSearchDecider<Solution_> decider;
        if (moveThreadCount == null) {
            decider = new LocalSearchDecider<>(configPolicy.getLogIndentation(), termination, moveSelector, acceptor, forager);
        } else {
            decider = TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.MULTITHREADED_SOLVING)
                    .buildLocalSearch(moveThreadCount, termination, moveSelector, acceptor, forager, environmentMode,
                            configPolicy);
        }
        decider.enableAssertions(environmentMode);
        return decider;
    }

    protected Acceptor<Solution_> buildAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
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
            var acceptorType = switch (localSearchType_) {
                case HILL_CLIMBING, VARIABLE_NEIGHBORHOOD_DESCENT -> AcceptorType.HILL_CLIMBING;
                case TABU_SEARCH -> AcceptorType.ENTITY_TABU;
                case SIMULATED_ANNEALING -> AcceptorType.SIMULATED_ANNEALING;
                case LATE_ACCEPTANCE -> AcceptorType.LATE_ACCEPTANCE;
                case DIVERSIFIED_LATE_ACCEPTANCE -> AcceptorType.DIVERSIFIED_LATE_ACCEPTANCE;
                case GREAT_DELUGE -> AcceptorType.GREAT_DELUGE;
            };
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
                throw new IllegalArgumentException("The localSearchType (" + phaseConfig.getLocalSearchType()
                        + ") must not be configured if the foragerConfig (" + phaseConfig.getForagerConfig()
                        + ") is explicitly configured.");
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
                    throw new IllegalStateException("The localSearchType (" + localSearchType_
                            + ") is not implemented.");
            }
        }
        return LocalSearchForagerFactory.<Solution_> create(foragerConfig_).buildForager();
    }

    @SuppressWarnings("rawtypes")
    protected MoveSelector<Solution_> buildMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy) {
        MoveSelector<Solution_> moveSelector;
        var defaultCacheType = SelectionCacheType.JUST_IN_TIME;
        SelectionOrder defaultSelectionOrder;
        if (phaseConfig.getLocalSearchType() == LocalSearchType.VARIABLE_NEIGHBORHOOD_DESCENT) {
            defaultSelectionOrder = SelectionOrder.ORIGINAL;
        } else {
            defaultSelectionOrder = SelectionOrder.RANDOM;
        }
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

    private UnionMoveSelectorConfig determineDefaultMoveSelectorConfig(HeuristicConfigPolicy<Solution_> configPolicy) {
        var solutionDescriptor = configPolicy.getSolutionDescriptor();
        var basicVariableDescriptorList = solutionDescriptor.getEntityDescriptors().stream()
                .flatMap(entityDescriptor -> entityDescriptor.getGenuineVariableDescriptorList().stream())
                .filter(variableDescriptor -> !variableDescriptor.isListVariable())
                .distinct()
                .toList();
        var hasChainedVariable = basicVariableDescriptorList.stream()
                .filter(v -> v instanceof BasicVariableDescriptor<Solution_>)
                .anyMatch(v -> ((BasicVariableDescriptor<?>) v).isChained());
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (basicVariableDescriptorList.isEmpty()) { // We only have the one list variable.
            return new UnionMoveSelectorConfig()
                    .withMoveSelectors(new ListChangeMoveSelectorConfig(), new ListSwapMoveSelectorConfig(),
                            new KOptListMoveSelectorConfig());
        } else if (listVariableDescriptor == null) { // We only have basic variables.
            if (hasChainedVariable && basicVariableDescriptorList.size() == 1) {
                return new UnionMoveSelectorConfig()
                        .withMoveSelectors(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig(),
                                new TailChainSwapMoveSelectorConfig());
            } else {
                return new UnionMoveSelectorConfig()
                        .withMoveSelectors(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig());
            }
        } else {
            /*
             * We have a mix of basic and list variables.
             * The "old" move selector configs handle this situation correctly but they complain of it being deprecated.
             *
             * Combining essential variables with list variables in a single entity is not supported. Therefore,
             * default selectors do not support enabling Nearby Selection with multiple entities.
             *
             * TODO Improve so that list variables get list variable selectors directly.
             * TODO PLANNER-2755 Support coexistence of basic and list variables on the same entity.
             */
            if (configPolicy.getNearbyDistanceMeterClass() != null) {
                throw new IllegalArgumentException(
                        """
                                The configuration contains both basic and list variables, which makes it incompatible with using a top-level nearbyDistanceMeterClass (%s).
                                Specify move selectors manually or remove the top-level nearbyDistanceMeterClass from your solver config."""
                                .formatted(configPolicy.getNearbyDistanceMeterClass()));
            }
            return new UnionMoveSelectorConfig()
                    .withMoveSelectors(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig());
        }
    }
}
