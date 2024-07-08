package ai.timefold.solver.jackson.api.score.stream.common;

import java.io.IOException;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class LoadBalanceJacksonSerializer<Value_>
        extends JsonSerializer<LoadBalance<Value_>> {

    @Override
    public void serialize(LoadBalance<Value_> loadBalance, JsonGenerator jsonGenerator, SerializerProvider serializers)
            throws IOException {
        jsonGenerator.writeObject(SerializableLoadBalance.of(loadBalance));
    }
}
