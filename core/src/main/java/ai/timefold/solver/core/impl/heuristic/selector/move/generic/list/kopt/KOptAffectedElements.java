package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.impl.util.CollectionUtils;

record KOptAffectedElements(int wrappedStartIndex, int wrappedEndIndex, List<Range> affectedMiddleRangeList) {

    static KOptAffectedElements forMiddleRange(int startInclusive, int endExclusive) {
        return new KOptAffectedElements(-1, -1, List.of(new Range(startInclusive, endExclusive)));
    }

    static KOptAffectedElements forWrappedRange(int startInclusive, int endExclusive) {
        return new KOptAffectedElements(startInclusive, endExclusive, Collections.emptyList());
    }

    public KOptAffectedElements merge(KOptAffectedElements other) {
        var newWrappedStartIndex = this.wrappedStartIndex;
        var newWrappedEndIndex = this.wrappedEndIndex;

        if (other.wrappedStartIndex != -1) {
            if (newWrappedStartIndex != -1) {
                newWrappedStartIndex = Math.min(other.wrappedStartIndex, newWrappedStartIndex);
                newWrappedEndIndex = Math.max(other.wrappedEndIndex, newWrappedEndIndex);
            } else {
                newWrappedStartIndex = other.wrappedStartIndex;
                newWrappedEndIndex = other.wrappedEndIndex;
            }
        }

        var newAffectedMiddleRangeList = CollectionUtils.concat(affectedMiddleRangeList, other.affectedMiddleRangeList);

        boolean removedAny;
        SearchForIntersectingRange: do {
            removedAny = false;
            final var listSize = newAffectedMiddleRangeList.size();
            for (var i = 0; i < listSize; i++) {
                for (var j = i + 1; j < listSize; j++) {
                    var leftRange = newAffectedMiddleRangeList.get(i);
                    var rightRange = newAffectedMiddleRangeList.get(j);

                    if (leftRange.startInclusive() <= rightRange.endExclusive()) {
                        if (rightRange.startInclusive() <= leftRange.endExclusive()) {
                            var mergedRange =
                                    new Range(Math.min(leftRange.startInclusive(), rightRange.startInclusive()),
                                            Math.max(leftRange.endExclusive(), rightRange.endExclusive()));
                            newAffectedMiddleRangeList.set(i, mergedRange);
                            newAffectedMiddleRangeList.remove(j);
                            removedAny = true;
                            continue SearchForIntersectingRange;
                        }
                    }
                }
            }
        } while (removedAny);

        return new KOptAffectedElements(newWrappedStartIndex, newWrappedEndIndex, newAffectedMiddleRangeList);
    }

    public record Range(int startInclusive, int endExclusive) {

    }

}
