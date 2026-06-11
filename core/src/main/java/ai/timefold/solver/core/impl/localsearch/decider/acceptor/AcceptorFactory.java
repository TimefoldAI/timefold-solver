package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.config.localsearch.decider.acceptor.AcceptorType;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.stepcountinghillclimbing.StepCountingHillClimbingType;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.greatdeluge.GreatDelugeAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.hillclimbing.HillClimbingAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance.DiversifiedLateAcceptanceAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance.LateAcceptanceAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.simulatedannealing.SimulatedAnnealingAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stepcountinghillclimbing.StepCountingHillClimbingAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.AbstractTabuAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.EntityTabuAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.MoveTabuAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.ValueTabuAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size.EntityRatioTabuSizeStrategy;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size.FixedTabuSizeStrategy;

public class AcceptorFactory<Solution_> {

    // Based on Tomas Muller's work. TODO Confirm with benchmark across our examples/datasets
    private static final double DEFAULT_WATER_LEVEL_INCREMENT_RATIO = 0.00_000_005;

    public static <Solution_> AcceptorFactory<Solution_> create(LocalSearchAcceptorConfig acceptorConfig) {
        return new AcceptorFactory<>(acceptorConfig);
    }

    private final LocalSearchAcceptorConfig acceptorConfig;

    public AcceptorFactory(LocalSearchAcceptorConfig acceptorConfig) {
        this.acceptorConfig = acceptorConfig;
    }

    public Acceptor<Solution_> buildAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        List<Acceptor<Solution_>> acceptorList = Stream.of(
                buildHillClimbingAcceptor(),
                buildStepCountingHillClimbingAcceptor(),
                buildEntityTabuAcceptor(configPolicy),
                buildValueTabuAcceptor(configPolicy),
                buildMoveTabuAcceptor(configPolicy),
                buildSimulatedAnnealingAcceptor(configPolicy),
                buildLateAcceptanceAcceptor(),
                buildDiversifiedLateAcceptanceAcceptor(configPolicy),
                buildGreatDelugeAcceptor(configPolicy))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (acceptorList.size() == 1) {
            return acceptorList.getFirst();
        } else if (acceptorList.size() > 1) {
            return new CompositeAcceptor<>(acceptorList);
        } else {
            throw new IllegalArgumentException("""
                    The acceptor does not specify any acceptorType (%s) or other acceptor property.
                    For good starting values, see the docs section "Which optimization algorithms should I use?"."""
                    .formatted(acceptorConfig.getAcceptorTypeList()));
        }
    }

    private Optional<HillClimbingAcceptor<Solution_>> buildHillClimbingAcceptor() {
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.HILL_CLIMBING)) {
            return Optional.of(new HillClimbingAcceptor<>());
        }
        return Optional.empty();
    }

    private boolean acceptorTypeListsContainsAcceptorType(AcceptorType acceptorType) {
        var acceptorTypeList = acceptorConfig.getAcceptorTypeList();
        return acceptorTypeList != null && acceptorTypeList.contains(acceptorType);
    }

    private Optional<StepCountingHillClimbingAcceptor<Solution_>> buildStepCountingHillClimbingAcceptor() {
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.STEP_COUNTING_HILL_CLIMBING)
                || acceptorConfig.getStepCountingHillClimbingSize() != null) {
            int stepCountingHillClimbingSize_ =
                    Objects.requireNonNullElse(acceptorConfig.getStepCountingHillClimbingSize(), 400);
            var stepCountingHillClimbingType_ =
                    Objects.requireNonNullElse(acceptorConfig.getStepCountingHillClimbingType(),
                            StepCountingHillClimbingType.STEP);
            var acceptor = new StepCountingHillClimbingAcceptor<Solution_>(
                    stepCountingHillClimbingSize_, stepCountingHillClimbingType_);
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }

    private Optional<EntityTabuAcceptor<Solution_>> buildEntityTabuAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        var entityTabuSize = acceptorConfig.getEntityTabuSize();
        var entityTabuRatio = acceptorConfig.getEntityTabuRatio();
        var fadingEntityTabuSize = acceptorConfig.getFadingEntityTabuSize();
        var fadingEntityTabuRatio = acceptorConfig.getFadingEntityTabuRatio();
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.ENTITY_TABU)
                || entityTabuSize != null || entityTabuRatio != null
                || fadingEntityTabuSize != null || fadingEntityTabuRatio != null) {
            var acceptor = new EntityTabuAcceptor<Solution_>(configPolicy.getLogIndentation());
            if (entityTabuSize != null) {
                if (entityTabuRatio != null) {
                    throw new IllegalArgumentException(
                            "The acceptor cannot have both entityTabuSize (%d) and entityTabuRatio (%f)."
                                    .formatted(entityTabuSize, entityTabuRatio));
                }
                acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(entityTabuSize));
            } else if (entityTabuRatio != null) {
                acceptor.setTabuSizeStrategy(new EntityRatioTabuSizeStrategy<>(entityTabuRatio));
            } else if (fadingEntityTabuSize == null && fadingEntityTabuRatio == null) {
                acceptor.setTabuSizeStrategy(new EntityRatioTabuSizeStrategy<>(0.1));
            }
            if (fadingEntityTabuSize != null) {
                if (fadingEntityTabuRatio != null) {
                    throw new IllegalArgumentException(
                            "The acceptor cannot have both fadingEntityTabuSize (%d) and fadingEntityTabuRatio (%f)."
                                    .formatted(fadingEntityTabuSize, fadingEntityTabuRatio));
                }
                acceptor.setFadingTabuSizeStrategy(new FixedTabuSizeStrategy<>(fadingEntityTabuSize));
            } else if (fadingEntityTabuRatio != null) {
                acceptor.setFadingTabuSizeStrategy(new EntityRatioTabuSizeStrategy<>(fadingEntityTabuRatio));
            }
            if (configPolicy.getEnvironmentMode().isFullyAsserted()) {
                acceptor.setAssertTabuHashCodeCorrectness(true);
            }
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }

    private Optional<ValueTabuAcceptor<Solution_>> buildValueTabuAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        var valueTabuSize = acceptorConfig.getValueTabuSize();
        var fadingValueTabuSize = acceptorConfig.getFadingValueTabuSize();
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.VALUE_TABU)
                || valueTabuSize != null || fadingValueTabuSize != null) {
            if (valueTabuSize == null && fadingValueTabuSize == null) {
                throw new IllegalArgumentException(
                        "The acceptorType (%s) requires either valueTabuSize or fadingValueTabuSize to be configured."
                                .formatted(AcceptorType.VALUE_TABU));
            }
            var acceptor = new ValueTabuAcceptor<Solution_>(configPolicy.getLogIndentation());
            configureFixedSizeTabuAcceptor(acceptor, configPolicy, valueTabuSize, fadingValueTabuSize);
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }

    private static <Solution_> void configureFixedSizeTabuAcceptor(AbstractTabuAcceptor<Solution_> acceptor,
            HeuristicConfigPolicy<Solution_> configPolicy, Integer tabuSize, Integer fadingTabuSize) {
        if (tabuSize != null) {
            acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(tabuSize));
        }
        if (fadingTabuSize != null) {
            acceptor.setFadingTabuSizeStrategy(new FixedTabuSizeStrategy<>(fadingTabuSize));
        }
        if (configPolicy.getEnvironmentMode().isFullyAsserted()) {
            acceptor.setAssertTabuHashCodeCorrectness(true);
        }
    }

    private Optional<MoveTabuAcceptor<Solution_>> buildMoveTabuAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        var moveTabuSize = acceptorConfig.getMoveTabuSize();
        var fadingMoveTabuSize = acceptorConfig.getFadingMoveTabuSize();
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.MOVE_TABU)
                || moveTabuSize != null || fadingMoveTabuSize != null) {
            if (moveTabuSize == null && fadingMoveTabuSize == null) {
                throw new IllegalArgumentException(
                        "The acceptorType (%s) requires either moveTabuSize or fadingMoveTabuSize to be configured."
                                .formatted(AcceptorType.MOVE_TABU));
            }
            var acceptor = new MoveTabuAcceptor<Solution_>(configPolicy.getLogIndentation());
            configureFixedSizeTabuAcceptor(acceptor, configPolicy, moveTabuSize, fadingMoveTabuSize);
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }

    private Optional<SimulatedAnnealingAcceptor<Solution_>>
            buildSimulatedAnnealingAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.SIMULATED_ANNEALING)
                || acceptorConfig.getSimulatedAnnealingStartingTemperature() != null) {
            var acceptor = new SimulatedAnnealingAcceptor<Solution_>();
            if (acceptorConfig.getSimulatedAnnealingStartingTemperature() == null) {
                // TODO Support SA without a parameter
                throw new IllegalArgumentException(
                        "The acceptorType (%s) requires non-null acceptorConfig.getSimulatedAnnealingStartingTemperature()."
                                .formatted(AcceptorType.SIMULATED_ANNEALING));
            }
            acceptor.setStartingTemperature(
                    configPolicy.getScoreDefinition().parseScore(acceptorConfig.getSimulatedAnnealingStartingTemperature()));
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }

    private Optional<LateAcceptanceAcceptor<Solution_>> buildLateAcceptanceAcceptor() {
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.LATE_ACCEPTANCE)
                || (!acceptorTypeListsContainsAcceptorType(AcceptorType.DIVERSIFIED_LATE_ACCEPTANCE)
                        && acceptorConfig.getLateAcceptanceSize() != null)) {
            var acceptor = new LateAcceptanceAcceptor<Solution_>();
            acceptor.setLateAcceptanceSize(Objects.requireNonNullElse(acceptorConfig.getLateAcceptanceSize(), 400));
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }

    private Optional<DiversifiedLateAcceptanceAcceptor<Solution_>>
            buildDiversifiedLateAcceptanceAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.DIVERSIFIED_LATE_ACCEPTANCE)) {
            configPolicy.ensurePreviewFeature(PreviewFeature.DIVERSIFIED_LATE_ACCEPTANCE);
            var acceptor = new DiversifiedLateAcceptanceAcceptor<Solution_>();
            acceptor.setLateAcceptanceSize(Objects.requireNonNullElse(acceptorConfig.getLateAcceptanceSize(), 5));
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }

    private Optional<GreatDelugeAcceptor<Solution_>> buildGreatDelugeAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.GREAT_DELUGE)
                || acceptorConfig.getGreatDelugeWaterLevelIncrementScore() != null
                || acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() != null) {
            var acceptor = new GreatDelugeAcceptor<Solution_>();
            if (acceptorConfig.getGreatDelugeWaterLevelIncrementScore() != null) {
                if (acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() != null) {
                    throw new IllegalArgumentException("""
                            The acceptor cannot have both acceptorConfig.getGreatDelugeWaterLevelIncrementScore() (%s) \
                            and acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() (%s)."""
                            .formatted(acceptorConfig.getGreatDelugeWaterLevelIncrementScore(),
                                    acceptorConfig.getGreatDelugeWaterLevelIncrementRatio()));
                }
                acceptor.setWaterLevelIncrementScore(
                        configPolicy.getScoreDefinition().parseScore(acceptorConfig.getGreatDelugeWaterLevelIncrementScore()));
            } else if (acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() != null) {
                if (acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() <= 0.0) {
                    throw new IllegalArgumentException("""
                            The acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() (%s) must be positive \
                            because the water level should increase."""
                            .formatted(acceptorConfig.getGreatDelugeWaterLevelIncrementRatio()));
                }
                acceptor.setWaterLevelIncrementRatio(acceptorConfig.getGreatDelugeWaterLevelIncrementRatio());
            } else {
                acceptor.setWaterLevelIncrementRatio(DEFAULT_WATER_LEVEL_INCREMENT_RATIO);
            }
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }
}
