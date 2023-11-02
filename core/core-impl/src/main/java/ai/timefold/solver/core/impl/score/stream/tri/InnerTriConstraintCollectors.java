package ai.timefold.solver.core.impl.score.stream.tri;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ReferenceAverageCalculator;

public class InnerTriConstraintCollectors {
    public static <A, B, C> AverageIntTriCollector<A, B, C> average(ToIntTriFunction<? super A, ? super B, ? super C> mapper) {
        return new AverageIntTriCollector<>(mapper);
    }

    public static <A, B, C> AverageLongTriCollector<A, B, C> average(
            ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        return new AverageLongTriCollector<>(mapper);
    }

    static <A, B, C, Mapped_, Average_> AverageReferenceTriCollector<A, B, C, Mapped_, Average_> average(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        return new AverageReferenceTriCollector<>(mapper, calculatorSupplier);
    }

    public static <A, B, C> AverageReferenceTriCollector<A, B, C, BigDecimal, BigDecimal> averageBigDecimal(
            TriFunction<? super A, ? super B, ? super C, ? extends BigDecimal> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A, B, C> AverageReferenceTriCollector<A, B, C, BigInteger, BigDecimal> averageBigInteger(
            TriFunction<? super A, ? super B, ? super C, ? extends BigInteger> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A, B, C> AverageReferenceTriCollector<A, B, C, Duration, Duration> averageDuration(
            TriFunction<? super A, ? super B, ? super C, ? extends Duration> mapper) {
        return average(mapper, ReferenceAverageCalculator.duration());
    }

    public static <A, B, C, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            ComposeFourTriCollector<A, B, C, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            compose(
                    TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first,
                    TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second,
                    TriConstraintCollector<A, B, C, ResultHolder3_, Result3_> third,
                    TriConstraintCollector<A, B, C, ResultHolder4_, Result4_> fourth,
                    QuadFunction<Result1_, Result2_, Result3_, Result4_, Result_> composeFunction) {
        return new ComposeFourTriCollector<>(
                first, second, third, fourth, composeFunction);
    }

    public static <A, B, C, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
            ComposeThreeTriCollector<A, B, C, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
            compose(
                    TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first,
                    TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second,
                    TriConstraintCollector<A, B, C, ResultHolder3_, Result3_> third,
                    TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction) {
        return new ComposeThreeTriCollector<>(
                first, second, third, composeFunction);
    }

    public static <A, B, C, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
            ComposeTwoTriCollector<A, B, C, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_> compose(
                    TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first,
                    TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second,
                    BiFunction<Result1_, Result2_, Result_> composeFunction) {
        return new ComposeTwoTriCollector<>(first, second,
                composeFunction);
    }

    public static <A, B, C, ResultContainer_, Result_> ConditionalTriCollector<A, B, C, ResultContainer_, Result_>
            conditionally(
                    TriPredicate<A, B, C> predicate,
                    TriConstraintCollector<A, B, C, ResultContainer_, Result_> delegate) {
        return new ConditionalTriCollector<>(predicate, delegate);
    }

    public static <A, B, C> CountIntTriCollector<A, B, C> count() {
        return new CountIntTriCollector<>();
    }

    public static <A, B, C, Mapped_> CountDistinctIntTriCollector<A, B, C, Mapped_> countDistinct(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        return new CountDistinctIntTriCollector<>(mapper);
    }

    public static <A, B, C, Mapped_> CountDistinctLongTriCollector<A, B, C, Mapped_> countDistinctLong(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        return new CountDistinctLongTriCollector<>(mapper);
    }

    public static <A, B, C> CountLongTriCollector<A, B, C> countLong() {
        return new CountLongTriCollector<>();
    }

    public static <A, B, C, Result_ extends Comparable<? super Result_>> MaxComparableTriCollector<A, B, C, Result_> max(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper) {
        return new MaxComparableTriCollector<>(mapper);
    }

    public static <A, B, C, Result_> MaxComparatorTriCollector<A, B, C, Result_> max(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MaxComparatorTriCollector<>(mapper, comparator);
    }

    public static <A, B, C, Result_, Property_ extends Comparable<? super Property_>>
            MaxPropertyTriCollector<A, B, C, Result_, Property_> max(
                    TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MaxPropertyTriCollector<>(mapper, propertyMapper);
    }

    public static <A, B, C, Result_ extends Comparable<? super Result_>> MinComparableTriCollector<A, B, C, Result_> min(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper) {
        return new MinComparableTriCollector<>(mapper);
    }

    public static <A, B, C, Result_> MinComparatorTriCollector<A, B, C, Result_> min(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MinComparatorTriCollector<>(mapper, comparator);
    }

    public static <A, B, C, Result_, Property_ extends Comparable<? super Property_>>
            MinPropertyTriCollector<A, B, C, Result_, Property_> min(
                    TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MinPropertyTriCollector<>(mapper, propertyMapper);
    }

    public static <A, B, C> SumIntTriCollector<A, B, C> sum(ToIntTriFunction<? super A, ? super B, ? super C> mapper) {
        return new SumIntTriCollector<>(mapper);
    }

    public static <A, B, C, Result_> SumReferenceTriCollector<A, B, C, Result_> sum(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper, Result_ zero,
            BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        return new SumReferenceTriCollector<>(mapper, zero, adder, subtractor);
    }

    public static <A, B, C, Mapped_, Result_ extends Collection<Mapped_>>
            ToCollectionTriCollector<A, B, C, Mapped_, Result_> toCollection(
                    TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
                    IntFunction<Result_> collectionFunction) {
        return new ToCollectionTriCollector<>(mapper, collectionFunction);
    }

    public static <A, B, C, Mapped_> ToListTriCollector<A, B, C, Mapped_> toList(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        return new ToListTriCollector<>(mapper);
    }

    public static <A, B, C, Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
            ToMultiMapTriCollector<A, B, C, Key_, Value_, Set_, Result_> toMap(
                    TriFunction<? super A, ? super B, ? super C, ? extends Key_> keyFunction,
                    TriFunction<? super A, ? super B, ? super C, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    IntFunction<Set_> setFunction) {
        return new ToMultiMapTriCollector<>(keyFunction, valueFunction, mapSupplier,
                setFunction);
    }

    public static <A, B, C, Key_, Value_, Result_ extends Map<Key_, Value_>>
            ToSimpleMapTriCollector<A, B, C, Key_, Value_, Result_> toMap(
                    TriFunction<? super A, ? super B, ? super C, ? extends Key_> keyFunction,
                    TriFunction<? super A, ? super B, ? super C, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    BinaryOperator<Value_> mergeFunction) {
        return new ToSimpleMapTriCollector<>(keyFunction, valueFunction, mapSupplier,
                mergeFunction);
    }

    public static <A, B, C, Mapped_> ToSetTriCollector<A, B, C, Mapped_> toSet(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        return new ToSetTriCollector<>(mapper);
    }

    public static <A, B, C, Mapped_> ToSortedSetComparatorTriCollector<A, B, C, Mapped_> toSortedSet(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        return new ToSortedSetComparatorTriCollector<>(mapper, comparator);
    }
}
