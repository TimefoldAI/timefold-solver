package ai.timefold.solver.core.impl.util;

import org.apache.commons.math3.util.CombinatoricsUtils;

public class MathUtils {
    private MathUtils() {
    }

    public static long getPossibleArrangementsScaledApproximateLog(long scale, long base,
            int listSize, int partitions) {
        // N! = number of ways to arrange a list of N values
        double possibleListArrangementsLog = CombinatoricsUtils.factorialLog(listSize);
        // A*B = number of ways to place A markers on a list of B elements
        double possibleMarkerArrangementsLog = Math.log(listSize * partitions);
        // E! = number of ways to arrange E partitions
        double possiblePartitionPermutationsLog = CombinatoricsUtils.factorialLog(partitions);

        // The number of possible assignments for a list variable with V values among E entities
        // is approximated by
        // `(V! * (V * E))/E!`
        // log(a) + log(b) = log(a * b)
        // log(a) - log(b) = log(a / b)
        double totalPossibleValueAssignmentsLog =
                possibleListArrangementsLog + possibleMarkerArrangementsLog - possiblePartitionPermutationsLog;

        // Need to change base of log to use logBase
        double totalPossibleValueAssignmentsLogInBase = totalPossibleValueAssignmentsLog / Math.log(base);
        return Math.round(scale * totalPossibleValueAssignmentsLogInBase);
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
