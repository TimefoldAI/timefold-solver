package ai.timefold.solver.core.api.function;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;

/**
 * Represents a function that accepts four arguments and returns no result.
 * This is the three-arity specialization of {@link Consumer}.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(Object, Object, Object, Object)}.
 *
 * @param <A> the type of the first argument to the function
 * @param <B> the type of the second argument to the function
 * @param <C> the type of the third argument to the function
 * @param <D> the type of the fourth argument to the function
 *
 * @see Function
 */
@FunctionalInterface
public interface QuadConsumer<A, B, C, D> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @param d the fourth function argument
     */
    void accept(A a, B b, C c, D d);

    default @NonNull QuadConsumer<A, B, C, D> andThen(@NonNull QuadConsumer<? super A, ? super B, ? super C, ? super D> after) {
        Objects.requireNonNull(after);
        return (a, b, c, d) -> {
            accept(a, b, c, d);
            after.accept(a, b, c, d);
        };
    }

}
