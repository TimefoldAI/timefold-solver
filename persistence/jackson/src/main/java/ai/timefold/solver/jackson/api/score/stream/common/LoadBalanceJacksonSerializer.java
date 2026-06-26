package ai.timefold.solver.jackson.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.JsonSerializer;

public final class LoadBalanceJacksonSerializer<Value_>
        extends JsonSerializer<LoadBalance<Value_>> {

    @Override
    public void serialize(LoadBalance<Value_> loadBalance, JsonGenerator jsonGenerator, SerializerProvider serializers)
            throws java.io.IOException {
        jsonGenerator.writePOJO(SerializableLoadBalance.of(loadBalance));
    }
}
