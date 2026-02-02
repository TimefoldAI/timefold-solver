package ai.timefold.solver.jackson.api.score.stream.common;

import java.io.IOException;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public final class LoadBalanceJacksonDeserializer<Value_>
        extends JsonDeserializer<LoadBalance<Value_>> {

    @Override
    public LoadBalance<Value_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        return deserializationContext.readTreeAsValue(jsonNode, DeserializableLoadBalance.class);
    }

}
