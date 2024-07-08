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
import ai.timefold.solver.core.config.solver.EnvironmentMode;
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
import ai.timefold.solver.core.impl.solver.termination.Termination;

public class DefaultLocalSearchPhaseFactory<Solution_> extends AbstractPhaseFactory<Solution_, LocalSearchPhaseConfig> {

    public DefaultLocalSearchPhaseFactory(LocalSearchPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public LocalSearchPhase<Solution_> buildPhase(int phaseIndex, boolean triggerFirstInitializedSolutionEvent,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            Termination<Solution_> solverTermination) {
        HeuristicConfigPolicy<Solution_> phaseConfigPolicy = solverConfigPolicy.createPhaseConfigPolicy();
        Termination<Solution_> phaseTermination = buildPhaseTermination(phaseConfigPolicy, solverTermination);
        DefaultLocalSearchPhase.Builder<Solution_> builder =
                new DefaultLocalSearchPhase.Builder<>(phaseIndex, solverConfigPolicy.getLogIndentation(), phaseTermination,
                        buildDecider(phaseConfigPolicy, phaseTermination));
        EnvironmentMode environmentMode = phaseConfigPolicy.getEnvironmentMode();
        if (environmentMode.isNonIntrusiveFullAsserted()) {
            builder.setAssertStepScoreFromScratch(true);
        }
        if (environmentMode.isIntrusiveFastAsserted()) {
            builder.setAssertExpectedStepScore(true);
            builder.setAssertShadowVariablesAreNotStaleAfterStep(true);
        }
        return builder.build();
    }

    private LocalSearchDecider<Solution_> buildDecider(HeuristicConfigPolicy<Solution_> configPolicy,
            Termination<Solution_> termination) {
        MoveSelector<Solution_> moveSelector = buildMoveSelector(configPolicy);
        Acceptor<Solution_> acceptor = buildAcceptor(configPolicy);
        LocalSearchForager<Solution_> forager = buildForager(configPolicy);
        if (moveSelector.isNeverEnding() && !forager.supportsNeverEndingMoveSelector()) {
            throw new IllegalStateException("The moveSelector (" + moveSelector
                    + ") has neverEnding (" + moveSelector.isNeverEnding()
                    + "), but the forager (" + forager
                    + ") does not support it.\n"
                    + "Maybe configure the <forager> with an <acceptedCountLimit>.");
        }
        Integer moveThreadCount = configPolicy.getMoveThreadCount();
        EnvironmentMode environmentMode = configPolicy.getEnvironmentMode();
        LocalSearchDecider<Solution_> decider;
        if (moveThreadCount == null) {
            decider = new LocalSearchDecider<>(configPolicy.getLogIndentation(), termination, moveSelector, acceptor, forager);
        } else {
            decider = TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.MULTITHREADED_SOLVING)
                    .buildLocalSearch(moveThreadCount, termination, moveSelector, acceptor, forager, environmentMode,
                            configPolicy);
        }
        if (environmentMode.isNonIntrusiveFullAsserted()) {
            decider.setAssertMoveScoreFromScratch(true);
        }
        if (environmentMode.isIntrusiveFastAsserted()) {
            decider.setAssertExpectedUndoMoveScore(true);
        }
        return decider;
    }

    protected Acceptor<Solution_> buildAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        LocalSearchAcceptorConfig acceptorConfig_;
        if (phaseConfig.getAcceptorConfig() != null) {
            if (phaseConfig.getLocalSearchType() != null) {
                throw new IllegalArgumentException("The localSearchType (" + phaseConfig.getLocalSearchType()
                        + ") must not be configured if the acceptorConfig (" + phaseConfig.getAcceptorConfig()
                        + ") is explicitly configured.");
            }
            acceptorConfig_ = phaseConfig.getAcceptorConfig();
        } else {
            LocalSearchType localSearchType_ =
                    Objects.requireNonNullElse(phaseConfig.getLocalSearchType(), LocalSearchType.LATE_ACCEPTANCE);
            acceptorConfig_ = new LocalSearchAcceptorConfig();
            switch (localSearchType_) {
                case HILL_CLIMBING:
                case VARIABLE_NEIGHBORHOOD_DESCENT:
                    acceptorConfig_.setAcceptorTypeList(Collections.singletonList(AcceptorType.HILL_CLIMBING));
                    break;
                case TABU_SEARCH:
                    acceptorConfig_.setAcceptorTypeList(Collections.singletonList(AcceptorType.ENTITY_TABU));
                    break;
                case SIMULATED_ANNEALING:
                    acceptorConfig_.setAcceptorTypeList(Collections.singletonList(AcceptorType.SIMULATED_ANNEALING));
                    break;
                case LATE_ACCEPTANCE:
                    acceptorConfig_.setAcceptorTypeList(Collections.singletonList(AcceptorType.LATE_ACCEPTANCE));
                    break;
                case GREAT_DELUGE:
                    acceptorConfig_.setAcceptorTypeList(Collections.singletonList(AcceptorType.GREAT_DELUGE));
                    break;
                default:
                    throw new IllegalStateException("The localSearchType (" + localSearchType_
                            + ") is not implemented.");
            }
        }
        return AcceptorFactory.<Solution_> create(acceptorConfig_)
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
            LocalSearchType localSearchType_ =
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

    protected MoveSelector<Solution_> buildMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy) {
        MoveSelector<Solution_> moveSelector;
        SelectionCacheType defaultCacheType = SelectionCacheType.JUST_IN_TIME;
        SelectionOrder defaultSelectionOrder;
        if (phaseConfig.getLocalSearchType() == LocalSearchType.VARIABLE_NEIGHBORHOOD_DESCENT) {
            defaultSelectionOrder = SelectionOrder.ORIGINAL;
        } else {
            defaultSelectionOrder = SelectionOrder.RANDOM;
        }
        if (phaseConfig.getMoveSelectorConfig() == null) {
            moveSelector = new UnionMoveSelectorFactory<Solution_>(determineDefaultMoveSelectorConfig(configPolicy))
                    .buildMoveSelector(configPolicy, defaultCacheType, defaultSelectionOrder, true);
        } else {
            AbstractMoveSelectorFactory<Solution_, ?> moveSelectorFactory =
                    MoveSelectorFactory.create(phaseConfig.getMoveSelectorConfig());

            if (configPolicy.getNearbyDistanceMeterClass() != null
                    && NearbyAutoConfigurationEnabled.class
                            .isAssignableFrom(phaseConfig.getMoveSelectorConfig().getClass())
                    && !UnionMoveSelectorConfig.class.isAssignableFrom(phaseConfig.getMoveSelectorConfig().getClass())) {
                // The move selector config is not a composite selector, but it accepts Nearby autoconfiguration.
                // We create a new UnionMoveSelectorConfig with the existing selector to enable Nearby autoconfiguration.
                MoveSelectorConfig moveSelectorCopy = (MoveSelectorConfig) phaseConfig.getMoveSelectorConfig().copyConfig();
                UnionMoveSelectorConfig updatedConfig = new UnionMoveSelectorConfig()
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
