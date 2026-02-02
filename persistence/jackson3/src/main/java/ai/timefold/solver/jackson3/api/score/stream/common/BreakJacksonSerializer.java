package ai.timefold.solver.jackson3.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.Break;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class BreakJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends ValueSerializer<Break<Value_, Difference_>> {

    @Override
    public void serialize(Break<Value_, Difference_> brk, JsonGenerator jsonGenerator,
            SerializationContext serializerProvider) throws JacksonException {
        jsonGenerator.writePOJO(SerializableBreak.of(brk));
    }

}
