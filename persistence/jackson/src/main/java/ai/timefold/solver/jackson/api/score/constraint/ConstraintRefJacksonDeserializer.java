package ai.timefold.solver.jackson.api.score.constraint;

import ai.timefold.solver.core.api.score.stream.ConstraintRef;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public final class ConstraintRefJacksonDeserializer extends JsonDeserializer<ConstraintRef> {
    @Override
    public ConstraintRef deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
        return ConstraintRef.of(p.getValueAsString());
    }

}
