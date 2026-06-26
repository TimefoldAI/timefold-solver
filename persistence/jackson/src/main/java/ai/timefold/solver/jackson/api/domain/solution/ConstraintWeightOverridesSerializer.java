package ai.timefold.solver.jackson.api.domain.solution;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.JsonSerializer;

public final class ConstraintWeightOverridesSerializer<Score_ extends Score<Score_>>
        extends JsonSerializer<ConstraintWeightOverrides<Score_>> {

    @Override
    public void serialize(ConstraintWeightOverrides<Score_> constraintWeightOverrides, JsonGenerator generator,
            SerializerProvider serializerProvider) throws java.io.IOException {
        generator.writeStartObject();
        for (var constraintId : constraintWeightOverrides.getKnownConstraintIds()) {
            var weight = Objects.requireNonNull(constraintWeightOverrides.getConstraintWeight(constraintId));
            generator.writeStringField(constraintId, weight.toString());
        }
        generator.writeEndObject();
    }
}
