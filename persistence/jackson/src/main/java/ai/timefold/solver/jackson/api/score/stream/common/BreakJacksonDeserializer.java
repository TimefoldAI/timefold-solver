package ai.timefold.solver.jackson.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.Break;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonDeserializer;

public final class BreakJacksonDeserializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonDeserializer<Break<Value_, Difference_>> {

    @Override
    public Break<Value_, Difference_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws java.io.IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        return deserializationContext.readTreeAsValue(jsonNode, DeserializableBreak.class);
    }

}
