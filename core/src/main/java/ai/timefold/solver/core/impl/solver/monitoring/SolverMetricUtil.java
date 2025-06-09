package ai.timefold.solver.core.impl.solver.monitoring;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NullMarked;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

@NullMarked
public final class SolverMetricUtil {

    // Necessary for benchmarker, but otherwise undocumented and not considered public.
    private static final String UNASSIGNED_COUNT = "unassigned.count";

    public static <Score_ extends Score<Score_>> void registerScore(SolverMetric metric, Tags tags,
            ScoreDefinition<Score_> scoreDefinition, Map<Tags, ScoreLevels> tagToScoreLevels, InnerScore<Score_> innerScore) {
        var levelValues = innerScore.raw().toLevelNumbers();
        if (tagToScoreLevels.containsKey(tags)) {
            var scoreLevels = tagToScoreLevels.get(tags);
            scoreLevels.setUnnassignedCount(innerScore.unassignedCount());
            for (var i = 0; i < levelValues.length; i++) {
                scoreLevels.setLevelValue(i, levelValues[i]);
            }
        } else {
            var levelLabels = scoreDefinition.getLevelLabels();
            for (var i = 0; i < levelLabels.length; i++) {
                levelLabels[i] = levelLabels[i].replace(' ', '.');
            }
            var scoreLevels = new Number[levelLabels.length];
            System.arraycopy(levelValues, 0, scoreLevels, 0, levelValues.length);
            var result = new ScoreLevels(innerScore.unassignedCount(), scoreLevels);
            tagToScoreLevels.put(tags, result);
            Metrics.gauge(metric.getMeterId() + "." + UNASSIGNED_COUNT, tags, result.unnassignedCount,
                    AtomicInteger::doubleValue);
            for (var i = 0; i < levelValues.length; i++) {
                Metrics.gauge(metric.getMeterId() + "." + levelLabels[i], tags, result.levelValues[i],
                        ref -> ref.get().doubleValue());
            }
        }
    }

    public static <Score_ extends Score<Score_>> InnerScore<Score_> extractScore(SolverMetric metric,
            ScoreDefinition<Score_> scoreDefinition, Function<String, Number> scoreLevelFunction) {
        var labelNames = scoreDefinition.getLevelLabels();
        for (var i = 0; i < labelNames.length; i++) {
            labelNames[i] = labelNames[i].replace(' ', '.');
        }
        var levelNumbers = new Number[labelNames.length];
        for (var i = 0; i < labelNames.length; i++) {
            levelNumbers[i] = scoreLevelFunction.apply(metric.getMeterId() + "." + labelNames[i]);
        }
        var score = scoreDefinition.fromLevelNumbers(levelNumbers);
        var unassignedCount = scoreLevelFunction.apply(metric.getMeterId() + "." + UNASSIGNED_COUNT);
        return InnerScore.withUnassignedCount(score, unassignedCount.intValue());
    }

    private SolverMetricUtil() {
        // No external instances.
    }

}
