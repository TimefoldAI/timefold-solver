package ai.timefold.solver.core.impl.score.stream.uni;

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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ReferenceAverageCalculator;

public class InnerUniConstraintCollectors {
    public static <A> AverageIntUniCollector<A> average(ToIntFunction<? super A> mapper) {
        return new AverageIntUniCollector<>(mapper);
    }

    public static <A> AverageLongUniCollector<A> average(ToLongFunction<? super A> mapper) {
        return new AverageLongUniCollector<>(mapper);
    }

    public static <A> AverageReferenceUniCollector<A, BigDecimal, BigDecimal> averageBigDecimal(
            Function<? super A, ? extends BigDecimal> mapper) {
        return new AverageReferenceUniCollector<>(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A> AverageReferenceUniCollector<A, BigInteger, BigDecimal> averageBigInteger(
            Function<? super A, ? extends BigInteger> mapper) {
        return new AverageReferenceUniCollector<>(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A> AverageReferenceUniCollector<A, Duration, Duration> averageDuration(
            Function<? super A, ? extends Duration> mapper) {
        return new AverageReferenceUniCollector<>(mapper, ReferenceAverageCalculator.duration());
    }

    public static <A, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            ComposeFourUniCollector<A, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            compose(
                    UniConstraintCollector<A, ResultHolder1_, Result1_> first,
                    UniConstraintCollector<A, ResultHolder2_, Result2_> second,
                    UniConstraintCollector<A, ResultHolder3_, Result3_> third,
                    UniConstraintCollector<A, ResultHolder4_, Result4_> fourth,
                    QuadFunction<Result1_, Result2_, Result3_, Result4_, Result_> composeFunction) {
        return new ComposeFourUniCollector<>(
                first, second, third, fourth, composeFunction);
    }

    public static <A, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
            ComposeThreeUniCollector<A, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
            compose(
                    UniConstraintCollector<A, ResultHolder1_, Result1_> first,
                    UniConstraintCollector<A, ResultHolder2_, Result2_> second,
                    UniConstraintCollector<A, ResultHolder3_, Result3_> third,
                    TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction) {
        return new ComposeThreeUniCollector<>(
                first, second, third, composeFunction);
    }

    public static <A, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
            ComposeTwoUniCollector<A, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_> compose(
                    UniConstraintCollector<A, ResultHolder1_, Result1_> first,
                    UniConstraintCollector<A, ResultHolder2_, Result2_> second,
                    BiFunction<Result1_, Result2_, Result_> composeFunction) {
        return new ComposeTwoUniCollector<>(first, second,
                composeFunction);
    }

    public static <A, ResultContainer_, Result_> ConditionalUniCollector<A, ResultContainer_, Result_> conditionally(
            Predicate<A> predicate, UniConstraintCollector<A, ResultContainer_, Result_> delegate) {
        return new ConditionalUniCollector<>(predicate, delegate);
    }

    public static <A> CountIntUniCollector<A> count() {
        return new CountIntUniCollector<>();
    }

    public static <A, Mapped_> CountDistinctIntUniCollector<A, Mapped_> countDistinct(
            Function<? super A, ? extends Mapped_> mapper) {
        return new CountDistinctIntUniCollector<>(mapper);
    }

    public static <A, Mapped_> CountDistinctLongUniCollector<A, Mapped_> countDistinctLong(
            Function<? super A, ? extends Mapped_> mapper) {
        return new CountDistinctLongUniCollector<>(mapper);
    }

    public static <A> CountLongUniCollector<A> countLong() {
        return new CountLongUniCollector<>();
    }

    public static <A, Result_ extends Comparable<? super Result_>> MaxComparableUniCollector<A, Result_> max(
            Function<? super A, ? extends Result_> mapper) {
        return new MaxComparableUniCollector<>(mapper);
    }

    public static <A, Result_> MaxComparatorUniCollector<A, Result_> max(Function<? super A, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MaxComparatorUniCollector<>(mapper, comparator);
    }

    public static <A, Result_, Property_ extends Comparable<? super Property_>> MaxPropertyUniCollector<A, Result_, Property_>
            max(
                    Function<? super A, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MaxPropertyUniCollector<>(mapper, propertyMapper);
    }

    public static <A, Result_ extends Comparable<? super Result_>> MinComparableUniCollector<A, Result_> min(
            Function<? super A, ? extends Result_> mapper) {
        return new MinComparableUniCollector<>(mapper);
    }

    public static <A, Result_> MinComparatorUniCollector<A, Result_> min(Function<? super A, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MinComparatorUniCollector<>(mapper, comparator);
    }

    public static <A, Result_, Property_ extends Comparable<? super Property_>> MinPropertyUniCollector<A, Result_, Property_>
            min(
                    Function<? super A, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MinPropertyUniCollector<>(mapper, propertyMapper);
    }

    public static <A> SumIntUniCollector<A> sum(ToIntFunction<? super A> mapper) {
        return new SumIntUniCollector<>(mapper);
    }

    public static <A> SumLongUniCollector<A> sum(ToLongFunction<? super A> mapper) {
        return new SumLongUniCollector<>(mapper);
    }

    public static <A, Result_> SumReferenceUniCollector<A, Result_> sum(Function<? super A, ? extends Result_> mapper,
            Result_ zero, BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        return new SumReferenceUniCollector<>(mapper, zero, adder, subtractor);
    }

    public static <A, Mapped_, Result_ extends Collection<Mapped_>> ToCollectionUniCollector<A, Mapped_, Result_>
            toCollection(
                    Function<? super A, ? extends Mapped_> mapper,
                    IntFunction<Result_> collectionFunction) {
        return new ToCollectionUniCollector<>(mapper, collectionFunction);
    }

    public static <A, Mapped_> ToListUniCollector<A, Mapped_> toList(Function<? super A, ? extends Mapped_> mapper) {
        return new ToListUniCollector<>(mapper);
    }

    public static <A, Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
            ToMultiMapUniCollector<A, Key_, Value_, Set_, Result_> toMap(
                    Function<? super A, ? extends Key_> keyFunction,
                    Function<? super A, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    IntFunction<Set_> setFunction) {
        return new ToMultiMapUniCollector<>(keyFunction, valueFunction, mapSupplier, setFunction);
    }

    public static <A, Key_, Value_, Result_ extends Map<Key_, Value_>> ToSimpleMapUniCollector<A, Key_, Value_, Result_> toMap(
            Function<? super A, ? extends Key_> keyFunction,
            Function<? super A, ? extends Value_> valueFunction,
            Supplier<Result_> mapSupplier,
            BinaryOperator<Value_> mergeFunction) {
        return new ToSimpleMapUniCollector<>(keyFunction, valueFunction, mapSupplier, mergeFunction);
    }

    public static <A, Mapped_> ToSetUniCollector<A, Mapped_> toSet(Function<? super A, ? extends Mapped_> mapper) {
        return new ToSetUniCollector<>(mapper);
    }

    public static <A, Mapped_> ToSortedSetComparatorUniCollector<A, Mapped_> toSortedSet(
            Function<? super A, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        return new ToSortedSetComparatorUniCollector<>(mapper, comparator);
    }
}
