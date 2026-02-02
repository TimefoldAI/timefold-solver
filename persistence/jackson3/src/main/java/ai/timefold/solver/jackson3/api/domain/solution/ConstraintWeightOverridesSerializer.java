package ai.timefold.solver.jackson3.api.domain.solution;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class ConstraintWeightOverridesSerializer<Score_ extends Score<Score_>>
        extends ValueSerializer<ConstraintWeightOverrides<Score_>> {

    @Override
    public void serialize(ConstraintWeightOverrides<Score_> constraintWeightOverrides, JsonGenerator generator,
            SerializationContext serializerProvider) throws JacksonException {
        generator.writeStartObject();
        for (var constraintName : constraintWeightOverrides.getKnownConstraintNames()) {
            var weight = Objects.requireNonNull(constraintWeightOverrides.getConstraintWeight(constraintName));
            generator.writeStringProperty(constraintName, weight.toString());
        }
        generator.writeEndObject();
    }
}
