package ai.timefold.solver.jackson.api.score.constraint;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public final class ConstraintRefJacksonDeserializer extends ValueDeserializer<ConstraintRef> {
    @Override
    public ConstraintRef deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        return ConstraintRef.parseId(p.getValueAsString());
    }

}
