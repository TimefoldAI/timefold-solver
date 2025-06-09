package ai.timefold.solver.core.impl.solver.monitoring;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NullMarked;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

@NullMarked
public final class SolverMetricUtil {

    public static void registerScoreMetrics(SolverMetric metric, Tags tags, ScoreDefinition<?> scoreDefinition,
            Map<Tags, ScoreLevels> tagToScoreLevels, InnerScore<?> innerScore) {
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
            Metrics.gauge(metric.getMeterId() + "." + SolverMetric.UNASSIGNED_COUNT, tags, result.unnassignedCount,
                    AtomicInteger::doubleValue);
            for (var i = 0; i < levelValues.length; i++) {
                Metrics.gauge(metric.getMeterId() + "." + levelLabels[i], tags, result.levelValues[i],
                        ref -> ref.get().doubleValue());
            }
        }
    }

    private SolverMetricUtil() {
        // No external instances.
    }

}
