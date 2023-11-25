package ai.timefold.solver.jackson.api.score.stream.common;

import java.io.IOException;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public final class SequenceChainJacksonDeserializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonDeserializer<SequenceChain<Value_, Difference_>> {

    @Override
    public SequenceChain<Value_, Difference_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        return deserializationContext.readTreeAsValue(jsonNode, DeserializedSequenceChain.class);
    }

}
