package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;

/**
 * Flips a sublist of a list variable, (the same thing as a {@link TwoOptListMove}, but no shift to restore the original
 * origin).
 * For instance, given [0, 1, 2, 3, 4], fromIndexInclusive = 1, toIndexExclusive = 3,
 * the list after the move would be [0, 3, 2, 1, 4].
 * If toIndexExclusive is before fromIndexInclusive,
 * the flip is performed on the combined sublists [fromIndexInclusive, size) and [0, toIndexExclusive).
 * For instance, given [0, 1, 2, 3, 4, 5, 6], fromIndexInclusive = 5, toIndexExclusive = 2,
 * the list after the move would be [6, 5, 2, 3, 4, 1, 0] (and not [0, 6, 5, 2, 3, 4, 1]).
 */
record FlipSublistAction(ListVariableDescriptor<?> variableDescriptor,
        int fromIndexInclusive, int toIndexExclusive) {

    FlipSublistAction createUndoMove() {
        return new FlipSublistAction(variableDescriptor, fromIndexInclusive, toIndexExclusive);
    }

    public KOptAffectedElements getAffectedElements() {
        if (fromIndexInclusive < toIndexExclusive) {
            return KOptAffectedElements.forMiddleRange(fromIndexInclusive, toIndexExclusive);
        } else {
            return KOptAffectedElements.forWrappedRange(fromIndexInclusive, toIndexExclusive);
        }
    }

    void doMoveOnGenuineVariables(MultipleDelegateList<?> combinedList) {
        flipSublist(combinedList, 0, fromIndexInclusive, toIndexExclusive);
    }

    public FlipSublistAction rebase() {
        return new FlipSublistAction(variableDescriptor,
                fromIndexInclusive,
                toIndexExclusive);
    }

    public static <T> void flipSublist(List<T> originalList, int entityFirstUnpinnedIndex, int fromIndexInclusive,
            int toIndexExclusive) {
        if (fromIndexInclusive < toIndexExclusive) {
            Collections.reverse(originalList.subList(fromIndexInclusive, toIndexExclusive));
        } else {
            List<T> firstHalfReversedPath = originalList.subList(fromIndexInclusive, originalList.size());
            List<T> secondHalfReversedPath = originalList.subList(entityFirstUnpinnedIndex, toIndexExclusive);

            // Reverse the combined list firstHalfReversedPath + secondHalfReversedPath
            // For instance, (1, 2, 3)(4, 5, 6, 7, 8, 9) becomes
            // (9, 8, 7)(6, 5, 4, 3, 2, 1)
            int totalLength = firstHalfReversedPath.size() + secondHalfReversedPath.size();

            for (int i = 0; (i < totalLength >> 1); i++) {
                if (i < firstHalfReversedPath.size()) {
                    if (i < secondHalfReversedPath.size()) {
                        // firstHalfIndex = i
                        int secondHalfIndex = secondHalfReversedPath.size() - i - 1;
                        T savedFirstItem = firstHalfReversedPath.get(i);
                        firstHalfReversedPath.set(i, secondHalfReversedPath.get(secondHalfIndex));
                        secondHalfReversedPath.set(secondHalfIndex, savedFirstItem);
                    } else {
                        // firstIndex = i
                        int secondIndex = firstHalfReversedPath.size() - i + secondHalfReversedPath.size() - 1;
                        T savedFirstItem = firstHalfReversedPath.get(i);
                        firstHalfReversedPath.set(i, firstHalfReversedPath.get(secondIndex));
                        firstHalfReversedPath.set(secondIndex, savedFirstItem);
                    }
                } else {
                    int firstIndex = i - firstHalfReversedPath.size();
                    int secondIndex = secondHalfReversedPath.size() - i - 1;
                    T savedFirstItem = secondHalfReversedPath.get(firstIndex);
                    secondHalfReversedPath.set(firstIndex, secondHalfReversedPath.get(secondIndex));
                    secondHalfReversedPath.set(secondIndex, savedFirstItem);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "FlipSublistAction(from=" + fromIndexInclusive + ", to=" + toIndexExclusive + ")";
    }
}
