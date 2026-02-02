package ai.timefold.solver.jackson.api.score.constraint;

import java.io.IOException;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public final class ConstraintRefJacksonDeserializer extends JsonDeserializer<ConstraintRef> {
    @Override
    public ConstraintRef deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return ConstraintRef.parseId(p.getValueAsString());
    }

}
