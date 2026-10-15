package ai.timefold.solver.core.impl.solver.termination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.termination.TerminationId;
import ai.timefold.solver.core.api.solver.termination.TerminationType;
import ai.timefold.solver.core.config.solver.termination.TerminationCompositionStyle;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class TerminationFactory<Solution_> {

    public static <Solution_> TerminationFactory<Solution_> create(TerminationConfig terminationConfig) {
        return new TerminationFactory<>(terminationConfig);
    }

    private final TerminationConfig terminationConfig;

    private TerminationFactory(TerminationConfig terminationConfig) {
        this.terminationConfig = terminationConfig;
    }

    public <Termination_ extends Termination<Solution_>> Termination_
            buildTermination(HeuristicConfigPolicy<Solution_> configPolicy, Termination_ chainedTermination) {
        return buildTermination(configPolicy, chainedTermination, null);
    }

    /**
     * @param phaseIndex null for the solver termination
     */
    @SuppressWarnings("unchecked")
    public <Termination_ extends Termination<Solution_>> Termination_ buildTermination(
            HeuristicConfigPolicy<Solution_> configPolicy, Termination_ chainedTermination, @Nullable Integer phaseIndex) {
        var termination = buildTermination(configPolicy, null, 0, phaseIndex);
        if (termination == null) {
            return chainedTermination;
        }
        // This cast works, because composite termination is Universal, 
        // and therefore extends both SolverTermination and PhaseTermination.
        return (Termination_) new OrCompositeTermination<>(chainedTermination, termination);
    }

    /**
     * Builds the root level of the solver termination.
     *
     * @return sometimes null
     */
    public @Nullable Termination<Solution_> buildTermination(HeuristicConfigPolicy<Solution_> configPolicy) {
        return buildTermination(configPolicy, null, 0, null);
    }

    /**
     * See {@link TerminationId} for the id model.
     *
     * @param parent null for the root level
     * @param ordinal the index of this level in its parent; 0 for the root level
     * @param phaseIndex null for the solver termination
     * @return sometimes null
     */
    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> @Nullable Termination<Solution_> buildTermination(
            HeuristicConfigPolicy<Solution_> configPolicy, @Nullable TerminationId parent, int ordinal,
            @Nullable Integer phaseIndex) {
        var levelType = getCompositionStyle() == TerminationCompositionStyle.AND ? TerminationType.AND : TerminationType.OR;
        var levelId = new DefaultTerminationId(levelType, parent, ordinal, phaseIndex);
        List<Termination<Solution_>> terminationList = new ArrayList<>(buildTimeBasedTermination(configPolicy));

        if (terminationConfig.getBestScoreLimit() != null) {
            ScoreDefinition<Score_> scoreDefinition = configPolicy.getScoreDefinition();
            Score_ bestScoreLimit_ = scoreDefinition.parseScore(terminationConfig.getBestScoreLimit());
            double[] timeGradientWeightNumbers = new double[scoreDefinition.getLevelsSize() - 1];
            Arrays.fill(timeGradientWeightNumbers, 0.50); // Number pulled out of thin air
            terminationList.add(new BestScoreTermination<>(scoreDefinition, bestScoreLimit_, timeGradientWeightNumbers));
        }
        var bestScoreFeasible = terminationConfig.getBestScoreFeasible();
        if (bestScoreFeasible != null) {
            ScoreDefinition<Score_> scoreDefinition = configPolicy.getScoreDefinition();
            if (!bestScoreFeasible) {
                throw new IllegalArgumentException("The termination bestScoreFeasible (%s) cannot be false."
                        .formatted(bestScoreFeasible));
            }
            int feasibleLevelsSize = scoreDefinition.getFeasibleLevelsSize();
            if (feasibleLevelsSize < 1) {
                throw new IllegalStateException("""
                        The termination with bestScoreFeasible (%s) can only be used with a score type \
                        that has at least 1 feasible level but the scoreDefinition (%s) has feasibleLevelsSize (%s), \
                        which is less than 1."""
                        .formatted(bestScoreFeasible, scoreDefinition, feasibleLevelsSize));
            }
            double[] timeGradientWeightFeasibleNumbers = new double[feasibleLevelsSize - 1];
            Arrays.fill(timeGradientWeightFeasibleNumbers, 0.50); // Number pulled out of thin air
            terminationList.add(new BestScoreFeasibleTermination<>(scoreDefinition, timeGradientWeightFeasibleNumbers));
        }
        if (terminationConfig.getStepCountLimit() != null) {
            terminationList.add(new StepCountTermination<>(terminationConfig.getStepCountLimit()));
        }
        if (terminationConfig.getScoreCalculationCountLimit() != null) {
            terminationList.add(new ScoreCalculationCountTermination<>(terminationConfig.getScoreCalculationCountLimit()));
        }
        if (terminationConfig.getUnimprovedStepCountLimit() != null) {
            terminationList.add(new UnimprovedStepCountTermination<>(terminationConfig.getUnimprovedStepCountLimit()));
        }
        if (terminationConfig.getMoveCountLimit() != null) {
            terminationList.add(new MoveCountTermination<>(terminationConfig.getMoveCountLimit()));
        }
        for (var leaf : terminationList) {
            var abstractLeaf = (AbstractTermination<Solution_>) leaf;
            abstractLeaf.setTerminationId(new DefaultTerminationId(
                    Objects.requireNonNull(abstractLeaf.getTerminationType()), levelId, 0, phaseIndex));
        }
        terminationList.addAll(buildInnerTermination(configPolicy, levelId));
        return buildTerminationFromList(terminationList, levelId);
    }

    @SuppressWarnings("unchecked")
    <Score_ extends Score<Score_>> List<Termination<Solution_>>
            buildTimeBasedTermination(HeuristicConfigPolicy<Solution_> configPolicy) {
        List<Termination<Solution_>> terminationList = new ArrayList<>();
        Long timeMillisSpentLimit = terminationConfig.calculateTimeMillisSpentLimit();
        if (timeMillisSpentLimit != null) {
            terminationList.add(new TimeMillisSpentTermination<>(timeMillisSpentLimit));
        }
        Long unimprovedTimeMillisSpentLimit = terminationConfig.calculateUnimprovedTimeMillisSpentLimit();
        if (unimprovedTimeMillisSpentLimit != null) {
            if (terminationConfig.getUnimprovedScoreDifferenceThreshold() == null) {
                terminationList.add(new UnimprovedTimeMillisSpentTermination<>(unimprovedTimeMillisSpentLimit));
            } else {
                ScoreDefinition<Score_> scoreDefinition = configPolicy.getScoreDefinition();
                Score_ unimprovedScoreDifferenceThreshold_ =
                        scoreDefinition.parseScore(terminationConfig.getUnimprovedScoreDifferenceThreshold());
                if (scoreDefinition.isNegativeOrZero(unimprovedScoreDifferenceThreshold_)) {
                    throw new IllegalStateException("The unimprovedScoreDifferenceThreshold ("
                            + terminationConfig.getUnimprovedScoreDifferenceThreshold() + ") must be positive.");

                }
                terminationList.add(new UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination<>(
                        unimprovedTimeMillisSpentLimit, unimprovedScoreDifferenceThreshold_));
            }
        } else if (terminationConfig.getUnimprovedScoreDifferenceThreshold() != null) {
            throw new IllegalStateException("The unimprovedScoreDifferenceThreshold ("
                    + terminationConfig.getUnimprovedScoreDifferenceThreshold()
                    + ") can only be used if an unimproved*SpentLimit ("
                    + unimprovedTimeMillisSpentLimit + ") is used too.");
        }

        if (terminationConfig.getDiminishedReturnsConfig() != null) {
            var diminishedReturnsConfig = terminationConfig.getDiminishedReturnsConfig();
            var gracePeriodMillis = diminishedReturnsConfig.calculateSlidingWindowMilliseconds();
            if (gracePeriodMillis == null) {
                gracePeriodMillis = 30_000L;
            }
            var minimumImprovementRatio = diminishedReturnsConfig.getMinimumImprovementRatio();
            if (minimumImprovementRatio == null) {
                minimumImprovementRatio = 0.0001;
            }
            terminationList.add(new DiminishedReturnsTermination<>(gracePeriodMillis, minimumImprovementRatio));
        }

        return terminationList;
    }

    List<Termination<Solution_>> buildInnerTermination(HeuristicConfigPolicy<Solution_> configPolicy,
            DefaultTerminationId levelId) {
        var terminationConfigList = terminationConfig.getTerminationConfigList();
        if (ConfigUtils.isEmptyCollection(terminationConfigList)) {
            return Collections.emptyList();
        }
        var phaseIndex = levelId.nullablePhaseIndex();
        // The index comes before the null filter, so an empty level does not shift the ordinals.
        return IntStream.range(0, terminationConfigList.size())
                .mapToObj(i -> TerminationFactory.<Solution_> create(terminationConfigList.get(i))
                        .buildTermination(configPolicy, levelId, i, phaseIndex))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TerminationCompositionStyle getCompositionStyle() {
        return Objects.requireNonNullElse(terminationConfig.getTerminationCompositionStyle(), TerminationCompositionStyle.OR);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    Termination<Solution_> buildTerminationFromList(List<Termination<Solution_>> terminationList,
            DefaultTerminationId levelId) {
        return switch (terminationList.size()) {
            case 0 -> null;
            case 1 -> terminationList.getFirst();
            default -> {
                var terminationArray = terminationList.toArray(new Termination[0]);
                yield switch (getCompositionStyle()) {
                    case AND -> {
                        var and = new AndCompositeTermination<Solution_>(terminationArray);
                        and.setTerminationId(levelId);
                        terminationList.forEach(TerminationFactory::stopRecording);
                        yield and;
                    }
                    case OR -> new OrCompositeTermination<Solution_>(terminationArray);
                };
            }
        };
    }

    private static <Solution_> void stopRecording(Termination<Solution_> termination) {
        if (termination instanceof AbstractCompositeTermination<Solution_> composite) {
            if (composite instanceof OrCompositeTermination<Solution_> or) {
                or.stopRecording();
            }
            composite.terminationList.forEach(TerminationFactory::stopRecording);
        }
    }
}
