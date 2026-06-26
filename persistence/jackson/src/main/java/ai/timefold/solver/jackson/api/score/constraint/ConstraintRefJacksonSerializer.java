package ai.timefold.solver.jackson.api.score.constraint;

import ai.timefold.solver.core.api.score.stream.ConstraintRef;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.JsonSerializer;

public final class ConstraintRefJacksonSerializer extends JsonSerializer<ConstraintRef> {

    @Override
    public void serialize(ConstraintRef constraintRef, JsonGenerator generator, SerializerProvider serializers)
            throws java.io.IOException {
        generator.writeString(constraintRef.id());
    }
}
