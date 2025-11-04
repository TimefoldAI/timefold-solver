package ai.timefold.solver.core.preview.api.move.ruin;

import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NonNull;

public interface RecreateEvaluator<Value_, Score_ extends Score<@NonNull Score_>> {
    Score_ evaluate(Value_ newValue);
}
