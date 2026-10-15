package ai.timefold.solver.core.impl.util;

import static ai.timefold.solver.core.impl.util.TriangularNumbers.nthTriangle;
import static ai.timefold.solver.core.impl.util.TriangularNumbers.triangularRoot;

import java.util.random.RandomGenerator;

public final class TriangleElementFactory {

    private final int minimumSubListSize;
    private final int maximumSubListSize;
    private final RandomGenerator workingRandom;

    /**
     * @throws IllegalArgumentException as defined by {@link #validateSizes(int, int)}
     */
    public TriangleElementFactory(int minimumSubListSize, int maximumSubListSize, RandomGenerator workingRandom) {
        validateSizes(minimumSubListSize, maximumSubListSize);
        this.minimumSubListSize = minimumSubListSize;
        this.maximumSubListSize = maximumSubListSize;
        this.workingRandom = workingRandom;
    }

    /**
     * Validates a {@code (minimumSubListSize, maximumSubListSize)} pair without needing a {@link RandomGenerator},
     * so that a caller which only creates a {@link TriangleElementFactory} later
     * (once a {@link RandomGenerator} becomes available) can still fail fast at construction time.
     *
     * @throws IllegalArgumentException if {@code minimumSubListSize > maximumSubListSize},
     *         or if {@code minimumSubListSize < 1}
     */
    public static void validateSizes(int minimumSubListSize, int maximumSubListSize) {
        if (minimumSubListSize > maximumSubListSize) {
            throw new IllegalArgumentException(
                    "The minimumSubListSize (%d) must be less than or equal to the maximumSubListSize (%d)."
                            .formatted(minimumSubListSize, maximumSubListSize));
        }
        if (minimumSubListSize < 1) {
            throw new IllegalArgumentException(
                    "The minimumSubListSize (%d) must be greater than 0."
                            .formatted(minimumSubListSize));
        }
    }

    /**
     * As {@link #validateSizes(int, int)}, plus a caller-specific floor above the general minimum of 1 -
     * for example, a move type for which a span of size 1 would be redundant with a simpler move provider.
     *
     * @throws IllegalArgumentException as defined by {@link #validateSizes(int, int)},
     *         or if {@code minimumSubListSize < minimumAllowedSubListSize}
     */
    public static void validateSizes(int minimumSubListSize, int maximumSubListSize, int minimumAllowedSubListSize) {
        validateSizes(minimumSubListSize, maximumSubListSize);
        if (minimumSubListSize < minimumAllowedSubListSize) {
            throw new IllegalArgumentException(
                    "The minimumSubListSize (%d) must be at least %d."
                            .formatted(minimumSubListSize, minimumAllowedSubListSize));
        }
    }

    /**
     * Produce next random element of Triangle(listSize) observing the given minimum and maximum subList size.
     *
     * @param listSize determines the Triangle to select an element from
     * @return next random triangle element
     * @throws IllegalArgumentException if {@code listSize} is less than {@code minimumSubListSize}
     */
    public TriangleElement nextElement(int listSize) throws IllegalArgumentException {
        if (listSize < minimumSubListSize) {
            throw new IllegalArgumentException(
                    "The listSize (%d) must be at least the minimumSubListSize (%d)."
                            .formatted(listSize, minimumSubListSize));
        }
        // Reduce the triangle base by the minimum subList size.
        var subListCount = nthTriangle(listSize - minimumSubListSize + 1);
        // The top triangle represents all subLists of size greater or equal to maximum subList size. Remove them all.
        var topTriangleSize = listSize <= maximumSubListSize ? 0 : nthTriangle(listSize - maximumSubListSize);
        // Triangle elements are indexed from 1.
        var subListIndex = workingRandom.nextInt(subListCount - topTriangleSize) + topTriangleSize + 1;
        return TriangleElement.valueOf(subListIndex);
    }

    public record TriangleElement(int index, int level, int indexOnLevel) {

        static TriangleElement valueOf(int index) {
            var level = (int) Math.ceil(triangularRoot(index));
            return new TriangleElement(index, level, index - nthTriangle(level - 1));
        }

    }
}
