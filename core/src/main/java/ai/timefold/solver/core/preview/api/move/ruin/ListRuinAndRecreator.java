package ai.timefold.solver.core.preview.api.move.ruin;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;

import org.jspecify.annotations.NonNull;

public interface ListRuinAndRecreator<Entity_, Value_, Metadata_ extends RecreateMetadata, Score_ extends Score<@NonNull Score_>> {
    ElementPosition recreate(RecreateEvaluator<ElementPosition, Score_> evaluator, Value_ recreatedValue,
            Metadata_ locationMetadata);
}
