package ai.timefold.solver.jackson.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.Break;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.JsonSerializer;

public final class BreakJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonSerializer<Break<Value_, Difference_>> {

    @Override
    public void serialize(Break<Value_, Difference_> brk, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws java.io.IOException {
        jsonGenerator.writePOJO(SerializableBreak.of(brk));
    }

}
