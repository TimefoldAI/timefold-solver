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
public class Quadruple<A, B, C, D> {
    private final A a;
    private final B b;
    private final C c;
    private final D d;

    public Quadruple(A a, B b, C c, D d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public A a() {
        return a;
    }

    public B b() {
        return b;
    }

    public C c() {
        return c;
    }

    public D d() {
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Quadruple<?, ?, ?, ?> quadruple = (Quadruple<?, ?, ?, ?>) o;
        return Objects.equals(a, quadruple.a) && Objects.equals(b, quadruple.b) && Objects.equals(c,
                quadruple.c) && Objects.equals(d, quadruple.d);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c, d);
    }
}
