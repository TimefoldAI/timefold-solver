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
            return acceptorList.get(0);
        } else if (acceptorList.size() > 1) {
            return new CompositeAcceptor<>(acceptorList);
        } else {
            throw new IllegalArgumentException(
                    "The acceptor does not specify any acceptorType (" + acceptorConfig.getAcceptorTypeList()
                            + ") or other acceptor property.\n"
                            + "For a good starting values,"
                            + " see the docs section \"Which optimization algorithms should I use?\".");
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
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.ENTITY_TABU)
                || acceptorConfig.getEntityTabuSize() != null || acceptorConfig.getEntityTabuRatio() != null
                || acceptorConfig.getFadingEntityTabuSize() != null || acceptorConfig.getFadingEntityTabuRatio() != null) {
            var acceptor = new EntityTabuAcceptor<Solution_>(configPolicy.getLogIndentation());
            if (acceptorConfig.getEntityTabuSize() != null) {
                if (acceptorConfig.getEntityTabuRatio() != null) {
                    throw new IllegalArgumentException("The acceptor cannot have both acceptorConfig.getEntityTabuSize() ("
                            + acceptorConfig.getEntityTabuSize() + ") and acceptorConfig.getEntityTabuRatio() ("
                            + acceptorConfig.getEntityTabuRatio() + ").");
                }
                acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(acceptorConfig.getEntityTabuSize()));
            } else if (acceptorConfig.getEntityTabuRatio() != null) {
                acceptor.setTabuSizeStrategy(new EntityRatioTabuSizeStrategy<>(acceptorConfig.getEntityTabuRatio()));
            } else if (acceptorConfig.getFadingEntityTabuSize() == null && acceptorConfig.getFadingEntityTabuRatio() == null) {
                acceptor.setTabuSizeStrategy(new EntityRatioTabuSizeStrategy<>(0.1));
            }
            if (acceptorConfig.getFadingEntityTabuSize() != null) {
                if (acceptorConfig.getFadingEntityTabuRatio() != null) {
                    throw new IllegalArgumentException(
                            "The acceptor cannot have both acceptorConfig.getFadingEntityTabuSize() ("
                                    + acceptorConfig.getFadingEntityTabuSize()
                                    + ") and acceptorConfig.getFadingEntityTabuRatio() ("
                                    + acceptorConfig.getFadingEntityTabuRatio() + ").");
                }
                acceptor.setFadingTabuSizeStrategy(new FixedTabuSizeStrategy<>(acceptorConfig.getFadingEntityTabuSize()));
            } else if (acceptorConfig.getFadingEntityTabuRatio() != null) {
                acceptor.setFadingTabuSizeStrategy(
                        new EntityRatioTabuSizeStrategy<>(acceptorConfig.getFadingEntityTabuRatio()));
            }
            if (configPolicy.getEnvironmentMode().isFullyAsserted()) {
                acceptor.setAssertTabuHashCodeCorrectness(true);
            }
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }

    private Optional<ValueTabuAcceptor<Solution_>> buildValueTabuAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.VALUE_TABU)
                || acceptorConfig.getValueTabuSize() != null || acceptorConfig.getValueTabuRatio() != null
                || acceptorConfig.getFadingValueTabuSize() != null || acceptorConfig.getFadingValueTabuRatio() != null) {
            var acceptor = new ValueTabuAcceptor<Solution_>(configPolicy.getLogIndentation());
            if (acceptorConfig.getValueTabuSize() != null) {
                if (acceptorConfig.getValueTabuRatio() != null) {
                    throw new IllegalArgumentException("The acceptor cannot have both acceptorConfig.getValueTabuSize() ("
                            + acceptorConfig.getValueTabuSize() + ") and acceptorConfig.getValueTabuRatio() ("
                            + acceptorConfig.getValueTabuRatio() + ").");
                }
                acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(acceptorConfig.getValueTabuSize()));
            } else if (acceptorConfig.getValueTabuRatio() != null) {
                /*
                 * Although the strategy was implemented, it always threw UnsupportedOperationException.
                 * Therefore the strategy was removed and exception thrown here directly.
                 */
                throw new UnsupportedOperationException();
            }
            if (acceptorConfig.getFadingValueTabuSize() != null) {
                if (acceptorConfig.getFadingValueTabuRatio() != null) {
                    throw new IllegalArgumentException("The acceptor cannot have both acceptorConfig.getFadingValueTabuSize() ("
                            + acceptorConfig.getFadingValueTabuSize() + ") and acceptorConfig.getFadingValueTabuRatio() ("
                            + acceptorConfig.getFadingValueTabuRatio() + ").");
                }
                acceptor.setFadingTabuSizeStrategy(new FixedTabuSizeStrategy<>(acceptorConfig.getFadingValueTabuSize()));
            } else if (acceptorConfig.getFadingValueTabuRatio() != null) {
                /*
                 * Although the strategy was implemented, it always threw UnsupportedOperationException.
                 * Therefore the strategy was removed and exception thrown here directly.
                 */
                throw new UnsupportedOperationException();
            }

            if (acceptorConfig.getValueTabuSize() != null) {
                acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(acceptorConfig.getValueTabuSize()));
            }
            if (acceptorConfig.getFadingValueTabuSize() != null) {
                acceptor.setFadingTabuSizeStrategy(new FixedTabuSizeStrategy<>(acceptorConfig.getFadingValueTabuSize()));
            }
            if (configPolicy.getEnvironmentMode().isFullyAsserted()) {
                acceptor.setAssertTabuHashCodeCorrectness(true);
            }
            return Optional.of(acceptor);
        }
        return Optional.empty();
    }

    private Optional<MoveTabuAcceptor<Solution_>> buildMoveTabuAcceptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        if (acceptorTypeListsContainsAcceptorType(AcceptorType.MOVE_TABU)
                || acceptorConfig.getMoveTabuSize() != null || acceptorConfig.getFadingMoveTabuSize() != null) {
            var acceptor = new MoveTabuAcceptor<Solution_>(configPolicy.getLogIndentation());
            if (acceptorConfig.getMoveTabuSize() != null) {
                acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(acceptorConfig.getMoveTabuSize()));
            }
            if (acceptorConfig.getFadingMoveTabuSize() != null) {
                acceptor.setFadingTabuSizeStrategy(new FixedTabuSizeStrategy<>(acceptorConfig.getFadingMoveTabuSize()));
            }
            if (configPolicy.getEnvironmentMode().isFullyAsserted()) {
                acceptor.setAssertTabuHashCodeCorrectness(true);
            }
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
                throw new IllegalArgumentException("The acceptorType (" + AcceptorType.SIMULATED_ANNEALING
                        + ") currently requires a acceptorConfig.getSimulatedAnnealingStartingTemperature() ("
                        + acceptorConfig.getSimulatedAnnealingStartingTemperature() + ").");
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
                    throw new IllegalArgumentException("The acceptor cannot have both a "
                            + "acceptorConfig.getGreatDelugeWaterLevelIncrementScore() ("
                            + acceptorConfig.getGreatDelugeWaterLevelIncrementScore()
                            + ") and a acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() ("
                            + acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() + ").");
                }
                acceptor.setWaterLevelIncrementScore(
                        configPolicy.getScoreDefinition().parseScore(acceptorConfig.getGreatDelugeWaterLevelIncrementScore()));
            } else if (acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() != null) {
                if (acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() <= 0.0) {
                    throw new IllegalArgumentException("The acceptorConfig.getGreatDelugeWaterLevelIncrementRatio() ("
                            + acceptorConfig.getGreatDelugeWaterLevelIncrementRatio()
                            + ") must be positive because the water level should increase.");
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
