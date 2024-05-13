package ai.timefold.solver.core.impl.score.stream.collector;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class LoadBalanceCalculator implements ObjectCalculator<Object, BigDecimal> {

    private final Map<Object, Long> groupCountMap = new LinkedHashMap<>();
    // the sum of squared deviation from zero
    private long squaredSum = 0L;
    private BigDecimal result = null;

    @Override
    public void insert(Object o) {
        var count = groupCountMap.compute(o, (key, value) -> value == null ? 1L : value + 1L);
        // squaredZeroDeviation = squaredZeroDeviation - (count - 1)² + count²
        // <=> squaredZeroDeviation = squaredZeroDeviation + (2 * count - 1)
        add(2 * count - 1);
    }

    @Override
    public void retract(Object o) {
        var count = groupCountMap.compute(o, (key, value) -> Objects.equals(value, 1L) ? null : value - 1L);
        var diff = (count == null) ? 1L : 2 * count + 1;
        add(-diff);
    }

    private void add(long sum) {
        squaredSum += sum;
        if (squaredSum < 0) {
            throw new IllegalStateException("Impossible state: The squared sum (" + squaredSum + ") should not be negative.");
        }
        result = null;
    }

    @Override
    public BigDecimal result() {
        if (result == null) { // Avoid re-creating the BigDecimal every time.
            result = calculateResult(squaredSum);
        }
        return result;
    }

    private static BigDecimal calculateResult(long squaredSum) {
        if (squaredSum == 0L) {
            return BigDecimal.ZERO;
        } else if (squaredSum == 1L) {
            return BigDecimal.ONE;
        } else if (squaredSum == 10L) {
            return BigDecimal.TEN;
        } else {
            return BigDecimal.valueOf(Math.sqrt(squaredSum));
        }
    }

    public Map<Object, Long> getLoads() {
        return groupCountMap;
    }

}
