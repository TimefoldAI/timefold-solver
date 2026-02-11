package ai.timefold.solver.quarkus.jackson.score.stream.common;

import java.io.IOException;

import ai.timefold.solver.core.api.score.stream.common.Sequence;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public final class SequenceJacksonDeserializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonDeserializer<Sequence<Value_, Difference_>> {

    @Override
    public Sequence<Value_, Difference_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        return deserializationContext.readTreeAsValue(jsonNode, DeserializableSequence.class);
    }

}
