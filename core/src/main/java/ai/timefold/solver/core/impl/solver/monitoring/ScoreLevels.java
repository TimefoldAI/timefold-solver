package ai.timefold.solver.core.impl.solver.monitoring;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class ScoreLevels {

    final AtomicReference<Number>[] levelValues;
    final AtomicInteger unassignedCount;

    @SuppressWarnings("unchecked")
    ScoreLevels(int unassignedCount, Number[] levelValues) {
        // We store the values inside a constant reference,
        // so that the metric can always load the latest value.
        // If we stored the value directly and just overwrote it,
        // the metric would always hold a reference to the old value,
        // effectively ignoring the update.
        this.unassignedCount = new AtomicInteger(unassignedCount);
        this.levelValues = Arrays.stream(levelValues)
                .map(AtomicReference::new)
                .toArray(AtomicReference[]::new);
    }

    void setLevelValue(int level, Number value) {
        levelValues[level].set(value);
    }

    void setUnassignedCount(int unassignedCount) {
        this.unassignedCount.set(unassignedCount);
    }

}
