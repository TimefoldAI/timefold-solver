package ai.timefold.solver.core.api.score.stream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.collector.bi.InnerBiConstraintCollectors;
import ai.timefold.solver.core.impl.score.stream.collector.quad.InnerQuadConstraintCollectors;
import ai.timefold.solver.core.impl.score.stream.collector.tri.InnerTriConstraintCollectors;
import ai.timefold.solver.core.impl.score.stream.collector.uni.InnerUniConstraintCollectors;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

/**
 * Creates an {@link UniConstraintCollector}, {@link BiConstraintCollector}, ... instance
 * for use in {@link UniConstraintStream#groupBy(Function, UniConstraintCollector)}, ...
 */
public final class ConstraintCollectors {
    // ************************************************************************
    // count
    // ************************************************************************

    /**
     * Returns a collector that counts the number of elements that are being grouped.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(count())} returns {@code 5}.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code 0}.
     *
     * @param <A> type of the matched fact
     * @return never null
     */
    public static <A> UniConstraintCollector<A, ?, Integer> count() {
        return InnerUniConstraintCollectors.count();
    }

    /**
     * As defined by {@link #count()}.
     */
    public static <A> UniConstraintCollector<A, ?, Long> countLong() {
        return InnerUniConstraintCollectors.countLong();
    }

    /**
     * As defined by {@link #count()}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Integer> countBi() {
        return InnerBiConstraintCollectors.count();
    }

    /**
     * As defined by {@link #count()}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Long> countLongBi() {
        return InnerBiConstraintCollectors.countLong();
    }

    /**
     * As defined by {@link #count()}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Integer> countTri() {
        return InnerTriConstraintCollectors.count();
    }

    /**
     * As defined by {@link #count()}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Long> countLongTri() {
        return InnerTriConstraintCollectors.countLong();
    }

    /**
     * As defined by {@link #count()}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Integer> countQuad() {
        return InnerQuadConstraintCollectors.count();
    }

    /**
     * As defined by {@link #count()}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Long> countLongQuad() {
        return InnerQuadConstraintCollectors.countLong();
    }

    // ************************************************************************
    // countDistinct
    // ************************************************************************

    /**
     * As defined by {@link #countDistinct(Function)}, with {@link Function#identity()} as the argument.
     */
    public static <A> UniConstraintCollector<A, ?, Integer> countDistinct() {
        return countDistinct(ConstantLambdaUtils.identity());
    }

    /**
     * Returns a collector that counts the number of unique elements that are being grouped.
     * Uniqueness is determined by {@link #equals(Object) equality}.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(countDistinct(Person::getAge))} returns {@code 3}, one for age 20, 25 and 30 each.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code 0}.
     *
     * @param <A> type of the matched fact
     * @return never null
     */
    public static <A> UniConstraintCollector<A, ?, Integer> countDistinct(Function<A, ?> groupValueMapping) {
        return InnerUniConstraintCollectors.countDistinct(groupValueMapping);
    }

    /**
     * As defined by {@link #countDistinct(Function)}.
     */
    public static <A> UniConstraintCollector<A, ?, Long> countDistinctLong(Function<A, ?> groupValueMapping) {
        return InnerUniConstraintCollectors.countDistinctLong(groupValueMapping);
    }

    /**
     * As defined by {@link #countDistinct(Function)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Integer> countDistinct(
            BiFunction<A, B, ?> groupValueMapping) {
        return InnerBiConstraintCollectors.countDistinct(groupValueMapping);
    }

    /**
     * As defined by {@link #countDistinct(Function)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Long> countDistinctLong(
            BiFunction<A, B, ?> groupValueMapping) {
        return InnerBiConstraintCollectors.countDistinctLong(groupValueMapping);
    }

    /**
     * As defined by {@link #countDistinct(Function)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Integer> countDistinct(
            TriFunction<A, B, C, ?> groupValueMapping) {
        return InnerTriConstraintCollectors.countDistinct(groupValueMapping);
    }

    /**
     * As defined by {@link #countDistinct(Function)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Long> countDistinctLong(
            TriFunction<A, B, C, ?> groupValueMapping) {
        return InnerTriConstraintCollectors.countDistinctLong(groupValueMapping);
    }

    /**
     * As defined by {@link #countDistinct(Function)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Integer> countDistinct(
            QuadFunction<A, B, C, D, ?> groupValueMapping) {
        return InnerQuadConstraintCollectors.countDistinct(groupValueMapping);
    }

    /**
     * As defined by {@link #countDistinct(Function)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Long> countDistinctLong(
            QuadFunction<A, B, C, D, ?> groupValueMapping) {
        return InnerQuadConstraintCollectors.countDistinctLong(groupValueMapping);
    }

    // ************************************************************************
    // sum
    // ************************************************************************

    /**
     * Returns a collector that sums an {@code int} property of the elements that are being grouped.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(sum(Person::getAge))} returns {@code 125}.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code 0}.
     *
     * @param <A> type of the matched fact
     * @return never null
     */
    public static <A> UniConstraintCollector<A, ?, Integer> sum(ToIntFunction<? super A> groupValueMapping) {
        return InnerUniConstraintCollectors.sum(groupValueMapping);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A> UniConstraintCollector<A, ?, Long> sumLong(ToLongFunction<? super A> groupValueMapping) {
        return InnerUniConstraintCollectors.sum(groupValueMapping);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, Result> UniConstraintCollector<A, ?, Result> sum(Function<? super A, Result> groupValueMapping,
            Result zero, BinaryOperator<Result> adder, BinaryOperator<Result> subtractor) {
        return InnerUniConstraintCollectors.sum(groupValueMapping, zero, adder, subtractor);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A> UniConstraintCollector<A, ?, BigDecimal> sumBigDecimal(
            Function<? super A, BigDecimal> groupValueMapping) {
        return sum(groupValueMapping, BigDecimal.ZERO, BigDecimal::add, BigDecimal::subtract);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A> UniConstraintCollector<A, ?, BigInteger> sumBigInteger(
            Function<? super A, BigInteger> groupValueMapping) {
        return sum(groupValueMapping, BigInteger.ZERO, BigInteger::add, BigInteger::subtract);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A> UniConstraintCollector<A, ?, Duration> sumDuration(
            Function<? super A, Duration> groupValueMapping) {
        return sum(groupValueMapping, Duration.ZERO, Duration::plus, Duration::minus);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A> UniConstraintCollector<A, ?, Period> sumPeriod(Function<? super A, Period> groupValueMapping) {
        return sum(groupValueMapping, Period.ZERO, Period::plus, Period::minus);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Integer> sum(
            ToIntBiFunction<? super A, ? super B> groupValueMapping) {
        return InnerBiConstraintCollectors.sum(groupValueMapping);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Long> sumLong(
            ToLongBiFunction<? super A, ? super B> groupValueMapping) {
        return InnerBiConstraintCollectors.sum(groupValueMapping);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, Result> BiConstraintCollector<A, B, ?, Result> sum(
            BiFunction<? super A, ? super B, Result> groupValueMapping, Result zero, BinaryOperator<Result> adder,
            BinaryOperator<Result> subtractor) {
        return InnerBiConstraintCollectors.sum(groupValueMapping, zero, adder, subtractor);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, BigDecimal> sumBigDecimal(
            BiFunction<? super A, ? super B, BigDecimal> groupValueMapping) {
        return sum(groupValueMapping, BigDecimal.ZERO, BigDecimal::add, BigDecimal::subtract);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, BigInteger> sumBigInteger(
            BiFunction<? super A, ? super B, BigInteger> groupValueMapping) {
        return sum(groupValueMapping, BigInteger.ZERO, BigInteger::add, BigInteger::subtract);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Duration> sumDuration(
            BiFunction<? super A, ? super B, Duration> groupValueMapping) {
        return sum(groupValueMapping, Duration.ZERO, Duration::plus, Duration::minus);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Period> sumPeriod(
            BiFunction<? super A, ? super B, Period> groupValueMapping) {
        return sum(groupValueMapping, Period.ZERO, Period::plus, Period::minus);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Integer> sum(
            ToIntTriFunction<? super A, ? super B, ? super C> groupValueMapping) {
        return InnerTriConstraintCollectors.sum(groupValueMapping);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Long> sumLong(
            ToLongTriFunction<? super A, ? super B, ? super C> groupValueMapping) {
        return InnerTriConstraintCollectors.sum(groupValueMapping);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C, Result> TriConstraintCollector<A, B, C, ?, Result> sum(
            TriFunction<? super A, ? super B, ? super C, Result> groupValueMapping, Result zero,
            BinaryOperator<Result> adder, BinaryOperator<Result> subtractor) {
        return InnerTriConstraintCollectors.sum(groupValueMapping, zero, adder, subtractor);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, BigDecimal> sumBigDecimal(
            TriFunction<? super A, ? super B, ? super C, BigDecimal> groupValueMapping) {
        return sum(groupValueMapping, BigDecimal.ZERO, BigDecimal::add, BigDecimal::subtract);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, BigInteger> sumBigInteger(
            TriFunction<? super A, ? super B, ? super C, BigInteger> groupValueMapping) {
        return sum(groupValueMapping, BigInteger.ZERO, BigInteger::add, BigInteger::subtract);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Duration> sumDuration(
            TriFunction<? super A, ? super B, ? super C, Duration> groupValueMapping) {
        return sum(groupValueMapping, Duration.ZERO, Duration::plus, Duration::minus);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Period> sumPeriod(
            TriFunction<? super A, ? super B, ? super C, Period> groupValueMapping) {
        return sum(groupValueMapping, Period.ZERO, Period::plus, Period::minus);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Integer> sum(
            ToIntQuadFunction<? super A, ? super B, ? super C, ? super D> groupValueMapping) {
        return InnerQuadConstraintCollectors.sum(groupValueMapping);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Long> sumLong(
            ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> groupValueMapping) {
        return InnerQuadConstraintCollectors.sum(groupValueMapping);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C, D, Result> QuadConstraintCollector<A, B, C, D, ?, Result> sum(
            QuadFunction<? super A, ? super B, ? super C, ? super D, Result> groupValueMapping, Result zero,
            BinaryOperator<Result> adder, BinaryOperator<Result> subtractor) {
        return InnerQuadConstraintCollectors.sum(groupValueMapping, zero, adder, subtractor);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, BigDecimal> sumBigDecimal(
            QuadFunction<? super A, ? super B, ? super C, ? super D, BigDecimal> groupValueMapping) {
        return sum(groupValueMapping, BigDecimal.ZERO, BigDecimal::add, BigDecimal::subtract);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, BigInteger> sumBigInteger(
            QuadFunction<? super A, ? super B, ? super C, ? super D, BigInteger> groupValueMapping) {
        return sum(groupValueMapping, BigInteger.ZERO, BigInteger::add, BigInteger::subtract);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Duration> sumDuration(
            QuadFunction<? super A, ? super B, ? super C, ? super D, Duration> groupValueMapping) {
        return sum(groupValueMapping, Duration.ZERO, Duration::plus, Duration::minus);
    }

    /**
     * As defined by {@link #sum(ToIntFunction)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Period> sumPeriod(
            QuadFunction<? super A, ? super B, ? super C, ? super D, Period> groupValueMapping) {
        return sum(groupValueMapping, Period.ZERO, Period::plus, Period::minus);
    }

    // ************************************************************************
    // min
    // ************************************************************************

    /**
     * Returns a collector that finds a minimum value in a group of {@link Comparable} elements.
     * <p>
     * Important: The {@link Comparable}'s {@link Comparable#compareTo(Object)} must be <i>consistent with equals</i>,
     * such that {@code e1.compareTo(e2) == 0} has the same boolean value as {@code e1.equals(e2)}.
     * In other words, if two elements compare to zero, any of them can be returned by the collector.
     * It can even differ between 2 score calculations on the exact same {@link PlanningSolution} state, due to
     * incremental score calculation.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(min())} returns either {@code Ann} or {@code Eric} arbitrarily, assuming the objects are
     * {@link Comparable} by the {@code age} field.
     * To avoid this, always end your {@link Comparator} by an identity comparison, such as
     * {@code Comparator.comparing(Person::getAge).comparing(Person::getId))}.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code null}.
     *
     * @param <A> type of the matched fact
     * @return never null
     */
    public static <A extends Comparable<A>> UniConstraintCollector<A, ?, A> min() {
        return InnerUniConstraintCollectors.min(ConstantLambdaUtils.identity());
    }

    /**
     * Returns a collector that finds a minimum value in a group of {@link Comparable} elements.
     * <p>
     * Important: The {@link Comparable}'s {@link Comparable#compareTo(Object)} must be <i>consistent with equals</i>,
     * such that {@code e1.compareTo(e2) == 0} has the same boolean value as {@code e1.equals(e2)}.
     * In other words, if two elements compare to zero, any of them can be returned by the collector.
     * It can even differ between 2 score calculations on the exact same {@link PlanningSolution} state, due to
     * incremental score calculation.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(min(Person::getAge))} returns {@code 20}.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code null}.
     *
     * @param <A> type of the matched fact
     * @param <Mapped> type of the result
     * @param groupValueMapping never null, maps facts from the matched type to the result type
     * @return never null
     */
    public static <A, Mapped extends Comparable<? super Mapped>> UniConstraintCollector<A, ?, Mapped> min(
            Function<A, Mapped> groupValueMapping) {
        return InnerUniConstraintCollectors.min(groupValueMapping);
    }

    /**
     * Returns a collector that finds a minimum value in a group of {@link Comparable} elements.
     * The elements will be compared according to the value returned by the comparable function.
     * <p>
     * Important: The {@link Comparable}'s {@link Comparable#compareTo(Object)} must be <i>consistent with equals</i>,
     * such that {@code e1.compareTo(e2) == 0} has the same boolean value as {@code e1.equals(e2)}.
     * In other words, if two elements compare to zero, any of them can be returned by the collector.
     * It can even differ between 2 score calculations on the exact same {@link PlanningSolution} state, due to
     * incremental score calculation.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(min(Person::name, Person::age))} returns {@code Ann} or {@code Eric},
     * as both have the same age.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code null}.
     *
     * @param <A> type of the matched fact
     * @param <Mapped> type of the result
     * @param <Comparable_> type of the comparable property
     * @param groupValueMapping never null, maps facts from the matched type to the result type
     * @param comparableFunction never null, maps facts from the matched type to the comparable property
     * @return never null
     */
    public static <A, Mapped, Comparable_ extends Comparable<? super Comparable_>> UniConstraintCollector<A, ?, Mapped> min(
            Function<A, Mapped> groupValueMapping, Function<Mapped, Comparable_> comparableFunction) {
        return InnerUniConstraintCollectors.min(groupValueMapping, comparableFunction);
    }

    /**
     * As defined by {@link #min()}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #min(Function, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A> UniConstraintCollector<A, ?, A> min(Comparator<? super A> comparator) {
        return min(ConstantLambdaUtils.identity(), comparator);
    }

    /**
     * As defined by {@link #min(Function)}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #min(Function, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A, Mapped> UniConstraintCollector<A, ?, Mapped> min(Function<A, Mapped> groupValueMapping,
            Comparator<? super Mapped> comparator) {
        return InnerUniConstraintCollectors.min(groupValueMapping, comparator);
    }

    /**
     * As defined by {@link #min(Function)}.
     */
    public static <A, B, Mapped extends Comparable<? super Mapped>> BiConstraintCollector<A, B, ?, Mapped> min(
            BiFunction<A, B, Mapped> groupValueMapping) {
        return InnerBiConstraintCollectors.min(groupValueMapping);
    }

    /**
     * As defined by {@link #min(Function, Function)}.
     */
    public static <A, B, Mapped, Comparable_ extends Comparable<? super Comparable_>> BiConstraintCollector<A, B, ?, Mapped>
            min(BiFunction<A, B, Mapped> groupValueMapping, Function<Mapped, Comparable_> comparableFunction) {
        return InnerBiConstraintCollectors.min(groupValueMapping, comparableFunction);
    }

    /**
     * As defined by {@link #min(Function)}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #min(BiFunction, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A, B, Mapped> BiConstraintCollector<A, B, ?, Mapped> min(BiFunction<A, B, Mapped> groupValueMapping,
            Comparator<? super Mapped> comparator) {
        return InnerBiConstraintCollectors.min(groupValueMapping, comparator);
    }

    /**
     * As defined by {@link #min(Function)}.
     */
    public static <A, B, C, Mapped extends Comparable<? super Mapped>> TriConstraintCollector<A, B, C, ?, Mapped> min(
            TriFunction<A, B, C, Mapped> groupValueMapping) {
        return InnerTriConstraintCollectors.min(groupValueMapping);
    }

    /**
     * As defined by {@link #min(Function, Function)}.
     */
    public static <A, B, C, Mapped, Comparable_ extends Comparable<? super Comparable_>>
            TriConstraintCollector<A, B, C, ?, Mapped>
            min(TriFunction<A, B, C, Mapped> groupValueMapping, Function<Mapped, Comparable_> comparableFunction) {
        return InnerTriConstraintCollectors.min(groupValueMapping, comparableFunction);
    }

    /**
     * As defined by {@link #min(Function)}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #min(TriFunction, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A, B, C, Mapped> TriConstraintCollector<A, B, C, ?, Mapped> min(
            TriFunction<A, B, C, Mapped> groupValueMapping, Comparator<? super Mapped> comparator) {
        return InnerTriConstraintCollectors.min(groupValueMapping, comparator);
    }

    /**
     * As defined by {@link #min(Function)}.
     */
    public static <A, B, C, D, Mapped extends Comparable<? super Mapped>> QuadConstraintCollector<A, B, C, D, ?, Mapped> min(
            QuadFunction<A, B, C, D, Mapped> groupValueMapping) {
        return InnerQuadConstraintCollectors.min(groupValueMapping);
    }

    /**
     * As defined by {@link #min(Function, Function)}.
     */
    public static <A, B, C, D, Mapped, Comparable_ extends Comparable<? super Comparable_>>
            QuadConstraintCollector<A, B, C, D, ?, Mapped>
            min(QuadFunction<A, B, C, D, Mapped> groupValueMapping, Function<Mapped, Comparable_> comparableFunction) {
        return InnerQuadConstraintCollectors.min(groupValueMapping, comparableFunction);
    }

    /**
     * As defined by {@link #min(Function)}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #min(QuadFunction, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A, B, C, D, Mapped> QuadConstraintCollector<A, B, C, D, ?, Mapped> min(
            QuadFunction<A, B, C, D, Mapped> groupValueMapping, Comparator<? super Mapped> comparator) {
        return InnerQuadConstraintCollectors.min(groupValueMapping, comparator);
    }

    // ************************************************************************
    // max
    // ************************************************************************

    /**
     * Returns a collector that finds a maximum value in a group of {@link Comparable} elements.
     * <p>
     * Important: The {@link Comparable}'s {@link Comparable#compareTo(Object)} must be <i>consistent with equals</i>,
     * such that {@code e1.compareTo(e2) == 0} has the same boolean value as {@code e1.equals(e2)}.
     * In other words, if two elements compare to zero, any of them can be returned by the collector.
     * It can even differ between 2 score calculations on the exact same {@link PlanningSolution} state, due to
     * incremental score calculation.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(max())} returns either {@code Cathy} or {@code David} arbitrarily, assuming the objects are
     * {@link Comparable} by the {@code age} field.
     * To avoid this, always end your {@link Comparator} by an identity comparison, such as
     * {@code Comparator.comparing(Person::getAge).comparing(Person::getId))}.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code null}.
     *
     * @param <A> type of the matched fact
     * @return never null
     */
    public static <A extends Comparable<A>> UniConstraintCollector<A, ?, A> max() {
        return InnerUniConstraintCollectors.max(ConstantLambdaUtils.identity());
    }

    /**
     * Returns a collector that finds a maximum value in a group of {@link Comparable} elements.
     * <p>
     * Important: The {@link Comparable}'s {@link Comparable#compareTo(Object)} must be <i>consistent with equals</i>,
     * such that {@code e1.compareTo(e2) == 0} has the same boolean value as {@code e1.equals(e2)}.
     * In other words, if two elements compare to zero, any of them can be returned by the collector.
     * It can even differ between 2 score calculations on the exact same {@link PlanningSolution} state, due to
     * incremental score calculation.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(max(Person::getAge))} returns {@code 30}.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code null}.
     *
     * @param <A> type of the matched fact
     * @param <Mapped> type of the result
     * @param groupValueMapping never null, maps facts from the matched type to the result type
     * @return never null
     */
    public static <A, Mapped extends Comparable<? super Mapped>> UniConstraintCollector<A, ?, Mapped> max(
            Function<A, Mapped> groupValueMapping) {
        return InnerUniConstraintCollectors.max(groupValueMapping);
    }

    /**
     * As defined by {@link #max()}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #max(Function, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A> UniConstraintCollector<A, ?, A> max(Comparator<? super A> comparator) {
        return InnerUniConstraintCollectors.max(ConstantLambdaUtils.identity(), comparator);
    }

    /**
     * Returns a collector that finds a maximum value in a group of elements.
     * The elements will be compared according to the value returned by the comparable function.
     * <p>
     * Important: The {@link Comparable}'s {@link Comparable#compareTo(Object)} must be <i>consistent with equals</i>,
     * such that {@code e1.compareTo(e2) == 0} has the same boolean value as {@code e1.equals(e2)}.
     * In other words, if two elements compare to zero, any of them can be returned by the collector.
     * It can even differ between 2 score calculations on the exact same {@link PlanningSolution} state, due to
     * incremental score calculation.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(max(Person::name, Person::age))} returns {@code Cathy} or {@code David},
     * as both have the same age.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code null}.
     *
     * @param <A> type of the matched fact
     * @param <Mapped> type of the result
     * @param <Comparable_> type of the comparable property
     * @param groupValueMapping never null, maps facts from the matched type to the result type
     * @param comparableFunction never null, maps facts from the matched type to the comparable property
     * @return never null
     */
    public static <A, Mapped, Comparable_ extends Comparable<? super Comparable_>> UniConstraintCollector<A, ?, Mapped>
            max(Function<A, Mapped> groupValueMapping, Function<Mapped, Comparable_> comparableFunction) {
        return InnerUniConstraintCollectors.max(groupValueMapping, comparableFunction);
    }

    /**
     * As defined by {@link #max(Function)}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #max(Function, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A, Mapped> UniConstraintCollector<A, ?, Mapped> max(Function<A, Mapped> groupValueMapping,
            Comparator<? super Mapped> comparator) {
        return InnerUniConstraintCollectors.max(groupValueMapping, comparator);
    }

    /**
     * As defined by {@link #max(Function)}.
     */
    public static <A, B, Mapped extends Comparable<? super Mapped>> BiConstraintCollector<A, B, ?, Mapped> max(
            BiFunction<A, B, Mapped> groupValueMapping) {
        return InnerBiConstraintCollectors.max(groupValueMapping);
    }

    /**
     * As defined by {@link #max(Function, Function)}, only with a custom {@link Comparator}.
     */
    public static <A, B, Mapped, Comparable_ extends Comparable<? super Comparable_>> BiConstraintCollector<A, B, ?, Mapped>
            max(BiFunction<A, B, Mapped> groupValueMapping, Function<Mapped, Comparable_> comparableFunction) {
        return InnerBiConstraintCollectors.max(groupValueMapping, comparableFunction);
    }

    /**
     * As defined by {@link #max()}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #max(BiFunction, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A, B, Mapped> BiConstraintCollector<A, B, ?, Mapped> max(BiFunction<A, B, Mapped> groupValueMapping,
            Comparator<? super Mapped> comparator) {
        return InnerBiConstraintCollectors.max(groupValueMapping, comparator);
    }

    /**
     * As defined by {@link #max(Function)}.
     */
    public static <A, B, C, Mapped extends Comparable<? super Mapped>> TriConstraintCollector<A, B, C, ?, Mapped> max(
            TriFunction<A, B, C, Mapped> groupValueMapping) {
        return InnerTriConstraintCollectors.max(groupValueMapping);
    }

    /**
     * As defined by {@link #max(Function, Function)}, only with a custom {@link Comparator}.
     */
    public static <A, B, C, Mapped, Comparable_ extends Comparable<? super Comparable_>>
            TriConstraintCollector<A, B, C, ?, Mapped>
            max(TriFunction<A, B, C, Mapped> groupValueMapping, Function<Mapped, Comparable_> comparableFunction) {
        return InnerTriConstraintCollectors.max(groupValueMapping, comparableFunction);
    }

    /**
     * As defined by {@link #max()}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #max(TriFunction, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A, B, C, Mapped> TriConstraintCollector<A, B, C, ?, Mapped> max(
            TriFunction<A, B, C, Mapped> groupValueMapping, Comparator<? super Mapped> comparator) {
        return InnerTriConstraintCollectors.max(groupValueMapping, comparator);
    }

    /**
     * As defined by {@link #max(Function)}.
     */
    public static <A, B, C, D, Mapped extends Comparable<? super Mapped>> QuadConstraintCollector<A, B, C, D, ?, Mapped> max(
            QuadFunction<A, B, C, D, Mapped> groupValueMapping) {
        return InnerQuadConstraintCollectors.max(groupValueMapping);
    }

    /**
     * As defined by {@link #max(Function, Function)}, only with a custom {@link Comparator}.
     */
    public static <A, B, C, D, Mapped, Comparable_ extends Comparable<? super Comparable_>>
            QuadConstraintCollector<A, B, C, D, ?, Mapped>
            max(QuadFunction<A, B, C, D, Mapped> groupValueMapping, Function<Mapped, Comparable_> comparableFunction) {
        return InnerQuadConstraintCollectors.max(groupValueMapping, comparableFunction);
    }

    /**
     * As defined by {@link #max()}, only with a custom {@link Comparator}.
     *
     * @deprecated Deprecated in favor of {@link #max(QuadFunction, Function)},
     *             as this method can lead to unavoidable score corruptions.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static <A, B, C, D, Mapped> QuadConstraintCollector<A, B, C, D, ?, Mapped> max(
            QuadFunction<A, B, C, D, Mapped> groupValueMapping, Comparator<? super Mapped> comparator) {
        return InnerQuadConstraintCollectors.max(groupValueMapping, comparator);
    }

    /**
     * @deprecated Prefer {@link #toList()}, {@link #toSet()} or {@link #toSortedSet()}
     */
    @Deprecated(/* forRemoval = true */)
    public static <A, Result extends Collection<A>> UniConstraintCollector<A, ?, Result> toCollection(
            IntFunction<Result> collectionFunction) {
        return toCollection(ConstantLambdaUtils.identity(), collectionFunction);
    }

    // ************************************************************************
    // average
    // ************************************************************************

    /**
     * Returns a collector that calculates an average of an {@code int} property of the elements that are being grouped.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]} with
     * {@code .groupBy(average(Person::getAge))} returns {@code 25}.
     * <p>
     * The default result of the collector (e.g. when never called) is {@code null}.
     *
     * @param <A> type of the matched fact
     * @return never null
     */
    public static <A> UniConstraintCollector<A, ?, Double> average(ToIntFunction<A> groupValueMapping) {
        return InnerUniConstraintCollectors.average(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A> UniConstraintCollector<A, ?, Double> averageLong(ToLongFunction<A> groupValueMapping) {
        return InnerUniConstraintCollectors.average(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     * The scale of the resulting {@link BigDecimal} will be equal to the scale of the sum of all the input tuples,
     * with rounding mode {@link RoundingMode#HALF_EVEN}.
     */
    public static <A> UniConstraintCollector<A, ?, BigDecimal> averageBigDecimal(
            Function<A, BigDecimal> groupValueMapping) {
        return InnerUniConstraintCollectors.averageBigDecimal(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     * The scale of the resulting {@link BigDecimal} will be equal to the scale of the sum of all the input tuples,
     * with rounding mode {@link RoundingMode#HALF_EVEN}.
     */
    public static <A> UniConstraintCollector<A, ?, BigDecimal> averageBigInteger(Function<A, BigInteger> groupValueMapping) {
        return InnerUniConstraintCollectors.averageBigInteger(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A> UniConstraintCollector<A, ?, Duration> averageDuration(Function<A, Duration> groupValueMapping) {
        return InnerUniConstraintCollectors.averageDuration(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Double> average(ToIntBiFunction<A, B> groupValueMapping) {
        return InnerBiConstraintCollectors.average(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Double> averageLong(ToLongBiFunction<A, B> groupValueMapping) {
        return InnerBiConstraintCollectors.average(groupValueMapping);
    }

    /**
     * As defined by {@link #averageBigDecimal(Function)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, BigDecimal>
            averageBigDecimal(BiFunction<A, B, BigDecimal> groupValueMapping) {
        return InnerBiConstraintCollectors.averageBigDecimal(groupValueMapping);
    }

    /**
     * As defined by {@link #averageBigInteger(Function)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, BigDecimal>
            averageBigInteger(BiFunction<A, B, BigInteger> groupValueMapping) {
        return InnerBiConstraintCollectors.averageBigInteger(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A, B> BiConstraintCollector<A, B, ?, Duration>
            averageDuration(BiFunction<A, B, Duration> groupValueMapping) {
        return InnerBiConstraintCollectors.averageDuration(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Double> average(ToIntTriFunction<A, B, C> groupValueMapping) {
        return InnerTriConstraintCollectors.average(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Double>
            averageLong(ToLongTriFunction<A, B, C> groupValueMapping) {
        return InnerTriConstraintCollectors.average(groupValueMapping);
    }

    /**
     * As defined by {@link #averageBigDecimal(Function)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, BigDecimal>
            averageBigDecimal(TriFunction<A, B, C, BigDecimal> groupValueMapping) {
        return InnerTriConstraintCollectors.averageBigDecimal(groupValueMapping);
    }

    /**
     * As defined by {@link #averageBigInteger(Function)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, BigDecimal>
            averageBigInteger(TriFunction<A, B, C, BigInteger> groupValueMapping) {
        return InnerTriConstraintCollectors.averageBigInteger(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Duration>
            averageDuration(TriFunction<A, B, C, Duration> groupValueMapping) {
        return InnerTriConstraintCollectors.averageDuration(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Double>
            average(ToIntQuadFunction<A, B, C, D> groupValueMapping) {
        return InnerQuadConstraintCollectors.average(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Double>
            averageLong(ToLongQuadFunction<A, B, C, D> groupValueMapping) {
        return InnerQuadConstraintCollectors.average(groupValueMapping);
    }

    /**
     * As defined by {@link #averageBigDecimal(Function)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, BigDecimal>
            averageBigDecimal(QuadFunction<A, B, C, D, BigDecimal> groupValueMapping) {
        return InnerQuadConstraintCollectors.averageBigDecimal(groupValueMapping);
    }

    /**
     * As defined by {@link #averageBigInteger(Function)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, BigDecimal>
            averageBigInteger(QuadFunction<A, B, C, D, BigInteger> groupValueMapping) {
        return InnerQuadConstraintCollectors.averageBigInteger(groupValueMapping);
    }

    /**
     * As defined by {@link #average(ToIntFunction)}.
     */
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Duration>
            averageDuration(QuadFunction<A, B, C, D, Duration> groupValueMapping) {
        return InnerQuadConstraintCollectors.averageDuration(groupValueMapping);
    }

    // ************************************************************************
    // toCollection
    // ************************************************************************

    /**
     * Creates constraint collector that returns {@link Set} of the same element type as the {@link ConstraintStream}.
     * Makes no guarantees on iteration order.
     * For stable iteration order, use {@link #toSortedSet()}.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link Set}.
     *
     * @param <A> type of the matched fact
     * @return never null
     */
    public static <A> UniConstraintCollector<A, ?, Set<A>> toSet() {
        return toSet(ConstantLambdaUtils.identity());
    }

    /**
     * Creates constraint collector that returns {@link SortedSet} of the same element type as the
     * {@link ConstraintStream}.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link SortedSet}.
     *
     * @param <A> type of the matched fact
     * @return never null
     */
    public static <A extends Comparable<A>> UniConstraintCollector<A, ?, SortedSet<A>> toSortedSet() {
        return toSortedSet(ConstantLambdaUtils.<A> identity());
    }

    /**
     * As defined by {@link #toSortedSet()}, only with a custom {@link Comparator}.
     */
    public static <A> UniConstraintCollector<A, ?, SortedSet<A>> toSortedSet(Comparator<? super A> comparator) {
        return toSortedSet(ConstantLambdaUtils.identity(), comparator);
    }

    /**
     * Creates constraint collector that returns {@link List} of the same element type as the {@link ConstraintStream}.
     * Makes no guarantees on iteration order.
     * For stable iteration order, use {@link #toSortedSet()}.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link List}.
     *
     * @param <A> type of the matched fact
     * @return never null
     */
    public static <A> UniConstraintCollector<A, ?, List<A>> toList() {
        return toList(ConstantLambdaUtils.identity());
    }

    /**
     * @deprecated Prefer {@link #toList(Function)}, {@link #toSet(Function)} or {@link #toSortedSet(Function)}
     */
    @Deprecated(/* forRemoval = true */)
    public static <A, Mapped, Result extends Collection<Mapped>> UniConstraintCollector<A, ?, Result> toCollection(
            Function<A, Mapped> groupValueMapping, IntFunction<Result> collectionFunction) {
        return InnerUniConstraintCollectors.toCollection(groupValueMapping, collectionFunction);
    }

    /**
     * Creates constraint collector that returns {@link Set} of the same element type as the {@link ConstraintStream}.
     * Makes no guarantees on iteration order.
     * For stable iteration order, use {@link #toSortedSet()}.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link Set}.
     *
     * @param groupValueMapping never null, converts matched facts to elements of the resulting set
     * @param <A> type of the matched fact
     * @param <Mapped> type of elements in the resulting set
     * @return never null
     */
    public static <A, Mapped> UniConstraintCollector<A, ?, Set<Mapped>> toSet(Function<A, Mapped> groupValueMapping) {
        return InnerUniConstraintCollectors.toSet(groupValueMapping);
    }

    /**
     * Creates constraint collector that returns {@link SortedSet} of the same element type as the
     * {@link ConstraintStream}.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link SortedSet}.
     *
     * @param groupValueMapping never null, converts matched facts to elements of the resulting set
     * @param <A> type of the matched fact
     * @param <Mapped> type of elements in the resulting set
     * @return never null
     */
    public static <A, Mapped extends Comparable<? super Mapped>> UniConstraintCollector<A, ?, SortedSet<Mapped>> toSortedSet(
            Function<A, Mapped> groupValueMapping) {
        return toSortedSet(groupValueMapping, Comparator.naturalOrder());
    }

    /**
     * As defined by {@link #toSortedSet(Function)}, only with a custom {@link Comparator}.
     */
    public static <A, Mapped> UniConstraintCollector<A, ?, SortedSet<Mapped>> toSortedSet(
            Function<A, Mapped> groupValueMapping, Comparator<? super Mapped> comparator) {
        return InnerUniConstraintCollectors.toSortedSet(groupValueMapping, comparator);
    }

    /**
     * Creates constraint collector that returns {@link List} of the given element type.
     * Makes no guarantees on iteration order.
     * For stable iteration order, use {@link #toSortedSet(Function)}.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link List}.
     *
     * @param groupValueMapping never null, converts matched facts to elements of the resulting collection
     * @param <A> type of the matched fact
     * @param <Mapped> type of elements in the resulting collection
     * @return never null
     */
    public static <A, Mapped> UniConstraintCollector<A, ?, List<Mapped>> toList(Function<A, Mapped> groupValueMapping) {
        return InnerUniConstraintCollectors.toList(groupValueMapping);
    }

    /**
     * @deprecated Prefer {@link #toList(BiFunction)}, {@link #toSet(BiFunction)}
     *             or {@link #toSortedSet(BiFunction)}
     */
    @Deprecated(/* forRemoval = true */)
    public static <A, B, Mapped, Result extends Collection<Mapped>> BiConstraintCollector<A, B, ?, Result> toCollection(
            BiFunction<A, B, Mapped> groupValueMapping, IntFunction<Result> collectionFunction) {
        return InnerBiConstraintCollectors.toCollection(groupValueMapping, collectionFunction);
    }

    /**
     * As defined by {@link #toSet(Function)}.
     */
    public static <A, B, Mapped> BiConstraintCollector<A, B, ?, Set<Mapped>> toSet(
            BiFunction<A, B, Mapped> groupValueMapping) {
        return InnerBiConstraintCollectors.toSet(groupValueMapping);
    }

    /**
     * As defined by {@link #toSortedSet(Function)}.
     */
    public static <A, B, Mapped extends Comparable<? super Mapped>> BiConstraintCollector<A, B, ?, SortedSet<Mapped>>
            toSortedSet(
                    BiFunction<A, B, Mapped> groupValueMapping) {
        return toSortedSet(groupValueMapping, Comparator.naturalOrder());
    }

    /**
     * As defined by {@link #toSortedSet(Function, Comparator)}.
     */
    public static <A, B, Mapped> BiConstraintCollector<A, B, ?, SortedSet<Mapped>> toSortedSet(
            BiFunction<A, B, Mapped> groupValueMapping, Comparator<? super Mapped> comparator) {
        return InnerBiConstraintCollectors.toSortedSet(groupValueMapping, comparator);
    }

    /**
     * As defined by {@link #toList(Function)}.
     */
    public static <A, B, Mapped> BiConstraintCollector<A, B, ?, List<Mapped>> toList(
            BiFunction<A, B, Mapped> groupValueMapping) {
        return InnerBiConstraintCollectors.toList(groupValueMapping);
    }

    /**
     * @deprecated Prefer {@link #toList(TriFunction)}, {@link #toSet(TriFunction)}
     *             or {@link #toSortedSet(TriFunction)}
     */
    @Deprecated(/* forRemoval = true */)
    public static <A, B, C, Mapped, Result extends Collection<Mapped>> TriConstraintCollector<A, B, C, ?, Result> toCollection(
            TriFunction<A, B, C, Mapped> groupValueMapping, IntFunction<Result> collectionFunction) {
        return InnerTriConstraintCollectors.toCollection(groupValueMapping, collectionFunction);
    }

    /**
     * As defined by {@link #toSet(Function)}.
     */
    public static <A, B, C, Mapped> TriConstraintCollector<A, B, C, ?, Set<Mapped>> toSet(
            TriFunction<A, B, C, Mapped> groupValueMapping) {
        return InnerTriConstraintCollectors.toSet(groupValueMapping);
    }

    /**
     * As defined by {@link #toSortedSet(Function)}.
     */
    public static <A, B, C, Mapped extends Comparable<? super Mapped>> TriConstraintCollector<A, B, C, ?, SortedSet<Mapped>>
            toSortedSet(TriFunction<A, B, C, Mapped> groupValueMapping) {
        return toSortedSet(groupValueMapping, Comparator.naturalOrder());
    }

    /**
     * As defined by {@link #toSortedSet(Function, Comparator)}.
     */
    public static <A, B, C, Mapped> TriConstraintCollector<A, B, C, ?, SortedSet<Mapped>> toSortedSet(
            TriFunction<A, B, C, Mapped> groupValueMapping, Comparator<? super Mapped> comparator) {
        return InnerTriConstraintCollectors.toSortedSet(groupValueMapping, comparator);
    }

    /**
     * As defined by {@link #toList(Function)}.
     */
    public static <A, B, C, Mapped> TriConstraintCollector<A, B, C, ?, List<Mapped>> toList(
            TriFunction<A, B, C, Mapped> groupValueMapping) {
        return InnerTriConstraintCollectors.toList(groupValueMapping);
    }

    /**
     * @deprecated Prefer {@link #toList(QuadFunction)}, {@link #toSet(QuadFunction)}
     *             or {@link #toSortedSet(QuadFunction)}
     */
    @Deprecated(/* forRemoval = true */)
    public static <A, B, C, D, Mapped, Result extends Collection<Mapped>> QuadConstraintCollector<A, B, C, D, ?, Result>
            toCollection(QuadFunction<A, B, C, D, Mapped> groupValueMapping, IntFunction<Result> collectionFunction) {
        return InnerQuadConstraintCollectors.toCollection(groupValueMapping, collectionFunction);
    }

    /**
     * As defined by {@link #toSet(Function)}.
     */
    public static <A, B, C, D, Mapped> QuadConstraintCollector<A, B, C, D, ?, Set<Mapped>> toSet(
            QuadFunction<A, B, C, D, Mapped> groupValueMapping) {
        return InnerQuadConstraintCollectors.toSet(groupValueMapping);
    }

    /**
     * As defined by {@link #toSortedSet(Function)}.
     */
    public static <A, B, C, D, Mapped extends Comparable<? super Mapped>>
            QuadConstraintCollector<A, B, C, D, ?, SortedSet<Mapped>>
            toSortedSet(QuadFunction<A, B, C, D, Mapped> groupValueMapping) {
        return toSortedSet(groupValueMapping, Comparator.naturalOrder());
    }

    /**
     * As defined by {@link #toSortedSet(Function, Comparator)}.
     */
    public static <A, B, C, D, Mapped> QuadConstraintCollector<A, B, C, D, ?, SortedSet<Mapped>> toSortedSet(
            QuadFunction<A, B, C, D, Mapped> groupValueMapping, Comparator<? super Mapped> comparator) {
        return InnerQuadConstraintCollectors.toSortedSet(groupValueMapping, comparator);
    }

    /**
     * As defined by {@link #toList(Function)}.
     */
    public static <A, B, C, D, Mapped> QuadConstraintCollector<A, B, C, D, ?, List<Mapped>> toList(
            QuadFunction<A, B, C, D, Mapped> groupValueMapping) {
        return InnerQuadConstraintCollectors.toList(groupValueMapping);
    }

    // ************************************************************************
    // toMap
    // ************************************************************************

    /**
     * Creates a constraint collector that returns a {@link Map} with given keys and values consisting of a
     * {@link Set} of mappings.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]}
     * with {@code .groupBy(toMap(Person::getAge, Person::getName))} returns
     * {@code {20: [Ann, Eric], 25: [Beth], 30: [Cathy, David]}}.
     * <p>
     * Makes no guarantees on iteration order, neither for map entries, nor for the value sets.
     * For stable iteration order, use {@link #toSortedMap(Function, Function, IntFunction)}.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link Map}.
     *
     * @param keyMapper map matched fact to a map key
     * @param valueMapper map matched fact to a value
     * @param <A> type of the matched fact
     * @param <Key> type of map key
     * @param <Value> type of map value
     * @return never null
     */
    public static <A, Key, Value> UniConstraintCollector<A, ?, Map<Key, Set<Value>>> toMap(
            Function<? super A, ? extends Key> keyMapper, Function<? super A, ? extends Value> valueMapper) {
        return toMap(keyMapper, valueMapper, (IntFunction<Set<Value>>) LinkedHashSet::new);
    }

    /**
     * Creates a constraint collector that returns a {@link Map} with given keys and values consisting of a
     * {@link Set} of mappings.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]}
     * with {@code .groupBy(toMap(Person::getAge, Person::getName))} returns
     * {@code {20: [Ann, Eric], 25: [Beth], 30: [Cathy, David]}}.
     * <p>
     * Iteration order of value collections depends on the {@link Set} provided.
     * Makes no guarantees on iteration order for map entries, use {@link #toSortedMap(Function, Function, IntFunction)}
     * for that.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link Map}.
     *
     * @param keyMapper map matched fact to a map key
     * @param valueMapper map matched fact to a value
     * @param valueSetFunction creates a set that will be used to store value mappings
     * @param <A> type of the matched fact
     * @param <Key> type of map key
     * @param <Value> type of map value
     * @param <ValueSet> type of the value set
     * @return never null
     */
    public static <A, Key, Value, ValueSet extends Set<Value>> UniConstraintCollector<A, ?, Map<Key, ValueSet>> toMap(
            Function<? super A, ? extends Key> keyMapper, Function<? super A, ? extends Value> valueMapper,
            IntFunction<ValueSet> valueSetFunction) {
        return InnerUniConstraintCollectors.toMap(keyMapper, valueMapper, HashMap::new, valueSetFunction);
    }

    /**
     * Creates a constraint collector that returns a {@link Map}.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]}
     * with {@code .groupBy(toMap(Person::getAge, Person::getName, (name1, name2) -> name1 + " and " + name2)} returns
     * {@code {20: "Ann and Eric", 25: "Beth", 30: "Cathy and David"}}.
     * <p>
     * Makes no guarantees on iteration order for map entries.
     * For stable iteration order, use {@link #toSortedMap(Function, Function, BinaryOperator)}.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link Map}.
     *
     * @param keyMapper map matched fact to a map key
     * @param valueMapper map matched fact to a value
     * @param mergeFunction takes two values and merges them to one
     * @param <A> type of the matched fact
     * @param <Key> type of map key
     * @param <Value> type of map value
     * @return never null
     */
    public static <A, Key, Value> UniConstraintCollector<A, ?, Map<Key, Value>> toMap(
            Function<? super A, ? extends Key> keyMapper, Function<? super A, ? extends Value> valueMapper,
            BinaryOperator<Value> mergeFunction) {
        return InnerUniConstraintCollectors.toMap(keyMapper, valueMapper, HashMap::new, mergeFunction);
    }

    /**
     * Creates a constraint collector that returns a {@link SortedMap} with given keys and values consisting of a
     * {@link Set} of mappings.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]}
     * with {@code .groupBy(toMap(Person::getAge, Person::getName))} returns
     * {@code {20: [Ann, Eric], 25: [Beth], 30: [Cathy, David]}}.
     * <p>
     * Makes no guarantees on iteration order for the value sets, use
     * {@link #toSortedMap(Function, Function, IntFunction)} for that.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link SortedMap}.
     *
     * @param keyMapper map matched fact to a map key
     * @param valueMapper map matched fact to a value
     * @param <A> type of the matched fact
     * @param <Key> type of map key
     * @param <Value> type of map value
     * @return never null
     */
    public static <A, Key extends Comparable<? super Key>, Value> UniConstraintCollector<A, ?, SortedMap<Key, Set<Value>>>
            toSortedMap(
                    Function<? super A, ? extends Key> keyMapper, Function<? super A, ? extends Value> valueMapper) {
        return toSortedMap(keyMapper, valueMapper, (IntFunction<Set<Value>>) LinkedHashSet::new);
    }

    /**
     * Creates a constraint collector that returns a {@link SortedMap} with given keys and values consisting of a
     * {@link Set} of mappings.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]}
     * with {@code .groupBy(toMap(Person::getAge, Person::getName))} returns
     * {@code {20: [Ann, Eric], 25: [Beth], 30: [Cathy, David]}}.
     * <p>
     * Iteration order of value collections depends on the {@link Set} provided.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link SortedMap}.
     *
     * @param keyMapper map matched fact to a map key
     * @param valueMapper map matched fact to a value
     * @param valueSetFunction creates a set that will be used to store value mappings
     * @param <A> type of the matched fact
     * @param <Key> type of map key
     * @param <Value> type of map value
     * @param <ValueSet> type of the value set
     * @return never null
     */
    public static <A, Key extends Comparable<? super Key>, Value, ValueSet extends Set<Value>>
            UniConstraintCollector<A, ?, SortedMap<Key, ValueSet>> toSortedMap(
                    Function<? super A, ? extends Key> keyMapper,
                    Function<? super A, ? extends Value> valueMapper, IntFunction<ValueSet> valueSetFunction) {
        return InnerUniConstraintCollectors.toMap(keyMapper, valueMapper, TreeMap::new, valueSetFunction);
    }

    /**
     * Creates a constraint collector that returns a {@link SortedMap}.
     * <p>
     * For example, {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]}
     * with {@code .groupBy(toMap(Person::getAge, Person::getName, (name1, name2) -> name1 + " and " + name2)} returns
     * {@code {20: "Ann and Eric", 25: "Beth", 30: "Cathy and David"}}.
     * <p>
     * The default result of the collector (e.g. when never called) is an empty {@link SortedMap}.
     *
     * @param keyMapper map matched fact to a map key
     * @param valueMapper map matched fact to a value
     * @param mergeFunction takes two values and merges them to one
     * @param <A> type of the matched fact
     * @param <Key> type of map key
     * @param <Value> type of map value
     * @return never null
     */
    public static <A, Key extends Comparable<? super Key>, Value> UniConstraintCollector<A, ?, SortedMap<Key, Value>>
            toSortedMap(
                    Function<? super A, ? extends Key> keyMapper, Function<? super A, ? extends Value> valueMapper,
                    BinaryOperator<Value> mergeFunction) {
        return InnerUniConstraintCollectors.toMap(keyMapper, valueMapper, TreeMap::new, mergeFunction);
    }

    /**
     * As defined by {@link #toMap(Function, Function)}.
     */
    public static <A, B, Key, Value> BiConstraintCollector<A, B, ?, Map<Key, Set<Value>>> toMap(
            BiFunction<? super A, ? super B, ? extends Key> keyMapper,
            BiFunction<? super A, ? super B, ? extends Value> valueMapper) {
        return toMap(keyMapper, valueMapper, (IntFunction<Set<Value>>) LinkedHashSet::new);
    }

    /**
     * As defined by {@link #toMap(Function, Function, IntFunction)}.
     */
    public static <A, B, Key, Value, ValueSet extends Set<Value>> BiConstraintCollector<A, B, ?, Map<Key, ValueSet>> toMap(
            BiFunction<? super A, ? super B, ? extends Key> keyMapper,
            BiFunction<? super A, ? super B, ? extends Value> valueMapper, IntFunction<ValueSet> valueSetFunction) {
        return InnerBiConstraintCollectors.toMap(keyMapper, valueMapper, HashMap::new, valueSetFunction);
    }

    /**
     * As defined by {@link #toMap(Function, Function, BinaryOperator)}.
     */
    public static <A, B, Key, Value> BiConstraintCollector<A, B, ?, Map<Key, Value>> toMap(
            BiFunction<? super A, ? super B, ? extends Key> keyMapper,
            BiFunction<? super A, ? super B, ? extends Value> valueMapper, BinaryOperator<Value> mergeFunction) {
        return InnerBiConstraintCollectors.toMap(keyMapper, valueMapper, HashMap::new, mergeFunction);
    }

    /**
     * As defined by {@link #toSortedMap(Function, Function)}.
     */
    public static <A, B, Key extends Comparable<? super Key>, Value> BiConstraintCollector<A, B, ?, SortedMap<Key, Set<Value>>>
            toSortedMap(BiFunction<? super A, ? super B, ? extends Key> keyMapper,
                    BiFunction<? super A, ? super B, ? extends Value> valueMapper) {
        return toSortedMap(keyMapper, valueMapper, (IntFunction<Set<Value>>) LinkedHashSet::new);
    }

    /**
     * As defined by {@link #toSortedMap(Function, Function, IntFunction)}.
     */
    public static <A, B, Key extends Comparable<? super Key>, Value, ValueSet extends Set<Value>>
            BiConstraintCollector<A, B, ?, SortedMap<Key, ValueSet>> toSortedMap(
                    BiFunction<? super A, ? super B, ? extends Key> keyMapper,
                    BiFunction<? super A, ? super B, ? extends Value> valueMapper, IntFunction<ValueSet> valueSetFunction) {
        return InnerBiConstraintCollectors.toMap(keyMapper, valueMapper, TreeMap::new, valueSetFunction);
    }

    /**
     * As defined by {@link #toSortedMap(Function, Function, BinaryOperator)}.
     */
    public static <A, B, Key extends Comparable<? super Key>, Value> BiConstraintCollector<A, B, ?, SortedMap<Key, Value>>
            toSortedMap(
                    BiFunction<? super A, ? super B, ? extends Key> keyMapper,
                    BiFunction<? super A, ? super B, ? extends Value> valueMapper, BinaryOperator<Value> mergeFunction) {
        return InnerBiConstraintCollectors.toMap(keyMapper, valueMapper, TreeMap::new, mergeFunction);
    }

    /**
     * As defined by {@link #toMap(Function, Function)}.
     */
    public static <A, B, C, Key, Value> TriConstraintCollector<A, B, C, ?, Map<Key, Set<Value>>> toMap(
            TriFunction<? super A, ? super B, ? super C, ? extends Key> keyMapper,
            TriFunction<? super A, ? super B, ? super C, ? extends Value> valueMapper) {
        return toMap(keyMapper, valueMapper, (IntFunction<Set<Value>>) LinkedHashSet::new);
    }

    /**
     * As defined by {@link #toMap(Function, Function, IntFunction)}.
     */
    public static <A, B, C, Key, Value, ValueSet extends Set<Value>> TriConstraintCollector<A, B, C, ?, Map<Key, ValueSet>>
            toMap(TriFunction<? super A, ? super B, ? super C, ? extends Key> keyMapper,
                    TriFunction<? super A, ? super B, ? super C, ? extends Value> valueMapper,
                    IntFunction<ValueSet> valueSetFunction) {
        return InnerTriConstraintCollectors.toMap(keyMapper, valueMapper, HashMap::new, valueSetFunction);
    }

    /**
     * As defined by {@link #toMap(Function, Function, BinaryOperator)}.
     */
    public static <A, B, C, Key, Value> TriConstraintCollector<A, B, C, ?, Map<Key, Value>> toMap(
            TriFunction<? super A, ? super B, ? super C, ? extends Key> keyMapper,
            TriFunction<? super A, ? super B, ? super C, ? extends Value> valueMapper,
            BinaryOperator<Value> mergeFunction) {
        return InnerTriConstraintCollectors.toMap(keyMapper, valueMapper, HashMap::new, mergeFunction);
    }

    /**
     * As defined by {@link #toSortedMap(Function, Function)}.
     */
    public static <A, B, C, Key extends Comparable<? super Key>, Value>
            TriConstraintCollector<A, B, C, ?, SortedMap<Key, Set<Value>>>
            toSortedMap(TriFunction<? super A, ? super B, ? super C, ? extends Key> keyMapper,
                    TriFunction<? super A, ? super B, ? super C, ? extends Value> valueMapper) {
        return toSortedMap(keyMapper, valueMapper, (IntFunction<Set<Value>>) LinkedHashSet::new);
    }

    /**
     * As defined by {@link #toSortedMap(Function, Function, IntFunction)}.
     */
    public static <A, B, C, Key extends Comparable<? super Key>, Value, ValueSet extends Set<Value>>
            TriConstraintCollector<A, B, C, ?, SortedMap<Key, ValueSet>> toSortedMap(
                    TriFunction<? super A, ? super B, ? super C, ? extends Key> keyMapper,
                    TriFunction<? super A, ? super B, ? super C, ? extends Value> valueMapper,
                    IntFunction<ValueSet> valueSetFunction) {
        return InnerTriConstraintCollectors.toMap(keyMapper, valueMapper, TreeMap::new, valueSetFunction);
    }

    /**
     * As defined by {@link #toSortedMap(Function, Function, BinaryOperator)}.
     */
    public static <A, B, C, Key extends Comparable<? super Key>, Value>
            TriConstraintCollector<A, B, C, ?, SortedMap<Key, Value>>
            toSortedMap(TriFunction<? super A, ? super B, ? super C, ? extends Key> keyMapper,
                    TriFunction<? super A, ? super B, ? super C, ? extends Value> valueMapper,
                    BinaryOperator<Value> mergeFunction) {
        return InnerTriConstraintCollectors.toMap(keyMapper, valueMapper, TreeMap::new, mergeFunction);
    }

    /**
     * As defined by {@link #toMap(Function, Function)}.
     */
    public static <A, B, C, D, Key, Value> QuadConstraintCollector<A, B, C, D, ?, Map<Key, Set<Value>>> toMap(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key> keyMapper,
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value> valueMapper) {
        return toMap(keyMapper, valueMapper, (IntFunction<Set<Value>>) LinkedHashSet::new);
    }

    /**
     * As defined by {@link #toMap(Function, Function, IntFunction)}.
     */
    public static <A, B, C, D, Key, Value, ValueSet extends Set<Value>>
            QuadConstraintCollector<A, B, C, D, ?, Map<Key, ValueSet>> toMap(
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key> keyMapper,
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value> valueMapper,
                    IntFunction<ValueSet> valueSetFunction) {
        return InnerQuadConstraintCollectors.toMap(keyMapper, valueMapper, HashMap::new, valueSetFunction);
    }

    /**
     * As defined by {@link #toMap(Function, Function, BinaryOperator)}.
     */
    public static <A, B, C, D, Key, Value> QuadConstraintCollector<A, B, C, D, ?, Map<Key, Value>> toMap(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key> keyMapper,
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value> valueMapper,
            BinaryOperator<Value> mergeFunction) {
        return InnerQuadConstraintCollectors.toMap(keyMapper, valueMapper, HashMap::new, mergeFunction);
    }

    /**
     * As defined by {@link #toSortedMap(Function, Function)}.
     */
    public static <A, B, C, D, Key extends Comparable<? super Key>, Value>
            QuadConstraintCollector<A, B, C, D, ?, SortedMap<Key, Set<Value>>> toSortedMap(
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key> keyMapper,
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value> valueMapper) {
        return toSortedMap(keyMapper, valueMapper, (IntFunction<Set<Value>>) LinkedHashSet::new);
    }

    /**
     * As defined by {@link #toSortedMap(Function, Function, IntFunction)}.
     */
    public static <A, B, C, D, Key extends Comparable<? super Key>, Value, ValueSet extends Set<Value>>
            QuadConstraintCollector<A, B, C, D, ?, SortedMap<Key, ValueSet>> toSortedMap(
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key> keyMapper,
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value> valueMapper,
                    IntFunction<ValueSet> valueSetFunction) {
        return InnerQuadConstraintCollectors.toMap(keyMapper, valueMapper, TreeMap::new, valueSetFunction);
    }

    /**
     * As defined by {@link #toSortedMap(Function, Function, BinaryOperator)}.
     */
    public static <A, B, C, D, Key extends Comparable<? super Key>, Value>
            QuadConstraintCollector<A, B, C, D, ?, SortedMap<Key, Value>>
            toSortedMap(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key> keyMapper,
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value> valueMapper,
                    BinaryOperator<Value> mergeFunction) {
        return InnerQuadConstraintCollectors.toMap(keyMapper, valueMapper, TreeMap::new, mergeFunction);
    }

    // ************************************************************************
    // conditional collectors
    // ************************************************************************

    /**
     * Returns a collector that delegates to the underlying collector
     * if and only if the input tuple meets the given condition.
     *
     * <p>
     * The result of the collector is always the underlying collector's result.
     * Therefore the default result of the collector (e.g. when never called) is the default result of the underlying collector.
     *
     * @param condition never null, condition to meet in order to delegate to the underlying collector
     * @param delegate never null, the underlying collector to delegate to
     * @param <A> generic type of the tuple variable
     * @param <ResultContainer_> generic type of the result container
     * @param <Result_> generic type of the collector's return value
     * @return never null
     */
    public static <A, ResultContainer_, Result_> UniConstraintCollector<A, ResultContainer_, Result_> conditionally(
            Predicate<A> condition, UniConstraintCollector<A, ResultContainer_, Result_> delegate) {
        return InnerUniConstraintCollectors.conditionally(condition, delegate);
    }

    /**
     * As defined by {@link #conditionally(Predicate, UniConstraintCollector)}.
     */
    public static <A, B, ResultContainer_, Result_> BiConstraintCollector<A, B, ResultContainer_, Result_>
            conditionally(BiPredicate<A, B> condition,
                    BiConstraintCollector<A, B, ResultContainer_, Result_> delegate) {
        return InnerBiConstraintCollectors.conditionally(condition, delegate);
    }

    /**
     * As defined by {@link #conditionally(Predicate, UniConstraintCollector)}.
     */
    public static <A, B, C, ResultContainer_, Result_> TriConstraintCollector<A, B, C, ResultContainer_, Result_>
            conditionally(TriPredicate<A, B, C> condition,
                    TriConstraintCollector<A, B, C, ResultContainer_, Result_> delegate) {
        return InnerTriConstraintCollectors.conditionally(condition, delegate);
    }

    /**
     * As defined by {@link #conditionally(Predicate, UniConstraintCollector)}.
     */
    public static <A, B, C, D, ResultContainer_, Result_> QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_>
            conditionally(QuadPredicate<A, B, C, D> condition,
                    QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> delegate) {
        return InnerQuadConstraintCollectors.conditionally(condition, delegate);
    }

    // ************************************************************************
    // forwarding collectors
    // ************************************************************************

    /**
     * Returns a collector that delegates to the underlying collector
     * and maps its result to another value.
     * <p>
     * This is a better performing alternative to {@code .groupBy(...).map(...)}.
     *
     * @param <A> generic type of the tuple variable
     * @param <Intermediate_> generic type of the delegate's return value
     * @param <Result_> generic type of the final colector's return value
     * @param delegate never null, the underlying collector to delegate to
     * @param mappingFunction never null, maps the result of the underlying collector to another value
     * @return never null
     */
    public static <A, Intermediate_, Result_> UniConstraintCollector<A, ?, Result_>
            collectAndThen(UniConstraintCollector<A, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return InnerUniConstraintCollectors.collectAndThen(delegate, mappingFunction);
    }

    /**
     * As defined by {@link #collectAndThen(UniConstraintCollector, Function)}.
     */
    public static <A, B, Intermediate_, Result_> BiConstraintCollector<A, B, ?, Result_>
            collectAndThen(BiConstraintCollector<A, B, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return InnerBiConstraintCollectors.collectAndThen(delegate, mappingFunction);
    }

    /**
     * As defined by {@link #collectAndThen(UniConstraintCollector, Function)}.
     */
    public static <A, B, C, Intermediate_, Result_> TriConstraintCollector<A, B, C, ?, Result_>
            collectAndThen(TriConstraintCollector<A, B, C, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return InnerTriConstraintCollectors.collectAndThen(delegate, mappingFunction);
    }

    /**
     * As defined by {@link #collectAndThen(UniConstraintCollector, Function)}.
     */
    public static <A, B, C, D, Intermediate_, Result_> QuadConstraintCollector<A, B, C, D, ?, Result_>
            collectAndThen(QuadConstraintCollector<A, B, C, D, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return InnerQuadConstraintCollectors.collectAndThen(delegate, mappingFunction);
    }

    // ************************************************************************
    // composite collectors
    // ************************************************************************

    /**
     * Returns a constraint collector the result of which is a composition of other constraint collectors.
     * The return value of this collector, incl. the default return value, depends solely on the compose function.
     *
     * @param subCollector1 never null, first collector to compose
     * @param subCollector2 never null, second collector to compose
     * @param composeFunction never null, turns results of the sub collectors to a result of the parent collector
     * @param <A> generic type of the tuple variable
     * @param <Result_> generic type of the parent collector's return value
     * @param <SubResultContainer1_> generic type of the first sub collector's result container
     * @param <SubResultContainer2_> generic type of the second sub collector's result container
     * @param <SubResult1_> generic type of the first sub collector's return value
     * @param <SubResult2_> generic type of the second sub collector's return value
     * @return never null
     */
    public static <A, Result_, SubResultContainer1_, SubResultContainer2_, SubResult1_, SubResult2_>
            UniConstraintCollector<A, ?, Result_> compose(
                    UniConstraintCollector<A, SubResultContainer1_, SubResult1_> subCollector1,
                    UniConstraintCollector<A, SubResultContainer2_, SubResult2_> subCollector2,
                    BiFunction<SubResult1_, SubResult2_, Result_> composeFunction) {
        return InnerUniConstraintCollectors.compose(subCollector1, subCollector2, composeFunction);
    }

    /**
     * Returns a constraint collector the result of which is a composition of other constraint collectors.
     * The return value of this collector, incl. the default return value, depends solely on the compose function.
     *
     * @param subCollector1 never null, first collector to compose
     * @param subCollector2 never null, second collector to compose
     * @param subCollector3 never null, third collector to compose
     * @param composeFunction never null, turns results of the sub collectors to a result of the parent collector
     * @param <A> generic type of the tuple variable
     * @param <Result_> generic type of the parent collector's return value
     * @param <SubResultContainer1_> generic type of the first sub collector's result container
     * @param <SubResultContainer2_> generic type of the second sub collector's result container
     * @param <SubResultContainer3_> generic type of the third sub collector's result container
     * @param <SubResult1_> generic type of the first sub collector's return value
     * @param <SubResult2_> generic type of the second sub collector's return value
     * @param <SubResult3_> generic type of the third sub collector's return value
     * @return never null
     */
    public static <A, Result_, SubResultContainer1_, SubResultContainer2_, SubResultContainer3_, SubResult1_, SubResult2_, SubResult3_>
            UniConstraintCollector<A, ?, Result_> compose(
                    UniConstraintCollector<A, SubResultContainer1_, SubResult1_> subCollector1,
                    UniConstraintCollector<A, SubResultContainer2_, SubResult2_> subCollector2,
                    UniConstraintCollector<A, SubResultContainer3_, SubResult3_> subCollector3,
                    TriFunction<SubResult1_, SubResult2_, SubResult3_, Result_> composeFunction) {
        return InnerUniConstraintCollectors.compose(subCollector1, subCollector2, subCollector3, composeFunction);
    }

    /**
     * Returns a constraint collector the result of which is a composition of other constraint collectors.
     * The return value of this collector, incl. the default return value, depends solely on the compose function.
     *
     * @param subCollector1 never null, first collector to compose
     * @param subCollector2 never null, second collector to compose
     * @param subCollector3 never null, third collector to compose
     * @param subCollector4 never null, fourth collector to compose
     * @param composeFunction never null, turns results of the sub collectors to a result of the parent collector
     * @param <A> generic type of the tuple variable
     * @param <Result_> generic type of the parent collector's return value
     * @param <SubResultContainer1_> generic type of the first sub collector's result container
     * @param <SubResultContainer2_> generic type of the second sub collector's result container
     * @param <SubResultContainer3_> generic type of the third sub collector's result container
     * @param <SubResultContainer4_> generic type of the fourth sub collector's result container
     * @param <SubResult1_> generic type of the first sub collector's return value
     * @param <SubResult2_> generic type of the second sub collector's return value
     * @param <SubResult3_> generic type of the third sub collector's return value
     * @param <SubResult4_> generic type of the fourth sub collector's return value
     * @return never null
     */
    public static <A, Result_, SubResultContainer1_, SubResultContainer2_, SubResultContainer3_, SubResultContainer4_, SubResult1_, SubResult2_, SubResult3_, SubResult4_>
            UniConstraintCollector<A, ?, Result_> compose(
                    UniConstraintCollector<A, SubResultContainer1_, SubResult1_> subCollector1,
                    UniConstraintCollector<A, SubResultContainer2_, SubResult2_> subCollector2,
                    UniConstraintCollector<A, SubResultContainer3_, SubResult3_> subCollector3,
                    UniConstraintCollector<A, SubResultContainer4_, SubResult4_> subCollector4,
                    QuadFunction<SubResult1_, SubResult2_, SubResult3_, SubResult4_, Result_> composeFunction) {
        return InnerUniConstraintCollectors.compose(subCollector1, subCollector2, subCollector3, subCollector4,
                composeFunction);
    }

    /**
     * As defined by {@link #compose(UniConstraintCollector, UniConstraintCollector, BiFunction)}.
     */
    public static <A, B, Result_, SubResultContainer1_, SubResultContainer2_, SubResult1_, SubResult2_>
            BiConstraintCollector<A, B, ?, Result_> compose(
                    BiConstraintCollector<A, B, SubResultContainer1_, SubResult1_> subCollector1,
                    BiConstraintCollector<A, B, SubResultContainer2_, SubResult2_> subCollector2,
                    BiFunction<SubResult1_, SubResult2_, Result_> composeFunction) {
        return InnerBiConstraintCollectors.compose(subCollector1, subCollector2, composeFunction);
    }

    /**
     * As defined by {@link #compose(UniConstraintCollector, UniConstraintCollector, UniConstraintCollector, TriFunction)}.
     */
    public static <A, B, Result_, SubResultContainer1_, SubResultContainer2_, SubResultContainer3_, SubResult1_, SubResult2_, SubResult3_>
            BiConstraintCollector<A, B, ?, Result_> compose(
                    BiConstraintCollector<A, B, SubResultContainer1_, SubResult1_> subCollector1,
                    BiConstraintCollector<A, B, SubResultContainer2_, SubResult2_> subCollector2,
                    BiConstraintCollector<A, B, SubResultContainer3_, SubResult3_> subCollector3,
                    TriFunction<SubResult1_, SubResult2_, SubResult3_, Result_> composeFunction) {
        return InnerBiConstraintCollectors.compose(subCollector1, subCollector2, subCollector3, composeFunction);
    }

    /**
     * As defined by
     * {@link #compose(UniConstraintCollector, UniConstraintCollector, UniConstraintCollector, UniConstraintCollector, QuadFunction)}.
     */
    public static <A, B, Result_, SubResultContainer1_, SubResultContainer2_, SubResultContainer3_, SubResultContainer4_, SubResult1_, SubResult2_, SubResult3_, SubResult4_>
            BiConstraintCollector<A, B, ?, Result_> compose(
                    BiConstraintCollector<A, B, SubResultContainer1_, SubResult1_> subCollector1,
                    BiConstraintCollector<A, B, SubResultContainer2_, SubResult2_> subCollector2,
                    BiConstraintCollector<A, B, SubResultContainer3_, SubResult3_> subCollector3,
                    BiConstraintCollector<A, B, SubResultContainer4_, SubResult4_> subCollector4,
                    QuadFunction<SubResult1_, SubResult2_, SubResult3_, SubResult4_, Result_> composeFunction) {
        return InnerBiConstraintCollectors.compose(subCollector1, subCollector2, subCollector3, subCollector4, composeFunction);
    }

    /**
     * As defined by {@link #compose(UniConstraintCollector, UniConstraintCollector, BiFunction)}.
     */
    public static <A, B, C, Result_, SubResultContainer1_, SubResultContainer2_, SubResult1_, SubResult2_>
            TriConstraintCollector<A, B, C, ?, Result_> compose(
                    TriConstraintCollector<A, B, C, SubResultContainer1_, SubResult1_> subCollector1,
                    TriConstraintCollector<A, B, C, SubResultContainer2_, SubResult2_> subCollector2,
                    BiFunction<SubResult1_, SubResult2_, Result_> composeFunction) {
        return InnerTriConstraintCollectors.compose(subCollector1, subCollector2, composeFunction);
    }

    /**
     * As defined by {@link #compose(UniConstraintCollector, UniConstraintCollector, UniConstraintCollector, TriFunction)}.
     */
    public static <A, B, C, Result_, SubResultContainer1_, SubResultContainer2_, SubResultContainer3_, SubResult1_, SubResult2_, SubResult3_>
            TriConstraintCollector<A, B, C, ?, Result_> compose(
                    TriConstraintCollector<A, B, C, SubResultContainer1_, SubResult1_> subCollector1,
                    TriConstraintCollector<A, B, C, SubResultContainer2_, SubResult2_> subCollector2,
                    TriConstraintCollector<A, B, C, SubResultContainer3_, SubResult3_> subCollector3,
                    TriFunction<SubResult1_, SubResult2_, SubResult3_, Result_> composeFunction) {
        return InnerTriConstraintCollectors.compose(subCollector1, subCollector2, subCollector3, composeFunction);
    }

    /**
     * As defined by
     * {@link #compose(UniConstraintCollector, UniConstraintCollector, UniConstraintCollector, UniConstraintCollector, QuadFunction)}.
     */
    public static <A, B, C, Result_, SubResultContainer1_, SubResultContainer2_, SubResultContainer3_, SubResultContainer4_, SubResult1_, SubResult2_, SubResult3_, SubResult4_>
            TriConstraintCollector<A, B, C, ?, Result_> compose(
                    TriConstraintCollector<A, B, C, SubResultContainer1_, SubResult1_> subCollector1,
                    TriConstraintCollector<A, B, C, SubResultContainer2_, SubResult2_> subCollector2,
                    TriConstraintCollector<A, B, C, SubResultContainer3_, SubResult3_> subCollector3,
                    TriConstraintCollector<A, B, C, SubResultContainer4_, SubResult4_> subCollector4,
                    QuadFunction<SubResult1_, SubResult2_, SubResult3_, SubResult4_, Result_> composeFunction) {
        return InnerTriConstraintCollectors.compose(subCollector1, subCollector2, subCollector3, subCollector4,
                composeFunction);
    }

    /**
     * As defined by {@link #compose(UniConstraintCollector, UniConstraintCollector, BiFunction)}.
     */
    public static <A, B, C, D, Result_, SubResultContainer1_, SubResultContainer2_, SubResult1_, SubResult2_>
            QuadConstraintCollector<A, B, C, D, ?, Result_> compose(
                    QuadConstraintCollector<A, B, C, D, SubResultContainer1_, SubResult1_> subCollector1,
                    QuadConstraintCollector<A, B, C, D, SubResultContainer2_, SubResult2_> subCollector2,
                    BiFunction<SubResult1_, SubResult2_, Result_> composeFunction) {
        return InnerQuadConstraintCollectors.compose(subCollector1, subCollector2, composeFunction);
    }

    /**
     * As defined by {@link #compose(UniConstraintCollector, UniConstraintCollector, UniConstraintCollector, TriFunction)}.
     */
    public static <A, B, C, D, Result_, SubResultContainer1_, SubResultContainer2_, SubResultContainer3_, SubResult1_, SubResult2_, SubResult3_>
            QuadConstraintCollector<A, B, C, D, ?, Result_> compose(
                    QuadConstraintCollector<A, B, C, D, SubResultContainer1_, SubResult1_> subCollector1,
                    QuadConstraintCollector<A, B, C, D, SubResultContainer2_, SubResult2_> subCollector2,
                    QuadConstraintCollector<A, B, C, D, SubResultContainer3_, SubResult3_> subCollector3,
                    TriFunction<SubResult1_, SubResult2_, SubResult3_, Result_> composeFunction) {
        return InnerQuadConstraintCollectors.compose(subCollector1, subCollector2, subCollector3, composeFunction);
    }

    /**
     * As defined by
     * {@link #compose(UniConstraintCollector, UniConstraintCollector, UniConstraintCollector, UniConstraintCollector, QuadFunction)}.
     */
    public static <A, B, C, D, Result_, SubResultContainer1_, SubResultContainer2_, SubResultContainer3_, SubResultContainer4_, SubResult1_, SubResult2_, SubResult3_, SubResult4_>
            QuadConstraintCollector<A, B, C, D, ?, Result_> compose(
                    QuadConstraintCollector<A, B, C, D, SubResultContainer1_, SubResult1_> subCollector1,
                    QuadConstraintCollector<A, B, C, D, SubResultContainer2_, SubResult2_> subCollector2,
                    QuadConstraintCollector<A, B, C, D, SubResultContainer3_, SubResult3_> subCollector3,
                    QuadConstraintCollector<A, B, C, D, SubResultContainer4_, SubResult4_> subCollector4,
                    QuadFunction<SubResult1_, SubResult2_, SubResult3_, SubResult4_, Result_> composeFunction) {
        return InnerQuadConstraintCollectors.compose(subCollector1, subCollector2, subCollector3, subCollector4,
                composeFunction);
    }

    // ************************************************************************
    // consecutive collectors
    // ************************************************************************

    /**
     * Creates a constraint collector that returns {@link SequenceChain} about the first fact.
     *
     * For instance, {@code [Shift slot=1] [Shift slot=2] [Shift slot=4] [Shift slot=6]}
     * returns the following information:
     *
     * <pre>
     * {@code
     * Consecutive Lengths: 2, 1, 1
     * Break Lengths: 2, 2
     * Consecutive Items: [[Shift slot=1] [Shift slot=2]], [[Shift slot=4]], [[Shift slot=6]]
     * }
     * </pre>
     *
     * @param indexMap Maps the fact to its position in the sequence
     * @param <A> type of the first mapped fact
     * @return never null
     */
    public static <A> UniConstraintCollector<A, ?, SequenceChain<A, Integer>>
            toConsecutiveSequences(ToIntFunction<A> indexMap) {
        return InnerUniConstraintCollectors.toConsecutiveSequences(indexMap);
    }

    /**
     * As defined by {@link #toConsecutiveSequences(ToIntFunction)}.
     *
     * @param resultMap Maps both facts to an item in the sequence
     * @param indexMap Maps the item to its position in the sequence
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <Result_> type of item in the sequence
     * @return never null
     */
    public static <A, B, Result_> BiConstraintCollector<A, B, ?, SequenceChain<Result_, Integer>>
            toConsecutiveSequences(BiFunction<A, B, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        return InnerBiConstraintCollectors.toConsecutiveSequences(resultMap, indexMap);
    }

    /**
     * As defined by {@link #toConsecutiveSequences(ToIntFunction)}.
     *
     * @param resultMap Maps the three facts to an item in the sequence
     * @param indexMap Maps the item to its position in the sequence
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <C> type of the third mapped fact
     * @param <Result_> type of item in the sequence
     * @return never null
     */
    public static <A, B, C, Result_> TriConstraintCollector<A, B, C, ?, SequenceChain<Result_, Integer>>
            toConsecutiveSequences(TriFunction<A, B, C, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        return InnerTriConstraintCollectors.toConsecutiveSequences(resultMap, indexMap);
    }

    /**
     * As defined by {@link #toConsecutiveSequences(ToIntFunction)}.
     *
     * @param resultMap Maps the four facts to an item in the sequence
     * @param indexMap Maps the item to its position in the sequence
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <C> type of the third mapped fact
     * @param <D> type of the fourth mapped fact
     * @param <Result_> type of item in the sequence
     * @return never null
     */
    public static <A, B, C, D, Result_> QuadConstraintCollector<A, B, C, D, ?, SequenceChain<Result_, Integer>>
            toConsecutiveSequences(QuadFunction<A, B, C, D, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        return InnerQuadConstraintCollectors.toConsecutiveSequences(resultMap, indexMap);
    }

    // *****************************************************************
    // toConnectedRanges
    // *****************************************************************
    /**
     * Creates a constraint collector that returns {@link ConnectedRangeChain} about the first fact.
     *
     * For instance, {@code [Equipment fromInclusive=2, toExclusive=4] [Equipment fromInclusive=3, toExclusive=5]
     *                      [Equipment fromInclusive=6, toExclusive=7] [Equipment fromInclusive=7, toExclusive=8]}
     * returns the following information:
     *
     * <pre>
     * {@code
     * ConnectedRanges: [minOverlap: 1, maxOverlap: 2,
     *                  [Equipment fromInclusive=2, toExclusive=4] [Equipment fromInclusive=3, toExclusive=5]],
     *                  [minConcurrentUsage: 1, maxConcurrentUsage: 1,
     *                  [Equipment fromInclusive=6, toExclusive=7] [Equipment fromInclusive=7, toExclusive=8]]
     * Breaks: [[Break from=5, to=6, length=1]]
     * }
     * </pre>
     *
     * This can be used to ensure a limited resource is not over-assigned.
     *
     * @param startInclusiveMap Maps the fact to its start
     * @param endExclusiveMap Maps the fact to its end
     * @param differenceFunction Computes the difference between two points. The second argument is always
     *        larger than the first (ex: {@link Duration#between}
     *        or {@code (a,b) -> b - a}).
     * @param <A> type of the first mapped fact
     * @param <PointType_> type of the fact endpoints
     * @param <DifferenceType_> type of difference between points
     * @return never null
     */
    public static <A, PointType_ extends Comparable<PointType_>, DifferenceType_ extends Comparable<DifferenceType_>>
            UniConstraintCollector<A, ?, ConnectedRangeChain<A, PointType_, DifferenceType_>>
            toConnectedRanges(Function<A, PointType_> startInclusiveMap, Function<A, PointType_> endExclusiveMap,
                    BiFunction<PointType_, PointType_, DifferenceType_> differenceFunction) {
        return InnerUniConstraintCollectors.toConnectedRanges(ConstantLambdaUtils.identity(), startInclusiveMap,
                endExclusiveMap,
                differenceFunction);
    }

    /**
     * Specialized version of {@link #toConnectedRanges(Function,Function,BiFunction)} for
     * {@link Temporal} types.
     *
     * @param <A> type of the first mapped fact
     * @param <PointType_> temporal type of the endpoints
     * @param startInclusiveMap Maps the fact to its start
     * @param endExclusiveMap Maps the fact to its end
     * @return never null
     */
    public static <A, PointType_ extends Temporal & Comparable<PointType_>>
            UniConstraintCollector<A, ?, ConnectedRangeChain<A, PointType_, Duration>>
            toConnectedTemporalRanges(Function<A, PointType_> startInclusiveMap, Function<A, PointType_> endExclusiveMap) {
        return toConnectedRanges(startInclusiveMap, endExclusiveMap, Duration::between);
    }

    /**
     * Specialized version of {@link #toConnectedRanges(Function,Function,BiFunction)} for Long.
     *
     * @param startInclusiveMap Maps the fact to its start
     * @param endExclusiveMap Maps the fact to its end
     * @param <A> type of the first mapped fact
     * @return never null
     */
    public static <A> UniConstraintCollector<A, ?, ConnectedRangeChain<A, Long, Long>>
            toConnectedRanges(ToLongFunction<A> startInclusiveMap, ToLongFunction<A> endExclusiveMap) {
        return toConnectedRanges(startInclusiveMap::applyAsLong, endExclusiveMap::applyAsLong, (a, b) -> b - a);
    }

    /**
     * As defined by {@link #toConnectedRanges(Function,Function,BiFunction)}.
     *
     * @param intervalMap Maps both facts to an item in the cluster
     * @param startInclusiveMap Maps the item to its start
     * @param endExclusiveMap Maps the item to its end
     * @param differenceFunction Computes the difference between two points. The second argument is always
     *        larger than the first (ex: {@link Duration#between}
     *        or {@code (a,b) -> b - a}).
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <IntervalType_> type of the item in the cluster
     * @param <PointType_> type of the item endpoints
     * @param <DifferenceType_> type of difference between points
     * @return never null
     */
    public static <A, B, IntervalType_, PointType_ extends Comparable<PointType_>, DifferenceType_ extends Comparable<DifferenceType_>>
            BiConstraintCollector<A, B, ?, ConnectedRangeChain<IntervalType_, PointType_, DifferenceType_>>
            toConnectedRanges(BiFunction<A, B, IntervalType_> intervalMap,
                    Function<IntervalType_, PointType_> startInclusiveMap,
                    Function<IntervalType_, PointType_> endExclusiveMap,
                    BiFunction<PointType_, PointType_, DifferenceType_> differenceFunction) {
        return InnerBiConstraintCollectors.toConnectedRanges(intervalMap, startInclusiveMap, endExclusiveMap,
                differenceFunction);
    }

    /**
     * As defined by {@link #toConnectedTemporalRanges(Function,Function)}.
     *
     * @param intervalMap Maps the three facts to an item in the cluster
     * @param startInclusiveMap Maps the fact to its start
     * @param endExclusiveMap Maps the fact to its end
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <IntervalType_> type of the item in the cluster
     * @param <PointType_> temporal type of the endpoints
     * @return never null
     */
    public static <A, B, IntervalType_, PointType_ extends Temporal & Comparable<PointType_>>
            BiConstraintCollector<A, B, ?, ConnectedRangeChain<IntervalType_, PointType_, Duration>>
            toConnectedTemporalRanges(BiFunction<A, B, IntervalType_> intervalMap,
                    Function<IntervalType_, PointType_> startInclusiveMap,
                    Function<IntervalType_, PointType_> endExclusiveMap) {
        return toConnectedRanges(intervalMap, startInclusiveMap, endExclusiveMap, Duration::between);
    }

    /**
     * As defined by {@link #toConnectedRanges(ToLongFunction, ToLongFunction)}.
     *
     * @param startInclusiveMap Maps the fact to its start
     * @param endExclusiveMap Maps the fact to its end
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <IntervalType_> type of the item in the cluster
     * @return never null
     */
    public static <A, B, IntervalType_>
            BiConstraintCollector<A, B, ?, ConnectedRangeChain<IntervalType_, Long, Long>>
            toConnectedRanges(BiFunction<A, B, IntervalType_> intervalMap, ToLongFunction<IntervalType_> startInclusiveMap,
                    ToLongFunction<IntervalType_> endExclusiveMap) {
        return toConnectedRanges(intervalMap, startInclusiveMap::applyAsLong, endExclusiveMap::applyAsLong, (a, b) -> b - a);
    }

    /**
     * As defined by {@link #toConnectedRanges(Function,Function,BiFunction)}.
     *
     * @param intervalMap Maps the three facts to an item in the cluster
     * @param startInclusiveMap Maps the item to its start
     * @param endExclusiveMap Maps the item to its end
     * @param differenceFunction Computes the difference between two points. The second argument is always
     *        larger than the first (ex: {@link Duration#between}
     *        or {@code (a,b) -> b - a}).
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <C> type of the third mapped fact
     * @param <IntervalType_> type of the item in the cluster
     * @param <PointType_> type of the item endpoints
     * @param <DifferenceType_> type of difference between points
     * @return never null
     */
    public static <A, B, C, IntervalType_, PointType_ extends Comparable<PointType_>, DifferenceType_ extends Comparable<DifferenceType_>>
            TriConstraintCollector<A, B, C, ?, ConnectedRangeChain<IntervalType_, PointType_, DifferenceType_>>
            toConnectedRanges(TriFunction<A, B, C, IntervalType_> intervalMap,
                    Function<IntervalType_, PointType_> startInclusiveMap,
                    Function<IntervalType_, PointType_> endExclusiveMap,
                    BiFunction<PointType_, PointType_, DifferenceType_> differenceFunction) {
        return InnerTriConstraintCollectors.toConnectedRanges(intervalMap, startInclusiveMap, endExclusiveMap,
                differenceFunction);
    }

    /**
     * As defined by {@link #toConnectedTemporalRanges(Function,Function)}.
     *
     * @param intervalMap Maps the three facts to an item in the cluster
     * @param startInclusiveMap Maps the fact to its start
     * @param endExclusiveMap Maps the fact to its end
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <C> type of the third mapped fact
     * @param <IntervalType_> type of the item in the cluster
     * @param <PointType_> temporal type of the endpoints
     * @return never null
     */
    public static <A, B, C, IntervalType_, PointType_ extends Temporal & Comparable<PointType_>>
            TriConstraintCollector<A, B, C, ?, ConnectedRangeChain<IntervalType_, PointType_, Duration>>
            toConnectedTemporalRanges(TriFunction<A, B, C, IntervalType_> intervalMap,
                    Function<IntervalType_, PointType_> startInclusiveMap,
                    Function<IntervalType_, PointType_> endExclusiveMap) {
        return toConnectedRanges(intervalMap, startInclusiveMap, endExclusiveMap, Duration::between);
    }

    /**
     * As defined by {@link #toConnectedRanges(ToLongFunction, ToLongFunction)}.
     *
     * @param startInclusiveMap Maps the fact to its start
     * @param endExclusiveMap Maps the fact to its end
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <C> type of the third mapped fact
     * @param <IntervalType_> type of the item in the cluster
     * @return never null
     */
    public static <A, B, C, IntervalType_>
            TriConstraintCollector<A, B, C, ?, ConnectedRangeChain<IntervalType_, Long, Long>>
            toConnectedRanges(TriFunction<A, B, C, IntervalType_> intervalMap, ToLongFunction<IntervalType_> startInclusiveMap,
                    ToLongFunction<IntervalType_> endExclusiveMap) {
        return toConnectedRanges(intervalMap, startInclusiveMap::applyAsLong, endExclusiveMap::applyAsLong, (a, b) -> b - a);
    }

    /**
     * As defined by {@link #toConnectedRanges(Function,Function,BiFunction)}.
     *
     * @param intervalMap Maps the four facts to an item in the cluster
     * @param startInclusiveMap Maps the item to its start
     * @param endExclusiveMap Maps the item to its end
     * @param differenceFunction Computes the difference between two points. The second argument is always
     *        larger than the first (ex: {@link Duration#between}
     *        or {@code (a,b) -> b - a}).
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <C> type of the third mapped fact
     * @param <D> type of the fourth mapped fact
     * @param <IntervalType_> type of the item in the cluster
     * @param <PointType_> type of the item endpoints
     * @param <DifferenceType_> type of difference between points
     * @return never null
     */
    public static <A, B, C, D, IntervalType_, PointType_ extends Comparable<PointType_>, DifferenceType_ extends Comparable<DifferenceType_>>
            QuadConstraintCollector<A, B, C, D, ?, ConnectedRangeChain<IntervalType_, PointType_, DifferenceType_>>
            toConnectedRanges(QuadFunction<A, B, C, D, IntervalType_> intervalMap,
                    Function<IntervalType_, PointType_> startInclusiveMap, Function<IntervalType_, PointType_> endExclusiveMap,
                    BiFunction<PointType_, PointType_, DifferenceType_> differenceFunction) {
        return InnerQuadConstraintCollectors.toConnectedRanges(intervalMap, startInclusiveMap, endExclusiveMap,
                differenceFunction);
    }

    /**
     * As defined by {@link #toConnectedTemporalRanges(Function,Function)}.
     *
     * @param intervalMap Maps the three facts to an item in the cluster
     * @param startInclusiveMap Maps the fact to its start
     * @param endExclusiveMap Maps the fact to its end
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <C> type of the third mapped fact
     * @param <D> type of the fourth mapped fact
     * @param <IntervalType_> type of the item in the cluster
     * @param <PointType_> temporal type of the endpoints
     * @return never null
     */
    public static <A, B, C, D, IntervalType_, PointType_ extends Temporal & Comparable<PointType_>>
            QuadConstraintCollector<A, B, C, D, ?, ConnectedRangeChain<IntervalType_, PointType_, Duration>>
            toConnectedTemporalRanges(QuadFunction<A, B, C, D, IntervalType_> intervalMap,
                    Function<IntervalType_, PointType_> startInclusiveMap,
                    Function<IntervalType_, PointType_> endExclusiveMap) {
        return toConnectedRanges(intervalMap, startInclusiveMap, endExclusiveMap, Duration::between);
    }

    /**
     * As defined by {@link #toConnectedRanges(ToLongFunction, ToLongFunction)}.
     *
     * @param startInclusiveMap Maps the fact to its start
     * @param endExclusiveMap Maps the fact to its end
     * @param <A> type of the first mapped fact
     * @param <B> type of the second mapped fact
     * @param <C> type of the third mapped fact
     * @param <D> type of the fourth mapped fact
     * @param <IntervalType_> type of the item in the cluster
     * @return never null
     */
    public static <A, B, C, D, IntervalType_>
            QuadConstraintCollector<A, B, C, D, ?, ConnectedRangeChain<IntervalType_, Long, Long>>
            toConnectedRanges(QuadFunction<A, B, C, D, IntervalType_> intervalMap,
                    ToLongFunction<IntervalType_> startInclusiveMap,
                    ToLongFunction<IntervalType_> endExclusiveMap) {
        return toConnectedRanges(intervalMap, startInclusiveMap::applyAsLong, endExclusiveMap::applyAsLong, (a, b) -> b - a);
    }

    // ************************************************************************
    // load balancing
    // ************************************************************************

    /**
     * As defined by {@link #loadBalance(Function, ToLongFunction, ToLongFunction)},
     * where the current load for each balanced item is set to one
     * and the starting load for each balanced item is set to zero.
     */
    public static <A, Balanced_> UniConstraintCollector<A, ?, LoadBalance<Balanced_>> loadBalance(
            Function<A, Balanced_> balancedItemFunction) {
        return loadBalance(balancedItemFunction, ConstantLambdaUtils.uniConstantOneLong());
    }

    /**
     * As defined by {@link #loadBalance(Function, ToLongFunction, ToLongFunction)},
     * where the starting load for each balanced item is set to zero.
     */
    public static <A, Balanced_> UniConstraintCollector<A, ?, LoadBalance<Balanced_>> loadBalance(
            Function<A, Balanced_> balancedItemFunction, ToLongFunction<A> loadFunction) {
        return loadBalance(balancedItemFunction, loadFunction, ConstantLambdaUtils.uniConstantZeroLong());
    }

    /**
     * Returns a collector that takes a stream of items and calculates the unfairness measure from them
     * (see {@link LoadBalance#unfairness()}).
     * The load for every item is provided by the loadFunction,
     * with the starting load provided by the initialLoadFunction.
     * <p>
     * When this collector is used in a constraint stream,
     * it is recommended that the score type be one of those based on {@link BigDecimal},
     * such as {@link HardSoftBigDecimalScore}.
     * This is so that the unfairness measure keeps its precision
     * without forcing the other constraints to be multiplied by a large constant,
     * which would otherwise be required to implement fixed-point arithmetic.
     *
     * @param balancedItemFunction The function that returns the item which should be load-balanced.
     * @param loadFunction How much the item should count for in the formula.
     * @param initialLoadFunction The initial value of the metric,
     *        allowing to provide initial state
     *        without requiring the entire previous planning windows in the working memory.
     * @param <A> type of the matched fact
     * @param <Balanced_> type of the item being balanced
     * @return never null
     */
    public static <A, Balanced_> UniConstraintCollector<A, ?, LoadBalance<Balanced_>> loadBalance(
            Function<A, Balanced_> balancedItemFunction, ToLongFunction<A> loadFunction,
            ToLongFunction<A> initialLoadFunction) {
        return InnerUniConstraintCollectors.loadBalance(balancedItemFunction, loadFunction, initialLoadFunction);
    }

    /**
     * As defined by {@link #loadBalance(BiFunction, ToLongBiFunction, ToLongBiFunction)},
     * where the current load for each balanced item is set to one
     * and the starting load for each balanced item is set to zero.
     */
    public static <A, B, Balanced_> BiConstraintCollector<A, B, ?, LoadBalance<Balanced_>> loadBalance(
            BiFunction<A, B, Balanced_> balancedItemFunction) {
        return loadBalance(balancedItemFunction, ConstantLambdaUtils.biConstantOneLong());
    }

    /**
     * As defined by {@link #loadBalance(BiFunction, ToLongBiFunction, ToLongBiFunction)},
     * where the starting load for each balanced item is set to zero.
     */
    public static <A, B, Balanced_> BiConstraintCollector<A, B, ?, LoadBalance<Balanced_>> loadBalance(
            BiFunction<A, B, Balanced_> balancedItemFunction, ToLongBiFunction<A, B> loadFunction) {
        return loadBalance(balancedItemFunction, loadFunction, ConstantLambdaUtils.biConstantZeroLong());
    }

    /**
     * As defined by {@link #loadBalance(Function, ToLongFunction, ToLongFunction)}.
     */
    public static <A, B, Balanced_> BiConstraintCollector<A, B, ?, LoadBalance<Balanced_>> loadBalance(
            BiFunction<A, B, Balanced_> balancedItemFunction, ToLongBiFunction<A, B> loadFunction,
            ToLongBiFunction<A, B> initialLoadFunction) {
        return InnerBiConstraintCollectors.loadBalance(balancedItemFunction, loadFunction, initialLoadFunction);
    }

    /**
     * As defined by {@link #loadBalance(TriFunction, ToLongTriFunction, ToLongTriFunction)},
     * where the current load for each balanced item is set to one
     * and the starting load for each balanced item is set to zero.
     */
    public static <A, B, C, Balanced_> TriConstraintCollector<A, B, C, ?, LoadBalance<Balanced_>> loadBalance(
            TriFunction<A, B, C, Balanced_> balancedItemFunction) {
        return loadBalance(balancedItemFunction, ConstantLambdaUtils.triConstantOneLong());
    }

    /**
     * As defined by {@link #loadBalance(TriFunction, ToLongTriFunction, ToLongTriFunction)},
     * where the starting load for each balanced item is set to zero.
     */
    public static <A, B, C, Balanced_> TriConstraintCollector<A, B, C, ?, LoadBalance<Balanced_>> loadBalance(
            TriFunction<A, B, C, Balanced_> balancedItemFunction, ToLongTriFunction<A, B, C> loadFunction) {
        return loadBalance(balancedItemFunction, loadFunction, ConstantLambdaUtils.triConstantZeroLong());
    }

    /**
     * As defined by {@link #loadBalance(Function, ToLongFunction, ToLongFunction)}.
     */
    public static <A, B, C, Balanced_> TriConstraintCollector<A, B, C, ?, LoadBalance<Balanced_>> loadBalance(
            TriFunction<A, B, C, Balanced_> balancedItemFunction, ToLongTriFunction<A, B, C> loadFunction,
            ToLongTriFunction<A, B, C> initialLoadFunction) {
        return InnerTriConstraintCollectors.loadBalance(balancedItemFunction, loadFunction, initialLoadFunction);
    }

    /**
     * As defined by {@link #loadBalance(QuadFunction, ToLongQuadFunction, ToLongQuadFunction)},
     * where the current load for each balanced item is set to one
     * and the starting load for each balanced item is set to zero.
     */
    public static <A, B, C, D, Balanced_> QuadConstraintCollector<A, B, C, D, ?, LoadBalance<Balanced_>> loadBalance(
            QuadFunction<A, B, C, D, Balanced_> balancedItemFunction) {
        return loadBalance(balancedItemFunction, ConstantLambdaUtils.quadConstantOneLong());
    }

    /**
     * As defined by {@link #loadBalance(QuadFunction, ToLongQuadFunction, ToLongQuadFunction)},
     * where the starting load for each balanced item is set to zero.
     */
    public static <A, B, C, D, Balanced_> QuadConstraintCollector<A, B, C, D, ?, LoadBalance<Balanced_>> loadBalance(
            QuadFunction<A, B, C, D, Balanced_> balancedItemFunction, ToLongQuadFunction<A, B, C, D> loadFunction) {
        return loadBalance(balancedItemFunction, loadFunction, ConstantLambdaUtils.quadConstantZeroLong());
    }

    /**
     * As defined by {@link #loadBalance(Function, ToLongFunction, ToLongFunction)}.
     */
    public static <A, B, C, D, Balanced_> QuadConstraintCollector<A, B, C, D, ?, LoadBalance<Balanced_>> loadBalance(
            QuadFunction<A, B, C, D, Balanced_> balancedItemFunction, ToLongQuadFunction<A, B, C, D> loadFunction,
            ToLongQuadFunction<A, B, C, D> initialLoadFunction) {
        return InnerQuadConstraintCollectors.loadBalance(balancedItemFunction, loadFunction, initialLoadFunction);
    }

    private ConstraintCollectors() {
    }
}
