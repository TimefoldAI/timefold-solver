package ai.timefold.solver.core.impl.util;

/**
 * An immutable tuple of four values.
 * Two instances {@link Object#equals(Object) are equal} if all four values in the first instance
 * are equal to their counterpart in the other instance.
 *
 * @param <A>
 * @param <B>
 * @param <C>
 * @param <D>
 */
public record Quadruple<A, B, C, D>(A a, B b, C c, D d) {

    @Override
    public boolean equals(Object o) {
        // We do not use Objects.equals(...) due to https://bugs.openjdk.org/browse/JDK-8015417.
        if (this == o) {
            return true;
        }
        return o instanceof Quadruple<?, ?, ?, ?> that &&
                ((a == that.a) || (a != null && a.equals(that.a))) &&
                ((b == that.b) || (b != null && b.equals(that.b))) &&
                ((c == that.c) || (c != null && c.equals(that.c))) &&
                ((d == that.d) || (d != null && d.equals(that.d)));
    }

    @Override
    public int hashCode() {
        // Often used in hash-based collections; the JDK-generated default is too slow.
        // We do not use Objects.hash(...) because it creates an array each time.
        // We do not use Objects.hashCode() due to https://bugs.openjdk.org/browse/JDK-8015417.
        var hash = 1;
        hash = 31 * hash + (a == null ? 0 : a.hashCode());
        hash = 31 * hash + (b == null ? 0 : b.hashCode());
        hash = 31 * hash + (c == null ? 0 : c.hashCode());
        return 31 * hash + (d == null ? 0 : d.hashCode());
        return hash;
    }

}
