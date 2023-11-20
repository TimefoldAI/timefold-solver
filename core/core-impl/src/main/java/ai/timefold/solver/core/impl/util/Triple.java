package ai.timefold.solver.core.impl.util;

/**
 * An immutable tuple of three values.
 * Two instances {@link Object#equals(Object) are equal} if all three values in the first instance
 * are equal to their counterpart in the other instance.
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
public record Triple<A, B, C>(A a, B b, C c) {

}
