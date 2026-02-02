package ai.timefold.solver.jackson3.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.Break;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

public final class BreakJacksonDeserializer<Value_, Difference_ extends Comparable<Difference_>>
        extends ValueDeserializer<Break<Value_, Difference_>> {

    @Override
    public Break<Value_, Difference_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws JacksonException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        return deserializationContext.readTreeAsValue(jsonNode, DeserializableBreak.class);
    }

}
