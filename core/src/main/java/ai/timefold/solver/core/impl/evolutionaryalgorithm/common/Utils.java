package ai.timefold.solver.core.impl.evolutionaryalgorithm.common;

import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;

public final class Utils {

    private Utils() {
        // No external instances.
    }

    /**
     * Generate a cut-point and ensure that the expected number of planning values is included in the interval.
     */
    public static int[] generateIndexes(RandomGenerator workingRandom, int size, double inheritanceRate) {
        var minSize = (int) (size * inheritanceRate);
        if (minSize == 0) {
            return generateIndexes(workingRandom, size);
        }
        var maxStart = size - minSize + 1;
        var start = workingRandom.nextInt(0, maxStart);
        var minEnd = start + minSize - 1;
        var maxEnd = start == 0 ? size - 1 : size;
        var end = start == maxStart - 1 ? size - 1 : workingRandom.nextInt(minEnd, maxEnd);
        return new int[] { start, end };
    }

    /**
     * Differ from {@link #generateIndexes(RandomGenerator, int, double)} as it generates a cut-point without a minimum number
     * of values that the interval must contain.
     */
    public static int[] generateIndexes(RandomGenerator workingRandom, int size) {
        var start = workingRandom.nextInt(size);
        var end = workingRandom.nextInt(size);
        while (start == end) {
            end = workingRandom.nextInt(size);
        }
        if (start > end) {
            var newEndIdx = start;
            start = end;
            end = newEndIdx;
        }
        // Avoid copying all values from the first individual (only enforceable when size > 2)
        if (start == 0 && end == size - 1 && size > 2) {
            // Pick a new end index in [1, size-2] to leave at least one value from the second parent
            end = 1 + workingRandom.nextInt(size - 2);
        }
        return new int[] { start, end };
    }

    /**
     * The method adjusts the position to encompass all values assigned to the target entity and avoids breaking constraints
     * that require groups of values to be assigned together.
     */
    public static int fixIndex(ChromosomeEntry[] chromosome, int position, boolean backward) {
        int index;
        var target = chromosome[position].entity();
        if (backward) {
            index = position - 1;
            while (index >= 0 && chromosome[index].entity() == target) {
                index--;
            }
            // We need to increment because the starting position is inclusive in the interval [index, end].
            index++;
        } else {
            index = position + 1;
            while (index < chromosome.length && chromosome[index].entity() == target) {
                index++;
            }
        }
        return index;
    }
}
