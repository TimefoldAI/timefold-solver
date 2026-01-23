package ai.timefold.solver.core.impl.score.stream;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.penta.joiner.DefaultPentaJoiner;
import ai.timefold.solver.core.impl.bavet.quad.joiner.DefaultQuadJoiner;
import ai.timefold.solver.core.impl.bavet.tri.joiner.DefaultTriJoiner;

import org.jspecify.annotations.NonNull;

/**
 * These joiners are not finished because they show score corruptions when used.
 * We merged them anyway, as we will get back to them,
 * but didn't want to have this relatively large PR open indefinitely.
 * TODO once the joiners are finished,
 * move them to {@link ai.timefold.solver.core.api.score.stream.Joiners}
 * and remove this class.
 */
public final class UnfinishedJoiners {

    // ************************************************************************
    // BiJoiner
    // ************************************************************************

    /**
     * Joins every A and B where a value of property on B is contained in the collection of properties on A.
     * <p>
     * For example:
     * <ul>
     * <li>{@code ["A", "B"]} containing {@code "A"} is {@code true}</li>
     * <li>{@code ["A"]} containing {@code "A"} is {@code true}</li>
     * <li>{@code ["X", "Y"]} containing {@code "A"} is {@code false}</li>
     * <li>{@code []} containing {@code "A"} is {@code false}</li>
     * <li>{@code ["A", "B"]} containing {@code null} is {@code false}</li>
     * <li>{@code []} containing {@code null} is {@code false}</li>
     * </ul>
     *
     * @param leftMapping mapping function to apply to A
     * @param rightMapping mapping function to apply to B
     * @param <A> the type of object on the left
     * @param <B> the type of object on the right
     * @param <Property_> the type of the property to compare
     */
    public static <A, B, Property_> @NonNull BiJoiner<A, B> containing(@NonNull Function<A, Collection<Property_>> leftMapping,
            @NonNull Function<B, Property_> rightMapping) {
        return new DefaultBiJoiner<>(leftMapping, JoinerType.CONTAINING, rightMapping);
    }

    /**
     * Joins every A and B where a value of property on A is contained in the collection of properties on B.
     * <p>
     * For example:
     * <ul>
     * <li>{@code "A"} contained in {@code ["A", "B"]} is {@code true}</li>
     * <li>{@code "A"} contained in {@code ["A"]} is {@code true}</li>
     * <li>{@code "A"} contained in {@code ["X", "Y"]} is {@code false}</li>
     * <li>{@code "A"} contained in {@code []} is {@code false}</li>
     * <li>{@code null} contained in {@code ["A", "B"]} is {@code false}</li>
     * <li>{@code null} contained in {@code []} is {@code false}</li>
     * </ul>
     *
     * @param leftMapping mapping function to apply to A
     * @param rightMapping mapping function to apply to B
     * @param <A> the type of object on the left
     * @param <B> the type of object on the right
     * @param <Property_> the type of the property to compare
     */
    public static <A, B, Property_> @NonNull BiJoiner<A, B> containedIn(@NonNull Function<A, Property_> leftMapping,
            @NonNull Function<B, Collection<Property_>> rightMapping) {
        return new DefaultBiJoiner<>(leftMapping, JoinerType.CONTAINED_IN, rightMapping);
    }

    /**
     * As defined by {@link #containingAnyOf(Function, Function)} with both arguments using the same mapping.
     *
     * @param mapping mapping function to apply to both A and B
     * @param <A> the type of both objects
     * @param <Property_> the type of the property to compare
     */
    public static <A, Property_> @NonNull BiJoiner<A, A> containingAnyOf(@NonNull Function<A, Collection<Property_>> mapping) {
        return containingAnyOf(mapping, mapping);
    }

    /**
     * Joins every A and B where a collection of properties on A overlaps with a collection of properties on B.
     * <p>
     * For example:
     * <ul>
     * <li>{@code ["A", "B"]} intersecting {@code ["A", "B"]} is {@code true}</li>
     * <li>{@code ["A", "B"]} intersecting {@code ["A"]} is {@code true}</li>
     * <li>{@code ["A"]} intersecting {@code ["A", "B"]} is {@code true}</li>
     * <li>{@code ["A", "B"]} intersecting {@code ["X", "Y"]} is {@code false}</li>
     * <li>{@code ["A", "B"]} intersecting {@code []} is {@code false}</li>
     * <li>{@code []} intersecting {@code ["A", "B"]} is {@code false}</li>
     * <li>{@code []} intersecting {@code []} is {@code false}</li>
     * </ul>
     *
     * @param leftMapping mapping function to apply to A
     * @param rightMapping mapping function to apply to B
     * @param <A> the type of object on the left
     * @param <B> the type of object on the right
     * @param <Property_> the type of the property to compare
     */
    public static <A, B, Property_> @NonNull BiJoiner<A, B> containingAnyOf(
            @NonNull Function<A, Collection<Property_>> leftMapping,
            @NonNull Function<B, Collection<Property_>> rightMapping) {
        return new DefaultBiJoiner<>(leftMapping, JoinerType.CONTAINING_ANY_OF, rightMapping);
    }

    // ************************************************************************
    // TriJoiner
    // ************************************************************************

    /**
     * As defined by {@link #containing(Function, Function)}.
     *
     * @param <A> the type of the first object on the left
     * @param <B> the type of the second object on the left
     * @param <C> the type of the object on the right
     * @param <Property_> the type of the collection elements
     * @param leftMapping mapping function to apply to (A,B)
     * @param rightMapping mapping function to apply to C
     */
    public static <A, B, C, Property_> @NonNull TriJoiner<A, B, C> containing(
            @NonNull BiFunction<A, B, Collection<Property_>> leftMapping,
            @NonNull Function<C, Property_> rightMapping) {
        return new DefaultTriJoiner<>(leftMapping, JoinerType.CONTAINING, rightMapping);
    }

    /**
     * As defined by {@link #containedIn(Function, Function)}.
     *
     * @param <A> the type of the first object on the left
     * @param <B> the type of the second object on the left
     * @param <C> the type of the object on the right
     * @param <Property_> the type of the collection elements
     * @param leftMapping mapping function to apply to (A,B)
     * @param rightMapping mapping function to apply to C
     */
    public static <A, B, C, Property_> @NonNull TriJoiner<A, B, C> containedIn(@NonNull BiFunction<A, B, Property_> leftMapping,
            @NonNull Function<C, Collection<Property_>> rightMapping) {
        return new DefaultTriJoiner<>(leftMapping, JoinerType.CONTAINED_IN, rightMapping);
    }

    /**
     * As defined by {@link #containingAnyOf(Function, Function)}.
     *
     * @param <A> the type of the first object on the left
     * @param <B> the type of the second object on the left
     * @param <C> the type of the object on the right
     * @param <Property_> the type of the collection elements
     * @param leftMapping mapping function to apply to (A,B)
     * @param rightMapping mapping function to apply to C
     */
    public static <A, B, C, Property_> @NonNull TriJoiner<A, B, C> containingAnyOf(
            @NonNull BiFunction<A, B, Collection<Property_>> leftMapping,
            @NonNull Function<C, Collection<Property_>> rightMapping) {
        return new DefaultTriJoiner<>(leftMapping, JoinerType.CONTAINING_ANY_OF, rightMapping);
    }

    // ************************************************************************
    // QuadJoiner
    // ************************************************************************

    /**
     * As defined by {@link #containing(Function, Function)}.
     *
     * @param <A> the type of the first object on the left
     * @param <B> the type of the second object on the left
     * @param <C> the type of the third object on the left
     * @param <D> the type of the object on the right
     * @param <Property_> the type of the collection elements
     * @param leftMapping mapping function to apply to (A,B,C)
     * @param rightMapping mapping function to apply to D
     */
    public static <A, B, C, D, Property_> @NonNull QuadJoiner<A, B, C, D> containing(
            @NonNull TriFunction<A, B, C, Collection<Property_>> leftMapping,
            @NonNull Function<D, Property_> rightMapping) {
        return new DefaultQuadJoiner<>(leftMapping, JoinerType.CONTAINING, rightMapping);
    }

    /**
     * As defined by {@link #containedIn(Function, Function)}.
     *
     * @param <A> the type of the first object on the left
     * @param <B> the type of the second object on the left
     * @param <C> the type of the third object on the left
     * @param <D> the type of the object on the right
     * @param <Property_> the type of the collection elements
     * @param leftMapping mapping function to apply to (A,B,C)
     * @param rightMapping mapping function to apply to D
     */
    public static <A, B, C, D, Property_> @NonNull QuadJoiner<A, B, C, D> containedIn(
            @NonNull TriFunction<A, B, C, Property_> leftMapping,
            @NonNull Function<D, Collection<Property_>> rightMapping) {
        return new DefaultQuadJoiner<>(leftMapping, JoinerType.CONTAINED_IN, rightMapping);
    }

    /**
     * As defined by {@link #containingAnyOf(Function, Function)}.
     *
     * @param <A> the type of the first object on the left
     * @param <B> the type of the second object on the left
     * @param <C> the type of the third object on the left
     * @param <D> the type of the object on the right
     * @param <Property_> the type of the collection elements
     * @param leftMapping mapping function to apply to (A,B,C)
     * @param rightMapping mapping function to apply to D
     */
    public static <A, B, C, D, Property_> @NonNull QuadJoiner<A, B, C, D> containingAnyOf(
            @NonNull TriFunction<A, B, C, Collection<Property_>> leftMapping,
            @NonNull Function<D, Collection<Property_>> rightMapping) {
        return new DefaultQuadJoiner<>(leftMapping, JoinerType.CONTAINING_ANY_OF, rightMapping);
    }

    // ************************************************************************
    // PentaJoiner
    // ************************************************************************

    /**
     * As defined by {@link #containing(Function, Function)}.
     *
     * @param <A> the type of the first object on the left
     * @param <B> the type of the second object on the left
     * @param <C> the type of the third object on the left
     * @param <D> the type of the fourth object on the left
     * @param <E> the type of the object on the right
     * @param <Property_> the type of the collection elements
     * @param leftMapping mapping function to apply to (A,B,C,D)
     * @param rightMapping mapping function to apply to E
     */
    public static <A, B, C, D, E, Property_> @NonNull PentaJoiner<A, B, C, D, E> containing(
            @NonNull QuadFunction<A, B, C, D, Collection<Property_>> leftMapping,
            @NonNull Function<E, Property_> rightMapping) {
        return new DefaultPentaJoiner<>(leftMapping, JoinerType.CONTAINING, rightMapping);
    }

    /**
     * As defined by {@link #containedIn(Function, Function)}.
     *
     * @param <A> the type of the first object on the left
     * @param <B> the type of the second object on the left
     * @param <C> the type of the third object on the left
     * @param <D> the type of the fourth object on the left
     * @param <E> the type of the object on the right
     * @param <Property_> the type of the collection elements
     * @param leftMapping mapping function to apply to (A,B,C,D)
     * @param rightMapping mapping function to apply to E
     */
    public static <A, B, C, D, E, Property_> @NonNull PentaJoiner<A, B, C, D, E> containedIn(
            @NonNull QuadFunction<A, B, C, D, Property_> leftMapping,
            @NonNull Function<E, Collection<Property_>> rightMapping) {
        return new DefaultPentaJoiner<>(leftMapping, JoinerType.CONTAINED_IN, rightMapping);
    }

    /**
     * As defined by {@link #containingAnyOf(Function, Function)}.
     *
     * @param <A> the type of the first object on the left
     * @param <B> the type of the second object on the left
     * @param <C> the type of the third object on the left
     * @param <D> the type of the fourth object on the left
     * @param <E> the type of the object on the right
     * @param <Property_> the type of the collection elements
     * @param leftMapping mapping function to apply to (A,B,C,D)
     * @param rightMapping mapping function to apply to E
     */
    public static <A, B, C, D, E, Property_> @NonNull PentaJoiner<A, B, C, D, E> containingAnyOf(
            @NonNull QuadFunction<A, B, C, D, Collection<Property_>> leftMapping,
            @NonNull Function<E, Collection<Property_>> rightMapping) {
        return new DefaultPentaJoiner<>(leftMapping, JoinerType.CONTAINING_ANY_OF, rightMapping);
    }

}
