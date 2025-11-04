package ai.timefold.solver.core.preview.api.move.ruin;

import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NonNull;

public interface BasicRuinAndRecreator<Entity_, Value_, Metadata_ extends RecreateMetadata, Score_ extends Score<@NonNull Score_>> {
    Value_ recreate(RecreateEvaluator<Value_, Score_> evaluator,
            Entity_ recreatedEntity, Metadata_ valueMetadata);
}
