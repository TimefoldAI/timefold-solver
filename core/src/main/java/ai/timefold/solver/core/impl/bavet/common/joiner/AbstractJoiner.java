package ai.timefold.solver.core.impl.bavet.common.joiner;

import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

public abstract class AbstractJoiner<Right_> {

    protected final Function<Right_, Object>[] rightMappings;
    protected final JoinerType[] joinerTypes;

    protected AbstractJoiner(Function<Right_, ?> rightMapping, JoinerType joinerType) {
        this(new Function[] { rightMapping }, new JoinerType[] { joinerType });
    }

    protected AbstractJoiner(Function<Right_, ?>[] rightMappings, JoinerType[] joinerTypes) {
        this.rightMappings = (Function<Right_, Object>[]) Objects.requireNonNull(rightMappings);
        this.joinerTypes = Objects.requireNonNull(joinerTypes);
    }

    public final Function<Right_, Object> getRightMapping(int index) {
        return rightMappings[index];
    }

    public final int getJoinerCount() {
        return joinerTypes.length;
    }

    public final JoinerType getJoinerType(int index) {
        return joinerTypes[index];
    }

    /**
     * Computes a stable permutation that moves all {@link JoinerType#EQUAL} joiners to the front,
     * preserving the original relative order within the equal group and within the rest.
     * Subclasses use it in {@code reorderedEqualsFirst()} so the comber can emit an equal-first
     * merged joiner; reordering ANDed joiners cannot change which pairs match.
     *
     * @param joinerTypes the joiner types in declared order
     * @return the index order to apply, or null when no move is needed (≤ 1 joiner, or already
     *         equal-first) so the caller can keep the joiner as-is and skip allocation
     */
    protected static int @Nullable [] equalsFirstOrder(JoinerType[] joinerTypes) {
        var count = joinerTypes.length;
        if (count <= 1) {
            return null;
        }
        var seenNonEqual = false;
        for (var i = 0; i < count; i++) {
            if (joinerTypes[i] == JoinerType.EQUAL) {
                if (seenNonEqual) { // An equal joiner follows a non-equal one, so a move is needed.
                    return buildEqualsFirstOrder(joinerTypes, count);
                }
            } else {
                seenNonEqual = true;
            }
        }
        return null; // Already equal-first.
    }

    private static int[] buildEqualsFirstOrder(JoinerType[] joinerTypes, int count) {
        var order = new int[count];
        var index = 0;
        for (var i = 0; i < count; i++) { // Equal joiners first, in their original relative order.
            if (joinerTypes[i] == JoinerType.EQUAL) {
                order[index++] = i;
            }
        }
        for (var i = 0; i < count; i++) { // Then the rest, in their original relative order.
            if (joinerTypes[i] != JoinerType.EQUAL) {
                order[index++] = i;
            }
        }
        return order;
    }

}
