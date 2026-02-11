package ai.timefold.solver.jackson.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

public final class LoadBalanceJacksonDeserializer<Value_>
        extends ValueDeserializer<LoadBalance<Value_>> {

    @Override
    public LoadBalance<Value_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws JacksonException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        return deserializationContext.readTreeAsValue(jsonNode, DeserializableLoadBalance.class);
    }

}
