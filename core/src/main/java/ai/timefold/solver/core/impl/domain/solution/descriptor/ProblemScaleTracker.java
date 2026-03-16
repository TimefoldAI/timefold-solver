package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.ListValueRangeStatistics;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.util.MathUtils;

public class ProblemScaleTracker<Solution_> {
    private final long logBase;
    private final ListValueRangeStatistics<Solution_> listValueRangeStatistics;
    private long basicProblemScaleLog = 0L;

    public ProblemScaleTracker(ListVariableDescriptor<Solution_> listVariableDescriptor,
            ValueRangeManager<Solution_> valueRangeManager,
            long logBase) {
        this.logBase = logBase;
        this.listValueRangeStatistics = new ListValueRangeStatistics<>(listVariableDescriptor, valueRangeManager);
    }

    // Simple getters
    public long getProblemScaleLog() {
        return basicProblemScaleLog + listValueRangeStatistics.computeListProblemScaleLog(logBase);
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
