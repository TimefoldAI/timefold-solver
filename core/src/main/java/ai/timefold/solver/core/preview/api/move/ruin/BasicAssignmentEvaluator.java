package ai.timefold.solver.core.preview.api.move.ruin;

import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NonNull;

public interface BasicAssignmentEvaluator<Entity_, Value_, Score_ extends Score<@NonNull Score_>> {
    void assign(Entity_ entity, Value_ value);

    void unassign(Entity_ entity);

    Score_ score();
}
