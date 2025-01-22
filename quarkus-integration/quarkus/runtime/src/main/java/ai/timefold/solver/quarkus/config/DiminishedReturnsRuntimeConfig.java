package ai.timefold.solver.quarkus.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface DiminishedReturnsRuntimeConfig {
    /**
     * If set to true, adds a termination to the local search
     * phase that records the initial improvement after a duration,
     * and terminates when the ratio new improvement/initial improvement
     * is below a specified ratio.
     */
    Optional<Boolean> enabled();

    /**
     * Specify the best score from how long ago should the current best
     * score be compared to.
     * For "30s", the current best score is compared against
     * the best score from 30 seconds ago to calculate the improvement.
     * "5m" is 5 minutes. "2h" is 2 hours. "1d" is 1 day.
     * Also supports ISO-8601 format, see {@link Duration}.
     * <br/>
     * Default to 30s.
     */
    Optional<Duration> slidingWindowDuration();

    /**
     * Specify the minimum ratio between the current improvement and the
     * initial improvement. Must be positive.
     * <br/>
     * For example, if the {@link #slidingWindowDuration} is "30s",
     * the {@link #minimumImprovementRatio} is 0.25, and the
     * score improves by 100soft during the first 30 seconds of local search,
     * then the local search phase will terminate when the difference between
     * the current best score and the best score from 30 seconds ago is less than
     * 25soft (= 0.25 * 100soft).
     * <br/>
     * Defaults to 0.0001.
     */
    OptionalDouble minimumImprovementRatio();
}
