package ai.timefold.solver.spring.boot.autoconfigure.config;

import static java.util.stream.Collectors.joining;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.convert.DurationStyle;

public class TerminationProperties {

    private static final String SPENT_LIMIT_PROERTY_NAME = "spent-limit";
    private static final String UNIMPROVED_SPENT_LIMIT_PROERTY_NAME = "unimproved-spent-limit";
    private static final String BEST_SCORE_LIMIT_PROERTY_NAME = "best-score-limit";
    private static final Set<String> VALID_FIELD_NAMES_SET =
            Set.of(SPENT_LIMIT_PROERTY_NAME, UNIMPROVED_SPENT_LIMIT_PROERTY_NAME, BEST_SCORE_LIMIT_PROERTY_NAME);

    /**
     * How long the solver can run.
     * For example: "30s" is 30 seconds. "5m" is 5 minutes. "2h" is 2 hours. "1d" is 1 day.
     * Also supports ISO-8601 format, see java.time.Duration.
     */
    private Duration spentLimit;
    /**
     * How long the solver can run without finding a new best solution after finding a new best solution.
     * For example: "30s" is 30 seconds. "5m" is 5 minutes. "2h" is 2 hours. "1d" is 1 day.
     * Also supports ISO-8601 format, see java.time.Duration.
     */
    private Duration unimprovedSpentLimit;
    /**
     * Terminates the solver when a specific or higher score has been reached.
     * For example: "0hard/-1000soft" terminates when the best score changes from "0hard/-1200soft" to "0hard/-900soft".
     * Wildcards are supported to replace numbers.
     * For example: "0hard/*soft" to terminate when any feasible score is reached.
     */
    private String bestScoreLimit;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public Duration getSpentLimit() {
        return spentLimit;
    }

    public void setSpentLimit(Duration spentLimit) {
        this.spentLimit = spentLimit;
    }

    public Duration getUnimprovedSpentLimit() {
        return unimprovedSpentLimit;
    }

    public void setUnimprovedSpentLimit(Duration unimprovedSpentLimit) {
        this.unimprovedSpentLimit = unimprovedSpentLimit;
    }

    public String getBestScoreLimit() {
        return bestScoreLimit;
    }

    public void setBestScoreLimit(String bestScoreLimit) {
        this.bestScoreLimit = bestScoreLimit;
    }

    public void loadProperties(Map<String, Object> properties) {
        // Check if the keys are valid
        String invalidKeys = properties.entrySet().stream()
                .filter(e -> !VALID_FIELD_NAMES_SET.contains(e.getKey()))
                .map(Map.Entry::getKey)
                .collect(joining(", "));

        if (!invalidKeys.isBlank()) {
            throw new IllegalStateException("""
                    The termination properties [%s] are not valid.
                    Maybe try changing the property name to kebab-case.
                    Here is the list of valid properties: %s"""
                    .formatted(invalidKeys, String.join(", ", VALID_FIELD_NAMES_SET)));
        }
        properties.forEach(this::loadProperty);
    }

    private void loadProperty(String key, Object value) {
        if (value == null) {
            return;
        }
        switch (key) {
            case SPENT_LIMIT_PROERTY_NAME:
                setSpentLimit(DurationStyle.detectAndParse((String) value));
                break;
            case UNIMPROVED_SPENT_LIMIT_PROERTY_NAME:
                setUnimprovedSpentLimit(DurationStyle.detectAndParse((String) value));
                break;
            case BEST_SCORE_LIMIT_PROERTY_NAME:
                setBestScoreLimit(value.toString());
                break;
            default:
                throw new IllegalStateException("The property %s is not valid.".formatted(key));
        }
    }

}
