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

public final class ConstantLambdaUtils {
    @SuppressWarnings("rawtypes")
    public static final Function IDENTITY = Function.identity();
    @SuppressWarnings("rawtypes")
    public static final BiPredicate NOT_EQUALS = (a, b) -> !Objects.equals(a, b);

    @SuppressWarnings("rawtypes")
    public static final BiFunction BI_PICK_FIRST = (a, b) -> a;

    @SuppressWarnings("rawtypes")
    public static final TriFunction TRI_PICK_FIRST = (a, b, c) -> a;

    @SuppressWarnings("rawtypes")
    public static final QuadFunction QUAD_PICK_FIRST = (a, b, c, d) -> a;

    @SuppressWarnings("rawtypes")
    public static final BiFunction BI_PICK_SECOND = (a, b) -> b;

    @SuppressWarnings("rawtypes")
    public static final TriFunction TRI_PICK_SECOND = (a, b, c) -> b;

    @SuppressWarnings("rawtypes")
    public static final QuadFunction QUAD_PICK_SECOND = (a, b, c, d) -> b;

    @SuppressWarnings("rawtypes")
    public static final TriFunction TRI_PICK_THIRD = (a, b, c) -> c;

    @SuppressWarnings("rawtypes")
    public static final QuadFunction QUAD_PICK_THIRD = (a, b, c, d) -> c;

    @SuppressWarnings("rawtypes")
    public static final QuadFunction QUAD_PICK_FOURTH = (a, b, c, d) -> d;

    @SuppressWarnings("rawtypes")
    public static final ToIntFunction UNI_CONSTANT_ONE = (a) -> 1;

    @SuppressWarnings("rawtypes")
    public static final ToIntBiFunction BI_CONSTANT_ONE = (a, b) -> 1;

    @SuppressWarnings("rawtypes")
    public static final ToIntTriFunction TRI_CONSTANT_ONE = (a, b, c) -> 1;

    @SuppressWarnings("rawtypes")
    public static final ToIntQuadFunction QUAD_CONSTANT_ONE = (a, b, c, d) -> 1;
}
