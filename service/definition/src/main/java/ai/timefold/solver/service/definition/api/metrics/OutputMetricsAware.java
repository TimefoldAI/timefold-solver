package ai.timefold.solver.service.definition.api.metrics;

public interface OutputMetricsAware<OutputMetrics_ extends ModelOutputMetrics> {

    OutputMetrics_ getOutputMetrics();
}
