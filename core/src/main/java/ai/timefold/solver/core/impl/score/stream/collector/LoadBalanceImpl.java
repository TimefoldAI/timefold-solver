package ai.timefold.solver.core.impl.score.stream.collector;

import java.math.BigDecimal;
import java.util.Map;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

public record LoadBalanceImpl(BigDecimal unfairness, Map<Object, Long> loads) implements LoadBalance {

}
