package ai.timefold.solver.core.impl.solver.random;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

public class RandomUtils {

    /**
     * Mimics {@link java.util.Random#nextInt(int)} for longs.
     *
     * @param random never null
     * @param n {@code > 0L}
     * @return like {@link java.util.Random#nextInt(int)} but for a long
     * @see java.util.Random#nextInt(int)
     */
    public static long nextLong(RandomGenerator random, long n) {
        // This code is based on java.util.Random#nextInt(int)'s javadoc.
        if (n <= 0L) {
            throw new IllegalArgumentException("n must be positive");
        }
        if (n < Integer.MAX_VALUE) {
            return random.nextInt((int) n);
        }

        long bits;
        long val;
        do {
            bits = (random.nextLong() << 1) >>> 1;
            val = bits % n;
        } while (bits - val + (n - 1L) < 0L);
        return val;
    }

    /**
     * Mimics {@link java.util.Random#nextInt(int)} for doubles.
     *
     * @param random never null
     * @param n {@code > 0.0}
     * @return like {@link java.util.Random#nextInt(int)} but for a double
     * @see java.util.Random#nextInt(int)
     */
    public static double nextDouble(RandomGenerator random, double n) {
        // This code is based on java.util.Random#nextInt(int)'s javadoc.
        if (n <= 0.0) {
            throw new IllegalArgumentException("n must be positive");
        }
        return random.nextDouble() * n;
    }

    /**
     * Implements {@link java.util.Collections#shuffle(List, Random)} for {@link RandomGenerator}.
     * There is a {@link RandomGenerator} overload for shuffle in JDK 21, but not JDK 17.
     * TODO: Remove me when Minimum JDK 21
     */
    public static <Item_> void shuffle(List<Item_> shuffledList, RandomGenerator random) {
        var listSize = shuffledList.size();
        for (var rightIndex = listSize - 1; rightIndex > 0; rightIndex--) {
            var leftIndex = random.nextInt(rightIndex + 1); // leftIndex <= rightIndex
            var leftValue = shuffledList.get(leftIndex);
            var rightValue = shuffledList.set(rightIndex, leftValue);
            shuffledList.set(leftIndex, rightValue);
        }
    }

    private RandomUtils() {
    }

}
