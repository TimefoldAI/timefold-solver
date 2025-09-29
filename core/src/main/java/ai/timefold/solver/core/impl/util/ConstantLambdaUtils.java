package ai.timefold.solver.core.impl.util;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;

/**
 * A class that holds common lambdas that are guaranteed to be the same across method calls.
 * In most JDK's,
 * stateless lambdas are bound to a {@link java.lang.invoke.ConstantCallSite} inside the method that defined them,
 * but that {@link java.lang.invoke.ConstantCallSite} is not shared across methods,
 * even for methods in the same class.
 * Thus, when lambda reference equality is important (such as for node sharing in Constraint Streams),
 * the lambdas in this class should be used.
 */
public final class ConstantLambdaUtils {
    private static final Runnable NO_OP = () -> {
    };

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
    private static final Function UNI_CONSTANT_NULL = a -> null;

    @SuppressWarnings("rawtypes")
    private static final ToLongFunction UNI_CONSTANT_ZERO_LONG = a -> 0L;

    @SuppressWarnings("rawtypes")
    private static final ToIntFunction UNI_CONSTANT_ONE = a -> 1;

    @SuppressWarnings("rawtypes")
    private static final ToLongFunction UNI_CONSTANT_ONE_LONG = a -> 1L;

    @SuppressWarnings("rawtypes")
    private static final Function UNI_CONSTANT_ONE_BIG_DECIMAL = a -> BigDecimal.ONE;

    @SuppressWarnings("rawtypes")
    private static final BiFunction BI_CONSTANT_NULL = (a, b) -> null;

    @SuppressWarnings("rawtypes")
    private static final ToLongBiFunction BI_CONSTANT_ZERO_LONG = (a, b) -> 0L;

    @SuppressWarnings("rawtypes")
    private static final ToIntBiFunction BI_CONSTANT_ONE = (a, b) -> 1;

    @SuppressWarnings("rawtypes")
    private static final ToLongBiFunction BI_CONSTANT_ONE_LONG = (a, b) -> 1L;

    @SuppressWarnings("rawtypes")
    private static final BiFunction BI_CONSTANT_ONE_BIG_DECIMAL = (a, b) -> BigDecimal.ONE;

    @SuppressWarnings("rawtypes")
    private static final TriFunction TRI_CONSTANT_NULL = (a, b, c) -> null;

    @SuppressWarnings("rawtypes")
    private static final ToLongTriFunction TRI_CONSTANT_ZERO_LONG = (a, b, c) -> 0L;

    @SuppressWarnings("rawtypes")
    private static final ToIntTriFunction TRI_CONSTANT_ONE = (a, b, c) -> 1;

    @SuppressWarnings("rawtypes")
    private static final ToLongTriFunction TRI_CONSTANT_ONE_LONG = (a, b, c) -> 1L;

    @SuppressWarnings("rawtypes")
    private static final TriFunction TRI_CONSTANT_ONE_BIG_DECIMAL = (a, b, c) -> BigDecimal.ONE;

    @SuppressWarnings("rawtypes")
    private static final ToLongQuadFunction QUAD_CONSTANT_ZERO_LONG = (a, b, c, d) -> 0L;

    @SuppressWarnings("rawtypes")
    private static final ToIntQuadFunction QUAD_CONSTANT_ONE = (a, b, c, d) -> 1;

    @SuppressWarnings("rawtypes")
    private static final ToLongQuadFunction QUAD_CONSTANT_ONE_LONG = (a, b, c, d) -> 1L;

    @SuppressWarnings("rawtypes")
    private static final QuadFunction QUAD_CONSTANT_ONE_BIG_DECiMAL = (a, b, c, d) -> BigDecimal.ONE;

    /**
     * Returns a {@link Runnable} that does nothing.
     *
     * @return never null
     */
    public static Runnable noop() {
        return NO_OP;
    }

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
     * Returns a {@link Function} that returns null.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B> Function<A, B> uniConstantNull() {
        return UNI_CONSTANT_NULL;
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
     * Returns a {@link ToLongFunction} that returns the constant 0.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A> ToLongFunction<A> uniConstantZeroLong() {
        return UNI_CONSTANT_ZERO_LONG;
    }

    /**
     * Returns a {@link ToLongFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A> ToLongFunction<A> uniConstantOneLong() {
        return UNI_CONSTANT_ONE_LONG;
    }

    /**
     * Returns a {@link Function} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A> Function<A, BigDecimal> uniConstantOneBigDecimal() {
        return UNI_CONSTANT_ONE_BIG_DECIMAL;
    }

    /**
     * Returns a {@link BiFunction} that returns null.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C> BiFunction<A, B, C> biConstantNull() {
        return BI_CONSTANT_NULL;
    }

    /**
     * Returns a {@link ToLongBiFunction} that returns the constant 0.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B> ToLongBiFunction<A, B> biConstantZeroLong() {
        return BI_CONSTANT_ZERO_LONG;
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
     * Returns a {@link ToLongBiFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B> ToLongBiFunction<A, B> biConstantOneLong() {
        return BI_CONSTANT_ONE_LONG;
    }

    /**
     * Returns a {@link BiFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B> BiFunction<A, B, BigDecimal> biConstantOneBigDecimal() {
        return BI_CONSTANT_ONE_BIG_DECIMAL;
    }

    /**
     * Returns a {@link TriFunction} that returns null.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, D> TriFunction<A, B, C, D> triConstantNull() {
        return TRI_CONSTANT_NULL;
    }

    /**
     * Returns a {@link ToLongTriFunction} that returns the constant 0.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C> ToLongTriFunction<A, B, C> triConstantZeroLong() {
        return TRI_CONSTANT_ZERO_LONG;
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
     * Returns a {@link ToLongTriFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C> ToLongTriFunction<A, B, C> triConstantOneLong() {
        return TRI_CONSTANT_ONE_LONG;
    }

    /**
     * Returns a {@link TriFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C> TriFunction<A, B, C, BigDecimal> triConstantOneBigDecimal() {
        return TRI_CONSTANT_ONE_BIG_DECIMAL;
    }

    /**
     * Returns a {@link ToLongQuadFunction} that returns the constant 0.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, D> ToLongQuadFunction<A, B, C, D> quadConstantZeroLong() {
        return QUAD_CONSTANT_ZERO_LONG;
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

    /**
     * Returns a {@link ToLongQuadFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, D> ToLongQuadFunction<A, B, C, D> quadConstantOneLong() {
        return QUAD_CONSTANT_ONE_LONG;
    }

    /**
     * Returns a {@link QuadFunction} that returns the constant 1.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, D> QuadFunction<A, B, C, D, BigDecimal> quadConstantOneBigDecimal() {
        return QUAD_CONSTANT_ONE_BIG_DECiMAL;
    }

    private ConstantLambdaUtils() {
        // No external instances.
    }
}
