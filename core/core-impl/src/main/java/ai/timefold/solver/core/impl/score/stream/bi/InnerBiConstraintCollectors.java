package ai.timefold.solver.core.impl.score.stream.bi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ReferenceAverageCalculator;

public class InnerBiConstraintCollectors {
    public static <A, B> AverageIntBiCollector<A, B> average(ToIntBiFunction<? super A, ? super B> mapper) {
        return new AverageIntBiCollector<>(mapper);
    }

    public static <A, B> AverageLongBiCollector<A, B> average(ToLongBiFunction<? super A, ? super B> mapper) {
        return new AverageLongBiCollector<>(mapper);
    }

    static <A, B, Mapped_, Average_> AverageReferenceBiCollector<A, B, Mapped_, Average_> average(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        return new AverageReferenceBiCollector<>(mapper, calculatorSupplier);
    }

    public static <A, B> AverageReferenceBiCollector<A, B, BigDecimal, BigDecimal> averageBigDecimal(
            BiFunction<? super A, ? super B, ? extends BigDecimal> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A, B> AverageReferenceBiCollector<A, B, Duration, Duration> averageDuration(
            BiFunction<? super A, ? super B, ? extends Duration> mapper) {
        return average(mapper, ReferenceAverageCalculator.duration());
    }

    public static <A, B> AverageReferenceBiCollector<A, B, BigInteger, BigDecimal> averageBigInteger(
            BiFunction<? super A, ? super B, ? extends BigInteger> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A, B, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            ComposeFourBiCollector<A, B, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            compose(
                    BiConstraintCollector<A, B, ResultHolder1_, Result1_> first,
                    BiConstraintCollector<A, B, ResultHolder2_, Result2_> second,
                    BiConstraintCollector<A, B, ResultHolder3_, Result3_> third,
                    BiConstraintCollector<A, B, ResultHolder4_, Result4_> fourth,
                    QuadFunction<Result1_, Result2_, Result3_, Result4_, Result_> composeFunction) {
        return new ComposeFourBiCollector<>(
                first, second, third, fourth, composeFunction);
    }

    public static <A, B, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
            ComposeThreeBiCollector<A, B, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
            compose(
                    BiConstraintCollector<A, B, ResultHolder1_, Result1_> first,
                    BiConstraintCollector<A, B, ResultHolder2_, Result2_> second,
                    BiConstraintCollector<A, B, ResultHolder3_, Result3_> third,
                    TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction) {
        return new ComposeThreeBiCollector<>(
                first, second, third, composeFunction);
    }

    public static <A, B, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
            ComposeTwoBiCollector<A, B, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_> compose(
                    BiConstraintCollector<A, B, ResultHolder1_, Result1_> first,
                    BiConstraintCollector<A, B, ResultHolder2_, Result2_> second,
                    BiFunction<Result1_, Result2_, Result_> composeFunction) {
        return new ComposeTwoBiCollector<>(first, second,
                composeFunction);
    }

    public static <A, B, ResultContainer_, Result_> ConditionalBiCollector<A, B, ResultContainer_, Result_> conditionally(
            BiPredicate<A, B> predicate,
            BiConstraintCollector<A, B, ResultContainer_, Result_> delegate) {
        return new ConditionalBiCollector<>(predicate, delegate);
    }

    public static <A, B> CountIntBiCollector<A, B> count() {
        return new CountIntBiCollector<>();
    }

    public static <A, B, Mapped_> CountDistinctIntBiCollector<A, B, Mapped_> countDistinct(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        return new CountDistinctIntBiCollector<>(mapper);
    }

    public static <A, B, Mapped_> CountDistinctLongBiCollector<A, B, Mapped_> countDistinctLong(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        return new CountDistinctLongBiCollector<>(mapper);
    }

    public static <A, B> CountLongBiCollector<A, B> countLong() {
        return new CountLongBiCollector<>();
    }

    public static <A, B, Result_ extends Comparable<? super Result_>> MaxComparableBiCollector<A, B, Result_> max(
            BiFunction<? super A, ? super B, ? extends Result_> mapper) {
        return new MaxComparableBiCollector<>(mapper);
    }

    public static <A, B, Result_> MaxComparatorBiCollector<A, B, Result_> max(
            BiFunction<? super A, ? super B, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MaxComparatorBiCollector<>(mapper, comparator);
    }

    public static <A, B, Result_, Property_ extends Comparable<? super Property_>>
            MaxPropertyBiCollector<A, B, Result_, Property_> max(
                    BiFunction<? super A, ? super B, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MaxPropertyBiCollector<>(mapper, propertyMapper);
    }

    public static <A, B, Result_ extends Comparable<? super Result_>> MinComparableBiCollector<A, B, Result_> min(
            BiFunction<? super A, ? super B, ? extends Result_> mapper) {
        return new MinComparableBiCollector<>(mapper);
    }

    public static <A, B, Result_> MinComparatorBiCollector<A, B, Result_> min(
            BiFunction<? super A, ? super B, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MinComparatorBiCollector<>(mapper, comparator);
    }

    public static <A, B, Result_, Property_ extends Comparable<? super Property_>>
            MinPropertyBiCollector<A, B, Result_, Property_> min(
                    BiFunction<? super A, ? super B, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MinPropertyBiCollector<>(mapper, propertyMapper);
    }

    public static <A, B> SumIntBiCollector<A, B> sum(ToIntBiFunction<? super A, ? super B> mapper) {
        return new SumIntBiCollector<>(mapper);
    }

    public static <A, B> SumLongBiCollector<A, B> sum(ToLongBiFunction<? super A, ? super B> mapper) {
        return new SumLongBiCollector<>(mapper);
    }

    public static <A, B, Result_> SumReferenceBiCollector<A, B, Result_> sum(
            BiFunction<? super A, ? super B, ? extends Result_> mapper, Result_ zero,
            BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        return new SumReferenceBiCollector<>(mapper, zero, adder, subtractor);
    }

    public static <A, B, Mapped_, Result_ extends Collection<Mapped_>> ToCollectionBiCollector<A, B, Mapped_, Result_>
            toCollection(
                    BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
                    IntFunction<Result_> collectionFunction) {
        return new ToCollectionBiCollector<>(mapper, collectionFunction);
    }

    public static <A, B, Mapped_> ToListBiCollector<A, B, Mapped_> toList(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        return new ToListBiCollector<>(mapper);
    }

    public static <A, B, Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
            ToMultiMapBiCollector<A, B, Key_, Value_, Set_, Result_> toMap(
                    BiFunction<? super A, ? super B, ? extends Key_> keyFunction,
                    BiFunction<? super A, ? super B, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    IntFunction<Set_> setFunction) {
        return new ToMultiMapBiCollector<>(keyFunction, valueFunction, mapSupplier, setFunction);
    }

    public static <A, B, Key_, Value_, Result_ extends Map<Key_, Value_>> ToSimpleMapBiCollector<A, B, Key_, Value_, Result_>
            toMap(
                    BiFunction<? super A, ? super B, ? extends Key_> keyFunction,
                    BiFunction<? super A, ? super B, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    BinaryOperator<Value_> mergeFunction) {
        return new ToSimpleMapBiCollector<>(keyFunction, valueFunction, mapSupplier, mergeFunction);
    }

    public static <A, B, Mapped_> ToSetBiCollector<A, B, Mapped_> toSet(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        return new ToSetBiCollector<>(mapper);
    }

    public static <A, B, Mapped_> ToSortedSetComparatorBiCollector<A, B, Mapped_> toSortedSet(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        return new ToSortedSetComparatorBiCollector<>(mapper, comparator);
    }
}
