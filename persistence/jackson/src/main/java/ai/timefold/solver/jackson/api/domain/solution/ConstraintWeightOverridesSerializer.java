package ai.timefold.solver.jackson.api.domain.solution;

import java.io.IOException;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class ConstraintWeightOverridesSerializer<Score_ extends Score<Score_>>
        extends JsonSerializer<ConstraintWeightOverrides<Score_>> {

    @Override
    public void serialize(ConstraintWeightOverrides<Score_> constraintWeightOverrides, JsonGenerator generator,
            SerializerProvider serializerProvider) throws IOException {
        generator.writeStartObject();
        for (var constraintName : constraintWeightOverrides.getKnownConstraintNames()) {
            var weight = constraintWeightOverrides.getConstraintWeight(constraintName);
            generator.writeStringField(constraintName, weight.toString());
        }
        generator.writeEndObject();
    }
}
