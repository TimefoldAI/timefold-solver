package ai.timefold.solver.service.worker.impl.termination;

import static java.util.Objects.requireNonNullElse;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.core.config.solver.termination.DiminishedReturnsTerminationConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationCompositionStyle;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.service.definition.api.termination.SolverTerminationConfig;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);

    private final Duration spentLimit;
    private final Duration unimprovedSpentLimit;
    private final String bestScoreLimit; // exposed for testing
    private final Integer stepCountLimit;
    private final Long moveCountLimit;

    @Inject
    TerminationService(
            @ConfigProperty(name = TerminationConfigParams.TERMINATION_SPENT_LIMIT) String spentLimit,
            @ConfigProperty(
                    name = TerminationConfigParams.TERMINATION_UNIMPROVED_SPENT_LIMIT) Optional<String> unimprovedSpentLimit,
            @ConfigProperty(name = TerminationConfigParams.TERMINATION_BEST_SCORE_LIMIT) Optional<String> bestScoreLimit,
            @ConfigProperty(name = TerminationConfigParams.TERMINATION_STEP_COUNT_LIMIT) Optional<Integer> stepCountLimit,
            @ConfigProperty(name = TerminationConfigParams.TERMINATION_MOVE_COUNT_LIMIT) Optional<Long> moveCountLimit) {
        this.spentLimit = parseDurationFromConfig(TerminationConfigParams.TERMINATION_SPENT_LIMIT, spentLimit);
        this.unimprovedSpentLimit = unimprovedSpentLimit
                .map(s -> parseDurationFromConfig(TerminationConfigParams.TERMINATION_UNIMPROVED_SPENT_LIMIT, s)).orElse(null);
        this.bestScoreLimit = bestScoreLimit.orElse(null);
        this.stepCountLimit = stepCountLimit.orElse(null);
        this.moveCountLimit = moveCountLimit.orElse(null);
    }

    public TerminationConfig resolveTerminationConfig(SolverTerminationConfig terminationConfig) {
        if (terminationConfig == null) {
            return solverTerminationConfig(spentLimit, unimprovedSpentLimit, stepCountLimit, moveCountLimit, null, null);
        }
        var spentLimit = requireNonNullElse(terminationConfig.spentLimit(), this.spentLimit);
        // unimprovedSpentLimit may be null
        var unimprovedSpentLimit =
                terminationConfig.unimprovedSpentLimit() != null ? terminationConfig.unimprovedSpentLimit()
                        : this.unimprovedSpentLimit;
        var stepCountLimit =
                terminationConfig.stepCountLimit() != null ? terminationConfig.stepCountLimit() : this.stepCountLimit;
        var moveCountLimit =
                terminationConfig.moveCountLimit() != null ? terminationConfig.moveCountLimit() : this.moveCountLimit;

        return solverTerminationConfig(spentLimit, unimprovedSpentLimit, stepCountLimit, moveCountLimit,
                terminationConfig.slidingWindowDuration(), terminationConfig.minimumImprovementRatio());
    }

    private TerminationConfig solverTerminationConfig(Duration spentLimit, Duration unimprovedSpentLimit,
            Integer stepCountLimit, Long moveCountLimit, Duration diminishedReturnsSlidingWindowDuration,
            Double diminishedReturnsMinimumImprovementRatio) {
        var terminationConfig = new TerminationConfig()
                .withTerminationCompositionStyle(TerminationCompositionStyle.OR)
                .withSpentLimit(spentLimit)
                .withBestScoreLimit(bestScoreLimit);

        if (unimprovedSpentLimit != null) {
            terminationConfig.withUnimprovedSpentLimit(unimprovedSpentLimit);
            LOGGER.info("Using time spent ({}) with unimproved time spent ({}) termination.", spentLimit, unimprovedSpentLimit);
        } else if (stepCountLimit != null || moveCountLimit != null) {
            // stepCountLimit and moveCountLimit are both hard, deterministic caps used for benchmarking;
            // they are OR-composed and may be combined so whichever is reached first terminates the solver.
            List<String> limits = new ArrayList<>(2);
            if (stepCountLimit != null) {
                terminationConfig.withStepCountLimit(stepCountLimit);
                limits.add("step count limit (%d)".formatted(stepCountLimit));
            }
            if (moveCountLimit != null) {
                terminationConfig.withMoveCountLimit(moveCountLimit);
                limits.add("move count limit (%d)".formatted(moveCountLimit));
            }
            LOGGER.info("Using time spent ({}) with {} termination.", spentLimit, String.join(" or ", limits));
        } else {
            var diminishedReturnsConfig = new DiminishedReturnsTerminationConfig();
            List<String> tuning = new ArrayList<>(2);
            if (diminishedReturnsSlidingWindowDuration != null) {
                diminishedReturnsConfig.setSlidingWindowDuration(diminishedReturnsSlidingWindowDuration);
                tuning.add("slidingWindowDuration=" + diminishedReturnsSlidingWindowDuration);
            }
            if (diminishedReturnsMinimumImprovementRatio != null) {
                diminishedReturnsConfig.setMinimumImprovementRatio(diminishedReturnsMinimumImprovementRatio);
                tuning.add("minimumImprovementRatio=" + diminishedReturnsMinimumImprovementRatio);
            }
            terminationConfig.withDiminishedReturnsConfig(diminishedReturnsConfig);
            if (tuning.isEmpty()) {
                LOGGER.info("Using time spent ({}) with diminished returns termination.", spentLimit);
            } else {
                LOGGER.info("Using time spent ({}) with diminished returns termination ({}).", spentLimit,
                        String.join(", ", tuning));
            }
        }

        return terminationConfig;
    }

    private Duration parseDurationFromConfig(String propertyName, String value) {
        try {
            return Duration.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new TimefoldRuntimeException(ErrorCodes.INVALID_TERMINATION_CONFIG,
                    ("Cannot parse duration value ('%s') of property '%s' in the platform configuration. "
                            + "Please make sure it is a valid ISO 8601 Duration.").formatted(value, propertyName),
                    e, false);
        }
    }
}
