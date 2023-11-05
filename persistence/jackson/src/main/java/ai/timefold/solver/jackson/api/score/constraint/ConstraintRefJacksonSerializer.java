package ai.timefold.solver.jackson.api.score.constraint;

import java.io.IOException;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class ConstraintRefJacksonSerializer extends JsonSerializer<ConstraintRef> {

    @Override
    public void serialize(ConstraintRef constraintRef, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        generator.writeString(constraintRef.constraintId());
    }
}
