package ai.timefold.solver.core.impl.solver.random;

import java.util.random.RandomGenerator;

public final class RandomUtils {

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
     * Return a value between 0 and {@code distribution.length}, with indices with larger
     * values in {@code distribution} being more likely.
     *
     * @param random The {@link RandomGenerator} to use.
     * @param distributionSum Sum of all values in {@code distribution}. Must be positive.
     * @param distribution Relative weight of the index being chosen. If one index's value is twice another index,
     *        that index is twice as likely to be chosen. Each value must be non-negative (0 is allowed).
     * @return An index between 0 and {@code distribution.length}, biased towards indices in distribution with larger values.
     */
    public static int sampleWithDistribution(RandomGenerator random, int distributionSum,
            int[] distribution) {
        var choice = random.nextInt(distributionSum);
        var index = 0;
        while (distribution[index] < choice) {
            choice -= distribution[index];
            index++;
        }
        return index;
    }

    private RandomUtils() {
    }

}
