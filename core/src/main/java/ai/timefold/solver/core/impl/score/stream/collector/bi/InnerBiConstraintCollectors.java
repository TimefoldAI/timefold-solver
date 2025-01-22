package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

public class InnerBiConstraintCollectors {
    public static <A, B> BiConstraintCollector<A, B, ?, Double> average(ToIntBiFunction<? super A, ? super B> mapper) {
        return new AverageIntBiCollector<>(mapper);
    }

    public static <A, B> BiConstraintCollector<A, B, ?, Double> average(ToLongBiFunction<? super A, ? super B> mapper) {
        return new AverageLongBiCollector<>(mapper);
    }

    static <A, B, Mapped_, Average_> BiConstraintCollector<A, B, ?, Average_> average(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        return new AverageReferenceBiCollector<>(mapper, calculatorSupplier);
    }

    public static <A, B> BiConstraintCollector<A, B, ?, BigDecimal> averageBigDecimal(
            BiFunction<? super A, ? super B, ? extends BigDecimal> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A, B> BiConstraintCollector<A, B, ?, Duration> averageDuration(
            BiFunction<? super A, ? super B, ? extends Duration> mapper) {
        return average(mapper, ReferenceAverageCalculator.duration());
    }

    public static <A, B> BiConstraintCollector<A, B, ?, BigDecimal> averageBigInteger(
            BiFunction<? super A, ? super B, ? extends BigInteger> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A, B, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            BiConstraintCollector<A, B, ?, Result_>
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
            BiConstraintCollector<A, B, ?, Result_>
            compose(
                    BiConstraintCollector<A, B, ResultHolder1_, Result1_> first,
                    BiConstraintCollector<A, B, ResultHolder2_, Result2_> second,
                    BiConstraintCollector<A, B, ResultHolder3_, Result3_> third,
                    TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction) {
        return new ComposeThreeBiCollector<>(
                first, second, third, composeFunction);
    }

    public static <A, B, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
            BiConstraintCollector<A, B, ?, Result_> compose(
                    BiConstraintCollector<A, B, ResultHolder1_, Result1_> first,
                    BiConstraintCollector<A, B, ResultHolder2_, Result2_> second,
                    BiFunction<Result1_, Result2_, Result_> composeFunction) {
        return new ComposeTwoBiCollector<>(first, second,
                composeFunction);
    }

    public static <A, B, ResultContainer_, Result_> BiConstraintCollector<A, B, ResultContainer_, Result_> conditionally(
            BiPredicate<A, B> predicate,
            BiConstraintCollector<A, B, ResultContainer_, Result_> delegate) {
        return new ConditionalBiCollector<>(predicate, delegate);
    }

    public static <A, B> BiConstraintCollector<A, B, ?, Integer> count() {
        return CountIntBiCollector.getInstance();
    }

    public static <A, B, Mapped_> BiConstraintCollector<A, B, ?, Integer> countDistinct(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        return new CountDistinctIntBiCollector<>(mapper);
    }

    public static <A, B, Mapped_> BiConstraintCollector<A, B, ?, Long> countDistinctLong(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        return new CountDistinctLongBiCollector<>(mapper);
    }

    public static <A, B> BiConstraintCollector<A, B, ?, Long> countLong() {
        return CountLongBiCollector.getInstance();
    }

    public static <A, B, Result_ extends Comparable<? super Result_>> BiConstraintCollector<A, B, ?, Result_> max(
            BiFunction<? super A, ? super B, ? extends Result_> mapper) {
        return new MaxComparableBiCollector<>(mapper);
    }

    public static <A, B, Result_> BiConstraintCollector<A, B, ?, Result_> max(
            BiFunction<? super A, ? super B, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MaxComparatorBiCollector<>(mapper, comparator);
    }

    public static <A, B, Result_, Property_ extends Comparable<? super Property_>>
            BiConstraintCollector<A, B, ?, Result_> max(
                    BiFunction<? super A, ? super B, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MaxPropertyBiCollector<>(mapper, propertyMapper);
    }

    public static <A, B, Result_ extends Comparable<? super Result_>> BiConstraintCollector<A, B, ?, Result_> min(
            BiFunction<? super A, ? super B, ? extends Result_> mapper) {
        return new MinComparableBiCollector<>(mapper);
    }

    public static <A, B, Result_> BiConstraintCollector<A, B, ?, Result_> min(
            BiFunction<? super A, ? super B, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MinComparatorBiCollector<>(mapper, comparator);
    }

    public static <A, B, Result_, Property_ extends Comparable<? super Property_>>
            BiConstraintCollector<A, B, ?, Result_> min(
                    BiFunction<? super A, ? super B, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MinPropertyBiCollector<>(mapper, propertyMapper);
    }

    public static <A, B> BiConstraintCollector<A, B, ?, Integer> sum(ToIntBiFunction<? super A, ? super B> mapper) {
        return new SumIntBiCollector<>(mapper);
    }

    public static <A, B> BiConstraintCollector<A, B, ?, Long> sum(ToLongBiFunction<? super A, ? super B> mapper) {
        return new SumLongBiCollector<>(mapper);
    }

    public static <A, B, Result_> BiConstraintCollector<A, B, ?, Result_> sum(
            BiFunction<? super A, ? super B, ? extends Result_> mapper, Result_ zero,
            BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        return new SumReferenceBiCollector<>(mapper, zero, adder, subtractor);
    }

    public static <A, B, Mapped_, Result_ extends Collection<Mapped_>> BiConstraintCollector<A, B, ?, Result_>
            toCollection(
                    BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
                    IntFunction<Result_> collectionFunction) {
        return new ToCollectionBiCollector<>(mapper, collectionFunction);
    }

    public static <A, B, Mapped_> BiConstraintCollector<A, B, ?, List<Mapped_>> toList(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        return new ToListBiCollector<>(mapper);
    }

    public static <A, B, Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
            BiConstraintCollector<A, B, ?, Result_> toMap(
                    BiFunction<? super A, ? super B, ? extends Key_> keyFunction,
                    BiFunction<? super A, ? super B, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    IntFunction<Set_> setFunction) {
        return new ToMultiMapBiCollector<>(keyFunction, valueFunction, mapSupplier, setFunction);
    }

    public static <A, B, Key_, Value_, Result_ extends Map<Key_, Value_>> BiConstraintCollector<A, B, ?, Result_>
            toMap(
                    BiFunction<? super A, ? super B, ? extends Key_> keyFunction,
                    BiFunction<? super A, ? super B, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    BinaryOperator<Value_> mergeFunction) {
        return new ToSimpleMapBiCollector<>(keyFunction, valueFunction, mapSupplier, mergeFunction);
    }

    public static <A, B, Mapped_> BiConstraintCollector<A, B, ?, Set<Mapped_>> toSet(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper) {
        return new ToSetBiCollector<>(mapper);
    }

    public static <A, B, Mapped_> BiConstraintCollector<A, B, ?, SortedSet<Mapped_>> toSortedSet(
            BiFunction<? super A, ? super B, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        return new ToSortedSetComparatorBiCollector<>(mapper, comparator);
    }

    public static <A, B, Result_> BiConstraintCollector<A, B, ?, SequenceChain<Result_, Integer>>
            toConsecutiveSequences(BiFunction<A, B, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        return new ConsecutiveSequencesBiConstraintCollector<>(resultMap, indexMap);
    }

    public static <A, B, Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
            BiConstraintCollector<A, B, ?, ConnectedRangeChain<Interval_, Point_, Difference_>>
            toConnectedRanges(BiFunction<? super A, ? super B, ? extends Interval_> mapper,
                    Function<? super Interval_, ? extends Point_> startMap,
                    Function<? super Interval_, ? extends Point_> endMap,
                    BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        return new ConnectedRangesBiConstraintCollector<>(mapper, startMap, endMap,
                differenceFunction);
    }

    public static <A, B, Intermediate_, Result_> BiConstraintCollector<A, B, ?, Result_>
            collectAndThen(BiConstraintCollector<A, B, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return new AndThenBiCollector<>(delegate, mappingFunction);
    }

    public static <A, B, Balanced_> BiConstraintCollector<A, B, ?, LoadBalance<Balanced_>> loadBalance(
            BiFunction<A, B, Balanced_> balancedItemFunction, ToLongBiFunction<A, B> loadFunction,
            ToLongBiFunction<A, B> initialLoadFunction) {
        return new LoadBalanceBiCollector<>(balancedItemFunction, loadFunction, initialLoadFunction);
    }

}
