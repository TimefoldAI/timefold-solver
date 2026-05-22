package ai.timefold.solver.model.definition.api.metrics;

public interface InputMetricsAware<InputMetrics_ extends ModelInputMetrics> {

    InputMetrics_ getInputMetrics();
}
