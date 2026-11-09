package ai.timefold.solver.service.definition.spi.metrics;

public interface OutputMetricsAware<OutputMetrics_ extends ModelOutputMetrics> {

    OutputMetrics_ getOutputMetrics();
}
