package ai.timefold.solver.jackson.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonDeserializer;

public final class SequenceChainJacksonDeserializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonDeserializer<SequenceChain<Value_, Difference_>> {

    @Override
    public SequenceChain<Value_, Difference_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws java.io.IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        return deserializationContext.readTreeAsValue(jsonNode, DeserializableSequenceChain.class);
    }

}
