package ai.timefold.solver.core.impl.score.director;

/**
 * Allows enabling or disabling the metric collection. In cases like Ruin and Recreate moves, metrics such as move count
 * should not be updated.
 */
public interface MetricCollector {

    void setEnableMetricCollection(boolean enable);
}
