package ai.timefold.solver.jackson3.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class LoadBalanceJacksonSerializer<Value_>
        extends ValueSerializer<LoadBalance<Value_>> {

    @Override
    public void serialize(LoadBalance<Value_> loadBalance, JsonGenerator jsonGenerator, SerializationContext serializers)
            throws JacksonException {
        jsonGenerator.writePOJO(SerializableLoadBalance.of(loadBalance));
    }
}
