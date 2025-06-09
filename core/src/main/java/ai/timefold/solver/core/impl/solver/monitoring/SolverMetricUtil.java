package ai.timefold.solver.core.impl.solver.monitoring;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

@NullMarked
public final class SolverMetricUtil {

    // Necessary for benchmarker, but otherwise undocumented and not considered public.
    private static final String UNASSIGNED_COUNT_LABEL = "unassigned.count";

    public static <Score_ extends Score<Score_>> void registerScore(SolverMetric metric, Tags tags,
            ScoreDefinition<Score_> scoreDefinition, Map<Tags, ScoreLevels> tagToScoreLevels, InnerScore<Score_> innerScore) {
        var levelValues = innerScore.raw().toLevelNumbers();
        if (tagToScoreLevels.containsKey(tags)) {
            // Set new score levels for the previously registered gauges to read.
            var scoreLevels = tagToScoreLevels.get(tags);
            scoreLevels.setUnassignedCount(innerScore.unassignedCount());
            for (var i = 0; i < levelValues.length; i++) {
                scoreLevels.setLevelValue(i, levelValues[i]);
            }
        } else {
            var levelLabels = getLevelLabels(scoreDefinition);
            var scoreLevels = new Number[levelLabels.length];
            System.arraycopy(levelValues, 0, scoreLevels, 0, levelValues.length);
            var result = new ScoreLevels(innerScore.unassignedCount(), scoreLevels);
            tagToScoreLevels.put(tags, result);

            // Register the gauges to read the score levels.
            Metrics.gauge(getGaugeName(metric, UNASSIGNED_COUNT_LABEL), tags, result.unassignedCount,
                    AtomicInteger::doubleValue);
            for (var i = 0; i < levelValues.length; i++) {
                Metrics.gauge(getGaugeName(metric, levelLabels[i]), tags, result.levelValues[i],
                        ref -> ref.get().doubleValue());
            }
        }
    }

    private static String[] getLevelLabels(ScoreDefinition<?> scoreDefinition) {
        var labelNames = scoreDefinition.getLevelLabels();
        for (var i = 0; i < labelNames.length; i++) {
            labelNames[i] = labelNames[i].replace(' ', '.');
        }
        return labelNames;
    }

    public static String getGaugeName(SolverMetric metric, String label) {
        return metric.getMeterId() + "." + label;
    }

    public static @Nullable Double getGaugeValue(MeterRegistry registry, SolverMetric metric, Tags runId) {
        return getGaugeValue(registry, metric.getMeterId(), runId);
    }

    public static @Nullable Double getGaugeValue(MeterRegistry registry, String meterId, Tags runId) {
        var gauge = registry.find(meterId).tags(runId).gauge();
        if (gauge != null && Double.isFinite(gauge.value())) {
            return gauge.value();
        } else {
            return null;
        }
    }

    public static <Score_ extends Score<Score_>> @Nullable InnerScore<Score_> extractScore(SolverMetric metric,
            ScoreDefinition<Score_> scoreDefinition, Function<String, @Nullable Number> scoreLevelFunction) {
        var levelLabels = getLevelLabels(scoreDefinition);
        var levelNumbers = new Number[levelLabels.length];
        for (var i = 0; i < levelLabels.length; i++) {
            var levelNumber = scoreLevelFunction.apply(getGaugeName(metric, levelLabels[i]));
            if (levelNumber == null) {
                return null;
            }
            levelNumbers[i] = levelNumber;
        }
        var score = scoreDefinition.fromLevelNumbers(levelNumbers);
        var unassignedCount = scoreLevelFunction.apply(getGaugeName(metric, UNASSIGNED_COUNT_LABEL));
        if (unassignedCount == null) {
            return null;
        }
        return InnerScore.withUnassignedCount(score, unassignedCount.intValue());
    }

    private SolverMetricUtil() {
        // No external instances.
    }

}
