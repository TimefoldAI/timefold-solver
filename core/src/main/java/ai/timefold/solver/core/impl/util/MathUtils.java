package ai.timefold.solver.core.impl.util;

/**
 * Includes code taken from Apache's commons-math library, v3.6.1 (Apache 2.0-licensed)
 * These methods are clearly marked in comments, their modifications listed.
 */
public class MathUtils {

    public static final long LOG_PRECISION = 1_000_000L;
    private static final int FACTORIAL_MAX_N = 20;
    private static final long[] FACTORIAL_CACHE = new long[FACTORIAL_MAX_N - 1]; // 0 and 1 are hard-coded.

    private MathUtils() {
    }

    public static long getPossibleArrangementsScaledApproximateLog(long scale, long base, int listSize, int partitions) {
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
            result = factorialLog(listSize + partitions - 1);
        } else {
            // If it n > 2 partitions, (listSize + partitions - 1)! will overcount by
            // a multiple of (partitions - 1)!
            result = factorialLog(listSize + partitions - 1) - factorialLog(partitions - 1);
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

    /**
     * Returns a scaled approximation of a log.
     *
     * @param scale What to scale the result by. Typically, a power of 10.
     * @param base The base of the log
     * @param value The parameter to the log function
     * @return A value approximately equal to {@code scale * log_base(value)}, rounded to the nearest integer.
     */
    public static long getScaledApproximateLog(long scale, long base, double value) {
        return Math.round(scale * getLogInBase(base, value));
    }

    public static double getLogInBase(double base, double value) {
        return Math.log(value) / Math.log(base);
    }

    public static long getSpeed(long count, long timeMillisSpent) {
        // Avoid divide by zero exception on a fast CPU
        return count * 1000L / (timeMillisSpent == 0L ? 1L : timeMillisSpent);
    }

    public static long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Factorial of %d is undefined."
                    .formatted(n));
        } else if (n > FACTORIAL_MAX_N) {
            throw new IllegalArgumentException("Factorial of %d is too large to fit in a long."
                    .formatted(n));
        }
        return switch (n) {
            case 0, 1 -> 1;
            default -> {
                var factorialCacheIndex = n - 2; // n={0,1} are already handled above.
                var factorial = FACTORIAL_CACHE[factorialCacheIndex];
                if (factorial != 0L) {
                    yield factorial;
                }
                yield FACTORIAL_CACHE[factorialCacheIndex] = n * factorial(n - 1);
            }
        };
    }

    private static double factorialLog(int n) {
        if (n <= FACTORIAL_MAX_N) {
            return Math.log(factorial(n));
        }
        var logSum = 0.0d;
        for (var i = 2; i <= n; i++) {
            logSum += Math.log(i);
        }
        return logSum;
    }

    /**
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "{@code n choose k}", the number of
     * {@code k}-element subsets that can be selected from an
     * {@code n}-element set.
     * <p>
     * <strong>Preconditions</strong>:
     * <ul>
     * <li>{@code 0 <= k <= n } (otherwise {@link IllegalArgumentException} is thrown)</li>
     * <li>The result is small enough to fit into a {@code long}.
     * The largest value of {@code n} for which all coefficients are less than {@code Long.MAX_VALUE} is 66.
     * If the computed value exceeds {@code Long.MAX_VALUE} a {@link ArithmeticException} is thrown.</li>
     * </ul>
     * Taken from `commons-math` 3.6.1 (Apache 2.0 licensed.)
     * Refactored to not depend on any other `commons-math` methods or types.
     *
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @return {@code n choose k} represented by a long integer.
     */
    public static long binomialCoefficient(final int n, final int k) {
        if (n < k) {
            throw new IllegalArgumentException("must have n >= k for binomial coefficient (n, k), got k = %d, n = %d"
                    .formatted(k, n));
        }
        if (n < 0) {
            throw new IllegalArgumentException("must have n >= 0 for binomial coefficient (n, k), got n = %d".formatted(n));
        }
        if ((n == k) || (k == 0)) {
            return 1;
        }
        if ((k == 1) || (k == n - 1)) {
            return n;
        }
        // Use symmetry for large k
        if (k > n / 2) {
            return binomialCoefficient(n, n - k);
        }

        // We use the formula
        // (n choose k) = n! / (n-k)! / k!
        // (n choose k) == ((n-k+1)*...*n) / (1*...*k)
        // which could be written
        // (n choose k) == (n-1 choose k-1) * n / k
        long result = 1;
        if (n <= 61) {
            // For n <= 61, the naive implementation cannot overflow.
            var i = n - k + 1;
            for (var j = 1; j <= k; j++) {
                result = result * i / j;
                i++;
            }
        } else if (n <= 66) {
            // For n > 61 but n <= 66, the result cannot overflow,
            // but we must take care not to overflow intermediate values.
            var i = n - k + 1;
            for (var j = 1; j <= k; j++) {
                // We know that (result * i) is divisible by j,
                // but (result * i) may overflow, so we split j:
                // Filter out the gcd, d, so j/d and i/d are integer.
                // result is divisible by (j/d) because (j/d)
                // is relative prime to (i/d) and is a divisor of
                // result * (i/d).
                final long d = gcd(i, j);
                result = (result / (j / d)) * (i / d);
                i++;
            }
        } else {
            // For n > 66, a result overflow might occur, so we check
            // the multiplication, taking care to not overflow
            // unnecessary.
            var i = n - k + 1;
            for (var j = 1; j <= k; j++) {
                final long d = gcd(i, j);
                result = mulAndCheck(result / (j / d), i / d);
                i++;
            }
        }
        return result;
    }

    /**
     * Taken from `commons-math` 3.6.1 (Apache 2.0 licensed.)
     * Refactored to not depend on any other `commons-math` methods or types,
     * replaced local variable types with inference.
     */
    private static int gcd(int p, int q) {
        var a = p;
        var b = q;
        if (a == 0 || b == 0) {
            if (a == Integer.MIN_VALUE || b == Integer.MIN_VALUE) {
                throw new ArithmeticException("overflow: gcd(%d, %d) is 2^31"
                        .formatted(p, q));
            }
            return Math.abs(a + b);
        }

        long al = a;
        long bl = b;
        var useLong = false;
        if (a < 0) {
            if (Integer.MIN_VALUE == a) {
                useLong = true;
            } else {
                a = -a;
            }
            al = -al;
        }
        if (b < 0) {
            if (Integer.MIN_VALUE == b) {
                useLong = true;
            } else {
                b = -b;
            }
            bl = -bl;
        }
        if (useLong) {
            if (al == bl) {
                throw new ArithmeticException("overflow: gcd(%d, %d) is 2^31"
                        .formatted(p, q));
            }
            var blbu = bl;
            bl = al;
            al = blbu % al;
            if (al == 0) {
                if (bl > Integer.MAX_VALUE) {
                    throw new ArithmeticException("overflow: gcd(%d, %d) is 2^31"
                            .formatted(p, q));
                }
                return (int) bl;
            }
            blbu = bl;

            // Now "al" and "bl" fit in an "int".
            b = (int) al;
            a = (int) (blbu % al);
        }

        return gcdPositive(a, b);
    }

    /**
     * Computes the greatest common divisor of two <em>positive</em> numbers
     * (this precondition is <em>not</em> checked and the result is undefined
     * if not fulfilled) using the "binary gcd" method which avoids division
     * and modulo operations.
     * See Knuth 4.5.2 algorithm B.
     * The algorithm is due to Josef Stein (1961).
     * <br/>
     * Special cases:
     * <ul>
     * <li>The result of {@code gcd(x, x)}, {@code gcd(0, x)} and
     * {@code gcd(x, 0)} is the value of {@code x}.</li>
     * <li>The invocation {@code gcd(0, 0)} is the only one which returns
     * {@code 0}.</li>
     * </ul>
     * <p>
     * Taken from `commons-math` 3.6.1 (Apache 2.0 licensed.)
     * Refactored to not depend on any other `commons-math` methods or types,
     * replaced local variable types with inference.
     *
     * @param a Positive number.
     * @param b Positive number.
     * @return the greatest common divisor.
     */
    private static int gcdPositive(int a, int b) {
        if (a == 0) {
            return b;
        } else if (b == 0) {
            return a;
        }

        // Make "a" and "b" odd, keeping track of common power of 2.
        var aTwos = Integer.numberOfTrailingZeros(a);
        a >>= aTwos;
        var bTwos = Integer.numberOfTrailingZeros(b);
        b >>= bTwos;
        var shift = Math.min(aTwos, bTwos);

        // "a" and "b" are positive.
        // If a > b then "gdc(a, b)" is equal to "gcd(a - b, b)".
        // If a < b then "gcd(a, b)" is equal to "gcd(b - a, a)".
        // Hence, in the successive iterations:
        //  "a" becomes the absolute difference of the current values,
        //  "b" becomes the minimum of the current values.
        while (a != b) {
            var delta = a - b;
            b = Math.min(a, b);
            a = Math.abs(delta);

            // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
            a >>= Integer.numberOfTrailingZeros(a);
        }

        // Recover the common power of 2.
        return a << shift;
    }

    /**
     * Taken from `commons-math` 3.6.1 (Apache 2.0 licensed.)
     * Refactored to not depend on any other `commons-math` methods or types,
     * replaced local variable types with inference and introduced multiple return.
     */
    private static long mulAndCheck(long a, long b) {
        if (a > b) {
            // use symmetry to reduce boundary cases
            return mulAndCheck(b, a);
        } else {
            if (a < 0) {
                if (b < 0) {
                    // check for positive overflow with negative a, negative b
                    if (a >= Long.MAX_VALUE / b) {
                        return a * b;
                    } else {
                        throw new ArithmeticException();
                    }
                } else if (b > 0) {
                    // check for negative overflow with negative a, positive b
                    if (Long.MIN_VALUE / b <= a) {
                        return a * b;
                    } else {
                        throw new ArithmeticException();
                    }
                } else {
                    // assert b == 0
                    return 0;
                }
            } else if (a > 0) {
                // assert a > 0
                // assert b > 0

                // check for positive overflow with positive a, positive b
                if (a <= Long.MAX_VALUE / b) {
                    return a * b;
                } else {
                    throw new ArithmeticException();
                }
            } else {
                // assert a == 0
                return 0;
            }
        }
    }

}
