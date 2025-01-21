package ai.timefold.solver.core.impl.util;

import java.util.Objects;

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

    public Quadruple<A, B, C, D> newIfDifferent(A newA, B newB, C newC, D newD) {
        return Objects.equals(a, newA) && Objects.equals(b, newB) && Objects.equals(c, newC) && Objects.equals(d, newD) ? this
                : new Quadruple<>(newA, newB, newC, newD);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Quadruple<?, ?, ?, ?> other &&
                Objects.equals(a, other.a) &&
                Objects.equals(b, other.b) &&
                Objects.equals(c, other.c) &&
                Objects.equals(d, other.d);
    }

    @Override
    public int hashCode() {
        var hash = 1;
        hash = 31 * hash + Objects.hashCode(a);
        hash = 31 * hash + Objects.hashCode(b);
        hash = 31 * hash + Objects.hashCode(c);
        hash = 31 * hash + Objects.hashCode(d);
        return hash;
    }

}
