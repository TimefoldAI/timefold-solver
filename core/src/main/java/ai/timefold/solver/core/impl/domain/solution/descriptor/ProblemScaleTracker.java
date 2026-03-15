package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.score.director.ListValueRangeStatistics;
import ai.timefold.solver.core.impl.util.MathUtils;

public class ProblemScaleTracker {
    private final long logBase;
    private final Set<Object> visitedAnchorSet = Collections.newSetFromMap(new IdentityHashMap<>());
    private final ListValueRangeStatistics listValueRangeStatistics = new ListValueRangeStatistics();
    private long basicProblemScaleLog = 0L;
    private boolean listAllowsUnassignedValues = false;

    public ProblemScaleTracker(long logBase) {
        this.logBase = logBase;
    }

    // Simple getters
    public long getProblemScaleLog() {
        return basicProblemScaleLog + listValueRangeStatistics.computeListProblemScaleLog(listAllowsUnassignedValues, logBase);
    }

    public void processListValueRange(boolean listAllowsUnassignedValues, ValueRange<?> valueRange) {
        this.listAllowsUnassignedValues |= listAllowsUnassignedValues;
        listValueRangeStatistics.addValueRange(valueRange);
    }

    public void addBasicProblemScale(long count) {
        if (count == 0) {
            // Log(0) = -infinity; also an invalid problem (since there are no
            // valid values for the variable, including null)
            return;
        }
        basicProblemScaleLog += MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, logBase, count);
    }
}
