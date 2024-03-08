package ai.timefold.solver.core.impl.util;

import org.apache.commons.math3.util.CombinatoricsUtils;

public class MathUtils {
    public static final long LOG_PRECISION = 1_000_000L;

    private MathUtils() {
    }

    public static long getPossibleArrangementsScaledApproximateLog(long scale, long base,
            int listSize, int partitions) {
        double result;
        if (listSize == 0 || partitions == 0) {
            // Only one way to divide an empty list, and the log of 1 is 0
            // Likewise, there is only 1 way to divide a list into 0 partitions
            // (since it impossible to do)
            result = 0L;
        } else if (partitions <= 2) {
            // If it a single partition, it the same as the number of permutations.
            // If it two partitions, it the same as the number of permutations of a list of size
            // n + 1 (where we add an element to seperate the two partitions)
            result = CombinatoricsUtils.factorialLog(listSize + partitions - 1);
        } else {
            // If it n > 2 partitions, (listSize + partitions - 1)! will overcount by
            // a multiple of (partitions - 1)!
            result = CombinatoricsUtils.factorialLog(listSize + partitions - 1)
                    - CombinatoricsUtils.factorialLog(partitions - 1);
        }

        // Need to change base to use the given base
        return Math.round(scale * result / Math.log(base));
    }

    /**
     * Returns a scaled approximation of a log
     * 
     * @param scale What to scale the result by. Typically, a power of 10.
     * @param base The base of the log
     * @param value The parameter to the log function
     * @return A value approximately equal to {@code scale * log_base(value)}, rounded
     *         to the nearest integer.
     */
    public static long getScaledApproximateLog(long scale, long base, long value) {
        return Math.round(scale * getLogInBase(base, value));
    }

    public static double getLogInBase(double base, double value) {
        return Math.log(value) / Math.log(base);
    }
}
