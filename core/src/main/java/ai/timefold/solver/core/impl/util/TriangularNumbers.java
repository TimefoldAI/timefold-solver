package ai.timefold.solver.core.impl.util;

public final class TriangularNumbers {

    /**
     * Calculate <em>n</em>th <a href="https://en.wikipedia.org/wiki/Triangular_number">triangular number</a>.
     * This is used to calculate the number of subLists for a given list variable of size <em>n</em>.
     *
     * @param n size of the triangle (the length of its side)
     * @return <em>n</em>th triangular number
     * @throws ArithmeticException if the result does not fit in a long; never for an int {@code n}
     */
    public static long nthTriangle(long n) throws ArithmeticException {
        return Math.multiplyExact(n, n + 1) / 2;
    }

    static double triangularRoot(long x) {
        double d = 8.0 * x + 1;
        return (Math.sqrt(d) - 1) / 2;
    }

    private TriangularNumbers() {
    }
}
