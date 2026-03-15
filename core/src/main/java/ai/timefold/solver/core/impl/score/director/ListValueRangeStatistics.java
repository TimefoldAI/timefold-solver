package ai.timefold.solver.core.impl.score.director;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.impl.util.MutableInt;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ListValueRangeStatistics {
    private final Map<ValueRangeState.HashedValueRange<?>, MutableInt> valueRangeToInstanceCount;

    public ListValueRangeStatistics() {
        valueRangeToInstanceCount = new HashMap<>();
    }

    public void addValueRange(ValueRange<?> valueRange) {
        var hashedValueRange = ValueRangeState.HashedValueRange.of(valueRange);
        valueRangeToInstanceCount
                .computeIfAbsent(hashedValueRange, ignored -> new MutableInt(0))
                .increment();
    }

    public long computeListProblemScaleLog(boolean allowsUnassignedValues, long logBase) {
        var valueToRangeCount = new IdentityHashMap<Object, MutableInt>();
        // Unassigned values are treated as if they are assigned to a virtual entity to simplify calculations
        var entityCount = allowsUnassignedValues ? 1 : 0;
        for (var entry : valueRangeToInstanceCount.entrySet()) {
            var iterator = entry.getKey().item().createOriginalIterator();
            var valueRangeInstanceCount = entry.getValue().intValue();
            entityCount += valueRangeInstanceCount;
            while (iterator.hasNext()) {
                var value = iterator.next();
                valueToRangeCount.computeIfAbsent(value, ignored -> new MutableInt(0))
                        .add(valueRangeInstanceCount);

            }
        }

        if (entityCount == 0) {
            return 0L;
        }

        var valueCount = valueToRangeCount.size();
        var validPercentageLog = 0L;
        var additionalCount = allowsUnassignedValues ? 1 : 0;
        for (var validEntityCount : valueToRangeCount.values()) {
            validPercentageLog += MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, logBase,
                    (validEntityCount.doubleValue() + additionalCount) / entityCount);
        }
        return MathUtils.getPossibleArrangementsScaledApproximateLog(MathUtils.LOG_PRECISION, logBase,
                valueCount, entityCount) + validPercentageLog;
    }
}
