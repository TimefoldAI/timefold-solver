package ai.timefold.solver.jackson.api.score.stream.common;

import java.io.IOException;

import ai.timefold.solver.core.api.score.stream.common.Break;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class BreakJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonSerializer<Break<Value_, Difference_>> {

    @Override
    public void serialize(Break<Value_, Difference_> brk, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(SerializedBreak.of(brk));
    }

}
