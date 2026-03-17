package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.util.MathUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ListValueRangeStatistics<Solution_> {
    @Nullable
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final ValueRangeManager<Solution_> valueRangeManager;

    public ListValueRangeStatistics(@Nullable ListVariableDescriptor<Solution_> listVariableDescriptor,
            ValueRangeManager<Solution_> valueRangeManager) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.valueRangeManager = valueRangeManager;
    }

    public long computeListProblemScaleLog(long logBase) {
        if (listVariableDescriptor == null) {
            // No list variable
            return 0L;
        }
        var allowsUnassignedValues = listVariableDescriptor.allowsUnassignedValues();
        var reachableValues = valueRangeManager.getReachableValues(listVariableDescriptor);
        var entityCount = reachableValues.extractAllEntitiesAsSet().size();
        if (entityCount == 0) {
            // No entities
            return 0L;
        }
        if (allowsUnassignedValues) {
            // Unassigned values are treated as if they are assigned to a virtual entity to simplify calculations
            entityCount++;
        }

        var valueSet = reachableValues.extractAllValuesAsSet();
        var valueCount = valueSet.size();
        var validPercentageLog = 0L;
        var additionalCount = allowsUnassignedValues ? 1 : 0;

        for (var value : valueSet) {
            validPercentageLog += MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, logBase,
                    ((double) reachableValues.getReachableEntitiesSize(value) + additionalCount) / entityCount);
        }

        return MathUtils.getPossibleArrangementsScaledApproximateLog(MathUtils.LOG_PRECISION, logBase,
                valueCount, entityCount) + validPercentageLog;
    }
}
