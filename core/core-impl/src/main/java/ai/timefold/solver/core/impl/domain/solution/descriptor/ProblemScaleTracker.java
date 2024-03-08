package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import ai.timefold.solver.core.impl.util.MathUtils;

public class ProblemScaleTracker {
    private final long logBase;
    private final Set<Object> visitedAnchorSet = Collections.newSetFromMap(new IdentityHashMap<>());

    private long basicProblemScaleLog = 0L;
    private int listPinnedValueCount = 0;
    private int listTotalEntityCount = 0;
    private int listMovableEntityCount = 0;
    private int listTotalValueCount = 0;

    public ProblemScaleTracker(long logBase) {
        this.logBase = logBase;
    }

    // Simple getters
    public long getBasicProblemScaleLog() {
        return basicProblemScaleLog;
    }

    public int getListPinnedValueCount() {
        return listPinnedValueCount;
    }

    public int getListTotalEntityCount() {
        return listTotalEntityCount;
    }

    public int getListMovableEntityCount() {
        return listMovableEntityCount;
    }

    public int getListTotalValueCount() {
        return listTotalValueCount;
    }

    public void setListTotalValueCount(int listTotalValueCount) {
        this.listTotalValueCount = listTotalValueCount;
    }

    // Complex methods
    public boolean isAnchorVisited(Object anchor) {
        if (visitedAnchorSet.contains(anchor)) {
            return true;
        }
        visitedAnchorSet.add(anchor);
        return false;
    }

    public void addListValueCount(int count) {
        listTotalValueCount += count;
    }

    public void addPinnedListValueCount(int count) {
        listPinnedValueCount += count;
    }

    public void incrementListEntityCount(boolean isMovable) {
        listTotalEntityCount++;
        if (isMovable) {
            listMovableEntityCount++;
        }
    }

    public void addBasicProblemScale(long count) {
        basicProblemScaleLog += MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, logBase, count);
    }
}
