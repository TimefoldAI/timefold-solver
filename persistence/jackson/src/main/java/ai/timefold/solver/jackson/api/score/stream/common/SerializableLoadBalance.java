package ai.timefold.solver.jackson.api.score.stream.common;

import java.math.BigDecimal;
import java.util.Objects;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

record SerializableLoadBalance(BigDecimal unfairness) {

    SerializableLoadBalance(BigDecimal unfairness) {
        this.unfairness = Objects.requireNonNull(unfairness);
    }

    static <Value_> SerializableLoadBalance of(LoadBalance<Value_> loadBalance) {
        if (loadBalance == null) {
            return null;
        }
        // We do not serialize loads(), because:
        // - It's possibly a very large map, that would end up in the JSON through default justifications.
        // - Deserializing the map would be tricky, because the type of <Value_> is not known at runtime.
        return new SerializableLoadBalance(loadBalance.unfairness());
    }

}
