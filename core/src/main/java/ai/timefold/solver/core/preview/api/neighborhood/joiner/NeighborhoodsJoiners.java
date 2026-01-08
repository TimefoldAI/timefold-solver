package ai.timefold.solver.core.preview.api.neighborhood.joiner;

import java.util.function.BiPredicate;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.neighborhood.joiner.DefaultBiNeighborhoodsJoiner;
import ai.timefold.solver.core.impl.neighborhood.joiner.FilteringBiNeighborhoodsJoiner;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;
import ai.timefold.solver.core.preview.api.neighborhood.function.BiNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;

import org.jspecify.annotations.NullMarked;

/**
 * Creates an {@link BiNeighborhoodsJoiner}, ... instance
 * for use in {@link UniEnumeratingStream#join(Class, BiNeighborhoodsJoiner[])}, ...
 */
@NullMarked
public final class NeighborhoodsJoiners {

    /**
     * As defined by {@link #equal(Function)} with {@link Function#identity()} as the argument.
     *
     * @param <A> the type of both objects
     */
    public static <A> BiNeighborhoodsJoiner<A, A> equal() {
        return equal(ConstantLambdaUtils.identity());
    }

    /**
     * As defined by {@link #equal(Function, Function)} with both arguments using the same mapping.
     *
     * @param <A> the type of both objects
     * @param <Property_> the type of the property to compare
     * @param mapping mapping function to apply to both A and B
     */
    public static <A, Property_> BiNeighborhoodsJoiner<A, A> equal(Function<A, Property_> mapping) {
        return equal(mapping, mapping);
    }

    /**
     * Joins every A and B that share a property.
     * These are exactly the pairs where {@code leftMapping.apply(A).equals(rightMapping.apply(B))}.
     * For example, on a cartesian product of list {@code [Ann(age = 20), Beth(age = 25), Eric(age = 20)]}
     * with both leftMapping and rightMapping being {@code Person::getAge},
     * this joiner will produce pairs {@code (Ann, Ann), (Ann, Eric), (Beth, Beth), (Eric, Ann), (Eric, Eric)}.
     *
     * @param <B> the type of object on the right
     * @param <Property_> the type of the property to compare
     * @param leftMapping mapping function to apply to A
     * @param rightMapping mapping function to apply to B
     */
    public static <A, B, Property_> BiNeighborhoodsJoiner<A, B> equal(Function<A, Property_> leftMapping,
            Function<B, Property_> rightMapping) {
        return new DefaultBiNeighborhoodsJoiner<>(leftMapping, JoinerType.EQUAL, rightMapping);
    }

    /**
     * As defined by {@link #lessThan(Function, Function)} with both arguments using the same mapping.
     *
     * @param mapping mapping function to apply
     * @param <A> the type of both objects
     * @param <Property_> the type of the property to compare
     */
    public static <A, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, A>
            lessThan(Function<A, Property_> mapping) {
        return lessThan(mapping, mapping);
    }

    /**
     * Joins every A and B where a value of property on A is less than the value of a property on B.
     * These are exactly the pairs where {@code leftMapping.apply(A).compareTo(rightMapping.apply(B)) < 0}.
     *
     * For example, on a cartesian product of list {@code [Ann(age = 20), Beth(age = 25), Eric(age = 20)]}
     * with both leftMapping and rightMapping being {@code Person::getAge},
     * this joiner will produce pairs {@code (Ann, Beth), (Eric, Beth)}.
     *
     * @param leftMapping mapping function to apply to A
     * @param rightMapping mapping function to apply to B
     * @param <A> the type of object on the left
     * @param <B> the type of object on the right
     * @param <Property_> the type of the property to compare
     */
    public static <A, B, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, B> lessThan(
            Function<A, Property_> leftMapping, Function<B, Property_> rightMapping) {
        return new DefaultBiNeighborhoodsJoiner<>(leftMapping, JoinerType.LESS_THAN, rightMapping);
    }

    /**
     * As defined by {@link #lessThanOrEqual(Function, Function)} with both arguments using the same mapping.
     *
     * @param mapping mapping function to apply
     * @param <A> the type of both objects
     * @param <Property_> the type of the property to compare
     */
    public static <A, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, A> lessThanOrEqual(
            Function<A, Property_> mapping) {
        return lessThanOrEqual(mapping, mapping);
    }

    /**
     * Joins every A and B where a value of property on A is less than or equal to the value of a property on B.
     * These are exactly the pairs where {@code leftMapping.apply(A).compareTo(rightMapping.apply(B)) <= 0}.
     *
     * For example, on a cartesian product of list {@code [Ann(age = 20), Beth(age = 25), Eric(age = 20)]}
     * with both leftMapping and rightMapping being {@code Person::getAge},
     * this joiner will produce pairs
     * {@code (Ann, Ann), (Ann, Beth), (Ann, Eric), (Beth, Beth), (Eric, Ann), (Eric, Beth), (Eric, Eric)}.
     *
     * @param leftMapping mapping function to apply to A
     * @param rightMapping mapping function to apply to B
     * @param <A> the type of object on the left
     * @param <B> the type of object on the right
     * @param <Property_> the type of the property to compare
     */
    public static <A, B, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, B> lessThanOrEqual(
            Function<A, Property_> leftMapping, Function<B, Property_> rightMapping) {
        return new DefaultBiNeighborhoodsJoiner<>(leftMapping, JoinerType.LESS_THAN_OR_EQUAL, rightMapping);
    }

    /**
     * As defined by {@link #greaterThan(Function, Function)} with both arguments using the same mapping.
     *
     * @param mapping mapping function to apply
     * @param <A> the type of both objects
     * @param <Property_> the type of the property to compare
     */
    public static <A, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, A> greaterThan(
            Function<A, Property_> mapping) {
        return greaterThan(mapping, mapping);
    }

    /**
     * Joins every A and B where a value of property on A is greater than the value of a property on B.
     * These are exactly the pairs where {@code leftMapping.apply(A).compareTo(rightMapping.apply(B)) > 0}.
     *
     * For example, on a cartesian product of list {@code [Ann(age = 20), Beth(age = 25), Eric(age = 20)]}
     * with both leftMapping and rightMapping being {@code Person::getAge},
     * this joiner will produce pairs {@code (Beth, Ann), (Beth, Eric)}.
     *
     * @param leftMapping mapping function to apply to A
     * @param rightMapping mapping function to apply to B
     * @param <A> the type of object on the left
     * @param <B> the type of object on the right
     * @param <Property_> the type of the property to compare
     */
    public static <A, B, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, B> greaterThan(
            Function<A, Property_> leftMapping, Function<B, Property_> rightMapping) {
        return new DefaultBiNeighborhoodsJoiner<>(leftMapping, JoinerType.GREATER_THAN, rightMapping);
    }

    /**
     * As defined by {@link #greaterThanOrEqual(Function, Function)} with both arguments using the same mapping.
     *
     * @param mapping mapping function to apply
     * @param <A> the type of both objects
     * @param <Property_> the type of the property to compare
     */
    public static <A, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, A> greaterThanOrEqual(
            Function<A, Property_> mapping) {
        return greaterThanOrEqual(mapping, mapping);
    }

    /**
     * Joins every A and B where a value of property on A is greater than or equal to the value of a property on B.
     * These are exactly the pairs where {@code leftMapping.apply(A).compareTo(rightMapping.apply(B)) >= 0}.
     *
     * For example, on a cartesian product of list {@code [Ann(age = 20), Beth(age = 25), Eric(age = 20)]}
     * with both leftMapping and rightMapping being {@code Person::getAge},
     * this joiner will produce pairs
     * {@code (Ann, Ann), (Ann, Eric), (Beth, Ann), (Beth, Beth), (Beth, Eric), (Eric, Ann), (Eric, Eric)}.
     *
     * @param leftMapping mapping function to apply to A
     * @param rightMapping mapping function to apply to B
     * @param <A> the type of object on the left
     * @param <B> the type of object on the right
     * @param <Property_> the type of the property to compare
     */
    public static <A, B, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, B> greaterThanOrEqual(
            Function<A, Property_> leftMapping, Function<B, Property_> rightMapping) {
        return new DefaultBiNeighborhoodsJoiner<>(leftMapping, JoinerType.GREATER_THAN_OR_EQUAL, rightMapping);
    }

    /**
     * Applies a filter to the joined tuple, with the semantics of {@link BiConstraintStream#filter(BiPredicate)}.
     *
     * For example, on a cartesian product of list {@code [Ann(age = 20), Beth(age = 25), Eric(age = 20)]}
     * with filter being {@code age == 20},
     * this joiner will produce pairs {@code (Ann, Ann), (Ann, Eric), (Eric, Ann), (Eric, Eric)}.
     *
     * @param filter filter to apply
     * @param <A> type of the first fact in the tuple
     * @param <B> type of the second fact in the tuple
     */
    public static <Solution_, A, B> BiNeighborhoodsJoiner<A, B> filtering(BiNeighborhoodsPredicate<Solution_, A, B> filter) {
        return new FilteringBiNeighborhoodsJoiner<>(filter);
    }

    /**
     * Joins every A and B that overlap for an interval which is specified by a start and end property on both A and B.
     * These are exactly the pairs where {@code A.start < B.end} and {@code A.end > B.start}.
     *
     * For example, on a cartesian product of list
     * {@code [Ann(start=08:00, end=14:00), Beth(start=12:00, end=18:00), Eric(start=16:00, end=22:00)]}
     * with startMapping being {@code Person::getStart} and endMapping being {@code Person::getEnd},
     * this joiner will produce pairs
     * {@code (Ann, Ann), (Ann, Beth), (Beth, Ann), (Beth, Beth), (Beth, Eric), (Eric, Beth), (Eric, Eric)}.
     *
     * @param startMapping maps the argument to the start point of its interval (inclusive)
     * @param endMapping maps the argument to the end point of its interval (exclusive)
     * @param <A> the type of both the first and second argument
     * @param <Property_> the type used to define the interval, comparable
     */
    public static <A, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, A> overlapping(
            Function<A, Property_> startMapping, Function<A, Property_> endMapping) {
        return overlapping(startMapping, endMapping, startMapping, endMapping);
    }

    /**
     * As defined by {@link #overlapping(Function, Function)}.
     *
     * @param leftStartMapping maps the first argument to its interval start point (inclusive)
     * @param leftEndMapping maps the first argument to its interval end point (exclusive)
     * @param rightStartMapping maps the second argument to its interval start point (inclusive)
     * @param rightEndMapping maps the second argument to its interval end point (exclusive)
     * @param <A> the type of the first argument
     * @param <B> the type of the second argument
     * @param <Property_> the type used to define the interval, comparable
     */
    public static <A, B, Property_ extends Comparable<Property_>> BiNeighborhoodsJoiner<A, B> overlapping(
            Function<A, Property_> leftStartMapping, Function<A, Property_> leftEndMapping,
            Function<B, Property_> rightStartMapping, Function<B, Property_> rightEndMapping) {
        return NeighborhoodsJoiners.lessThan(leftStartMapping, rightEndMapping)
                .and(NeighborhoodsJoiners.greaterThan(leftEndMapping, rightStartMapping));
    }

    private NeighborhoodsJoiners() {
    }

}
