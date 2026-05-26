package ai.timefold.solver.model.definition.api.metrics;

public interface OutputMetricsAware<OutputMetrics_ extends ModelOutputMetrics> {

    OutputMetrics_ getOutputMetrics();
}
