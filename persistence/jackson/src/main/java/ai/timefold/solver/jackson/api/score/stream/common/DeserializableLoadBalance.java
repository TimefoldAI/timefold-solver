package ai.timefold.solver.jackson.api.score.stream.common;

import java.math.BigDecimal;
import java.util.Map;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

import org.jspecify.annotations.NonNull;

record DeserializableLoadBalance<Value_>(BigDecimal unfairness) implements LoadBalance<Value_> {

    @Override
    public @NonNull Map<Value_, Long> loads() {
        throw new UnsupportedOperationException("Deserialization of loads is not supported.");
    }

}
