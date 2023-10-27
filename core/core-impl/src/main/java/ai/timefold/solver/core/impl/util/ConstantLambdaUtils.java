package ai.timefold.solver.core.impl.util;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;

/**
 * A class that hold common lambdas that are guarantee to be the same across method calls. In most JDK's, stateless lambdas are
 * bound to a {@link java.lang.invoke.ConstantCallSite} inside the method that define them, but that
 * {@link java.lang.invoke.ConstantCallSite} is not shared across methods (even for methods in the same class). Thus, when
 * lambda reference equality is important (such as for node sharing in Constraint Streams), the lambdas in this class should be
 * used.
 */
public final class ConstantLambdaUtils {
    @SuppressWarnings("rawtypes")
    private static final Function IDENTITY = Function.identity();

    @SuppressWarnings("rawtypes")
    private static final BiPredicate NOT_EQUALS = (a, b) -> !Objects.equals(a, b);

    @SuppressWarnings("rawtypes")
    private static final BiFunction BI_PICK_FIRST = (a, b) -> a;

    @SuppressWarnings("rawtypes")
    private static final TriFunction TRI_PICK_FIRST = (a, b, c) -> a;

    @SuppressWarnings("rawtypes")
    private static final QuadFunction QUAD_PICK_FIRST = (a, b, c, d) -> a;

    @SuppressWarnings("rawtypes")
    private static final BiFunction BI_PICK_SECOND = (a, b) -> b;

    @SuppressWarnings("rawtypes")
    private static final TriFunction TRI_PICK_SECOND = (a, b, c) -> b;

    @SuppressWarnings("rawtypes")
    private static final QuadFunction QUAD_PICK_SECOND = (a, b, c, d) -> b;

    @SuppressWarnings("rawtypes")
    private static final TriFunction TRI_PICK_THIRD = (a, b, c) -> c;

    @SuppressWarnings("rawtypes")
    private static final QuadFunction QUAD_PICK_THIRD = (a, b, c, d) -> c;

    @SuppressWarnings("rawtypes")
    private static final QuadFunction QUAD_PICK_FOURTH = (a, b, c, d) -> d;

    @SuppressWarnings("rawtypes")
    private static final ToIntFunction UNI_CONSTANT_ONE = (a) -> 1;
    @SuppressWarnings("rawtypes")
    private static final ToIntBiFunction BI_CONSTANT_ONE = (a, b) -> 1;
    @SuppressWarnings("rawtypes")
    private static final ToIntTriFunction TRI_CONSTANT_ONE = (a, b, c) -> 1;

    @SuppressWarnings("rawtypes")
    private static final ToIntQuadFunction QUAD_CONSTANT_ONE = (a, b, c, d) -> 1;

    /**
     * Returns a {@link Function} that returns its only input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A> Function<A, A> identity() {
        return IDENTITY;
    }

    /**
     * Returns a {@link BiPredicate} that return true if and only if its inputs are not equal according to
     * {@link Objects#equals(Object, Object)}.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A> BiPredicate<A, A> notEquals() {
        return NOT_EQUALS;
    }

    /**
     * Returns a {@link BiFunction} that returns its first input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B> BiFunction<A, B, A> biPickFirst() {
        return BI_PICK_FIRST;
    }

    /**
     * Returns a {@link TriFunction} that returns its first input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C> TriFunction<A, B, C, A> triPickFirst() {
        return TRI_PICK_FIRST;
    }

    /**
     * Returns a {@link QuadFunction} that returns its first input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, D> QuadFunction<A, B, C, D, A> quadPickFirst() {
        return QUAD_PICK_FIRST;
    }

    /**
     * Returns a {@link BiFunction} that returns its second input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B> BiFunction<A, B, B> biPickSecond() {
        return BI_PICK_SECOND;
    }

    /**
     * Returns a {@link TriFunction} that returns its second input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C> TriFunction<A, B, C, B> triPickSecond() {
        return TRI_PICK_SECOND;
    }

    /**
     * Returns a {@link QuadFunction} that returns its second input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, D> QuadFunction<A, B, C, D, B> quadPickSecond() {
        return QUAD_PICK_SECOND;
    }

    /**
     * Returns a {@link TriFunction} that returns its third input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C> TriFunction<A, B, C, C> triPickThird() {
        return TRI_PICK_THIRD;
    }

    /**
     * Returns a {@link TriFunction} that returns its third input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, D> QuadFunction<A, B, C, D, C> quadPickThird() {
        return QUAD_PICK_THIRD;
    }

    /**
     * Returns a {@link QuadFunction} that returns its fourth input.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, D> QuadFunction<A, B, C, D, D> quadPickFourth() {
        return QUAD_PICK_FOURTH;
    }

    /**
     * Returns a {@link ToIntFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A> ToIntFunction<A> uniConstantOne() {
        return UNI_CONSTANT_ONE;
    }

    /**
     * Returns a {@link ToIntBiFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B> ToIntBiFunction<A, B> biConstantOne() {
        return BI_CONSTANT_ONE;
    }

    /**
     * Returns a {@link ToIntTriFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C> ToIntTriFunction<A, B, C> triConstantOne() {
        return TRI_CONSTANT_ONE;
    }

    /**
     * Returns a {@link ToIntQuadFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, D> ToIntQuadFunction<A, B, C, D> quadConstantOne() {
        return QUAD_CONSTANT_ONE;
    }
}
