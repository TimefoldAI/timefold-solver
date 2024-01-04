package ai.timefold.solver.core.impl.util;

import java.util.Objects;

/**
 * An immutable tuple of three values.
 * Two instances {@link Object#equals(Object) are equal} if all three values in the first instance
 * are equal to their counterpart in the other instance.
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
public class Triple<A, B, C> {
    private final A a;
    private final B b;
    private final C c;

    public Triple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return Objects.equals(a, triple.a) && Objects.equals(b, triple.b) && Objects.equals(c,
                triple.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c);
    }
}
