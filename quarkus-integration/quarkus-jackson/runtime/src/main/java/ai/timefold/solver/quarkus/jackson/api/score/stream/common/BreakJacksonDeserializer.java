package ai.timefold.solver.quarkus.jackson.api.score.stream.common;

import java.io.IOException;

import ai.timefold.solver.core.api.score.stream.common.Break;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public final class BreakJacksonDeserializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonDeserializer<Break<Value_, Difference_>> {

    @Override
    public Break<Value_, Difference_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        return deserializationContext.readTreeAsValue(jsonNode, DeserializableBreak.class);
    }

}
