package ai.timefold.solver.service.definition.spi.metrics;

public interface InputMetricsAware<InputMetrics_ extends ModelInputMetrics> {

    InputMetrics_ getInputMetrics();
}
