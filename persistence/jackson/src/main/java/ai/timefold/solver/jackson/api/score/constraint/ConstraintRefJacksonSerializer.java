package ai.timefold.solver.jackson.api.score.constraint;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class ConstraintRefJacksonSerializer extends ValueSerializer<ConstraintRef> {

    @Override
    public void serialize(ConstraintRef constraintRef, JsonGenerator generator, SerializationContext serializers)
            throws JacksonException {
        generator.writeString(constraintRef.constraintName());
    }
}
