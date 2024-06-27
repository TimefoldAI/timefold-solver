package ai.timefold.solver.core.impl.score.stream.collector;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

public final class LoadBalanceImpl<Balanced_> implements LoadBalance<Balanced_> {

    private final Map<Balanced_, Integer> balancedItemCountMap = new HashMap<>();
    private final Map<Balanced_, Long> balancedItemToMetricValueMap = new LinkedHashMap<>();

    private long sum = 0;
    private long squaredDeviationIntegralPart = 0;
    private long squaredDeviationFractionNumerator = 0;

    public Runnable registerBalanced(Balanced_ balanced, long metricValue, long initialMetricValue) {
        var balancedItemCount = balancedItemCountMap.compute(balanced, (k, v) -> v == null ? 1 : v + 1);
        if (balancedItemCount == 1) {
            addToMetric(balanced, metricValue + initialMetricValue);
        } else {
            addToMetric(balanced, metricValue);
        }
        return () -> unregisterBalanced(balanced, metricValue);
    }

    public void unregisterBalanced(Balanced_ balanced, long metricValue) {
        var count = balancedItemCountMap.compute(balanced, (k, v) -> v == 1 ? null : v - 1);
        if (count == null) {
            resetMetric(balanced);
        } else {
            addToMetric(balanced, -metricValue);
        }
    }

    private void addToMetric(Balanced_ balanced, long diff) {
        var oldValue = balancedItemToMetricValueMap.getOrDefault(balanced, 0L);
        var newValue = oldValue + diff;
        if (newValue == 0) {
            balancedItemToMetricValueMap.remove(balanced);
        } else {
            balancedItemToMetricValueMap.put(balanced, newValue);
        }
        updateSquaredDeviation(oldValue, newValue);
        sum += diff;
    }

    private void resetMetric(Balanced_ balanced) {
        var oldValue = balancedItemToMetricValueMap.remove(balanced);
        updateSquaredDeviation(oldValue, 0);
        sum -= oldValue;
    }

    private void updateSquaredDeviation(long oldValue, long newValue) {
        // o' = o + (x_0'^2 - x_0^2) + (2 * (x_0s - x_0's') + 2 * (x_1 + x_2 + x_3 + ... + x_n)(s - s') + (s'^2 - s^2))/n

        //(x_0'^2 - x_0^2)
        var squaredDeviationFirstTerm = newValue * newValue - oldValue * oldValue;

        // 2 * (x_1 + x_2 + x_3 + ... + x_n)
        var secondTermFirstFactor = 2 * (sum - oldValue);

        var newSum = sum - oldValue + newValue;

        // (s' - s)
        var secondTermSecondFactor = sum - newSum;

        // (s'^2 - s^2)
        var thirdTerm = newSum * newSum - sum * sum;

        // 2 * (x_0u - x_0'u')
        var fourthTerm = 2 * (oldValue * sum - newValue * newSum);
        var squaredDeviationSecondTermNumerator = secondTermFirstFactor * secondTermSecondFactor + thirdTerm + fourthTerm;

        squaredDeviationIntegralPart += squaredDeviationFirstTerm;
        squaredDeviationFractionNumerator += squaredDeviationSecondTermNumerator;
    }

    @Override
    public Map<Balanced_, Long> loads() {
        if (balancedItemCountMap.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(balancedItemToMetricValueMap);
    }

    @Override
    public BigDecimal unfairness() {
        var totalToBalanceCount = balancedItemCountMap.size();
        if (totalToBalanceCount == 0) {
            return BigDecimal.ZERO;
        }
        // 12 digits is twice the recommended minimum amount of 6.
        return BigDecimal.valueOf(squaredDeviationFractionNumerator)
                .divide(BigDecimal.valueOf(totalToBalanceCount), 12, RoundingMode.HALF_UP)
                .add(BigDecimal.valueOf(squaredDeviationIntegralPart))
                .stripTrailingZeros();
    }

}
