package ai.timefold.solver.service.definition.api.metrics;

public interface InputMetricsAware<InputMetrics_ extends ModelInputMetrics> {

    InputMetrics_ getInputMetrics();
}
