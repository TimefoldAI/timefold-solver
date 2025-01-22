package ai.timefold.solver.core.impl.score.stream.collector.tri;

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
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

public class InnerTriConstraintCollectors {
    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Double> average(
            ToIntTriFunction<? super A, ? super B, ? super C> mapper) {
        return new AverageIntTriCollector<>(mapper);
    }

    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Double> average(
            ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        return new AverageLongTriCollector<>(mapper);
    }

    static <A, B, C, Mapped_, Average_> TriConstraintCollector<A, B, C, ?, Average_> average(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        return new AverageReferenceTriCollector<>(mapper, calculatorSupplier);
    }

    public static <A, B, C> TriConstraintCollector<A, B, C, ?, BigDecimal> averageBigDecimal(
            TriFunction<? super A, ? super B, ? super C, ? extends BigDecimal> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A, B, C> TriConstraintCollector<A, B, C, ?, BigDecimal> averageBigInteger(
            TriFunction<? super A, ? super B, ? super C, ? extends BigInteger> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Duration> averageDuration(
            TriFunction<? super A, ? super B, ? super C, ? extends Duration> mapper) {
        return average(mapper, ReferenceAverageCalculator.duration());
    }

    public static <A, B, C, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            TriConstraintCollector<A, B, C, ?, Result_>
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
            TriConstraintCollector<A, B, C, ?, Result_>
            compose(
                    TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first,
                    TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second,
                    TriConstraintCollector<A, B, C, ResultHolder3_, Result3_> third,
                    TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction) {
        return new ComposeThreeTriCollector<>(
                first, second, third, composeFunction);
    }

    public static <A, B, C, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
            TriConstraintCollector<A, B, C, ?, Result_> compose(
                    TriConstraintCollector<A, B, C, ResultHolder1_, Result1_> first,
                    TriConstraintCollector<A, B, C, ResultHolder2_, Result2_> second,
                    BiFunction<Result1_, Result2_, Result_> composeFunction) {
        return new ComposeTwoTriCollector<>(first, second,
                composeFunction);
    }

    public static <A, B, C, ResultContainer_, Result_> TriConstraintCollector<A, B, C, ResultContainer_, Result_>
            conditionally(
                    TriPredicate<A, B, C> predicate,
                    TriConstraintCollector<A, B, C, ResultContainer_, Result_> delegate) {
        return new ConditionalTriCollector<>(predicate, delegate);
    }

    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Integer> count() {
        return CountIntTriCollector.getInstance();
    }

    public static <A, B, C, Mapped_> TriConstraintCollector<A, B, C, ?, Integer> countDistinct(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        return new CountDistinctIntTriCollector<>(mapper);
    }

    public static <A, B, C, Mapped_> TriConstraintCollector<A, B, C, ?, Long> countDistinctLong(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        return new CountDistinctLongTriCollector<>(mapper);
    }

    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Long> countLong() {
        return CountLongTriCollector.getInstance();
    }

    public static <A, B, C, Result_ extends Comparable<? super Result_>> TriConstraintCollector<A, B, C, ?, Result_> max(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper) {
        return new MaxComparableTriCollector<>(mapper);
    }

    public static <A, B, C, Result_> TriConstraintCollector<A, B, C, ?, Result_> max(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MaxComparatorTriCollector<>(mapper, comparator);
    }

    public static <A, B, C, Result_, Property_ extends Comparable<? super Property_>>
            TriConstraintCollector<A, B, C, ?, Result_> max(
                    TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MaxPropertyTriCollector<>(mapper, propertyMapper);
    }

    public static <A, B, C, Result_ extends Comparable<? super Result_>> TriConstraintCollector<A, B, C, ?, Result_> min(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper) {
        return new MinComparableTriCollector<>(mapper);
    }

    public static <A, B, C, Result_> TriConstraintCollector<A, B, C, ?, Result_> min(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MinComparatorTriCollector<>(mapper, comparator);
    }

    public static <A, B, C, Result_, Property_ extends Comparable<? super Property_>>
            TriConstraintCollector<A, B, C, ?, Result_> min(
                    TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MinPropertyTriCollector<>(mapper, propertyMapper);
    }

    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Integer> sum(
            ToIntTriFunction<? super A, ? super B, ? super C> mapper) {
        return new SumIntTriCollector<>(mapper);
    }

    public static <A, B, C> TriConstraintCollector<A, B, C, ?, Long> sum(
            ToLongTriFunction<? super A, ? super B, ? super C> mapper) {
        return new SumLongTriCollector<>(mapper);
    }

    public static <A, B, C, Result_> TriConstraintCollector<A, B, C, ?, Result_> sum(
            TriFunction<? super A, ? super B, ? super C, ? extends Result_> mapper, Result_ zero,
            BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        return new SumReferenceTriCollector<>(mapper, zero, adder, subtractor);
    }

    public static <A, B, C, Mapped_, Result_ extends Collection<Mapped_>>
            TriConstraintCollector<A, B, C, ?, Result_> toCollection(
                    TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
                    IntFunction<Result_> collectionFunction) {
        return new ToCollectionTriCollector<>(mapper, collectionFunction);
    }

    public static <A, B, C, Mapped_> TriConstraintCollector<A, B, C, ?, List<Mapped_>> toList(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        return new ToListTriCollector<>(mapper);
    }

    public static <A, B, C, Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
            TriConstraintCollector<A, B, C, ?, Result_> toMap(
                    TriFunction<? super A, ? super B, ? super C, ? extends Key_> keyFunction,
                    TriFunction<? super A, ? super B, ? super C, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    IntFunction<Set_> setFunction) {
        return new ToMultiMapTriCollector<>(keyFunction, valueFunction, mapSupplier,
                setFunction);
    }

    public static <A, B, C, Key_, Value_, Result_ extends Map<Key_, Value_>>
            TriConstraintCollector<A, B, C, ?, Result_> toMap(
                    TriFunction<? super A, ? super B, ? super C, ? extends Key_> keyFunction,
                    TriFunction<? super A, ? super B, ? super C, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    BinaryOperator<Value_> mergeFunction) {
        return new ToSimpleMapTriCollector<>(keyFunction, valueFunction, mapSupplier,
                mergeFunction);
    }

    public static <A, B, C, Mapped_> TriConstraintCollector<A, B, C, ?, Set<Mapped_>> toSet(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper) {
        return new ToSetTriCollector<>(mapper);
    }

    public static <A, B, C, Mapped_> TriConstraintCollector<A, B, C, ?, SortedSet<Mapped_>> toSortedSet(
            TriFunction<? super A, ? super B, ? super C, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        return new ToSortedSetComparatorTriCollector<>(mapper, comparator);
    }

    public static <A, B, C, Result_> TriConstraintCollector<A, B, C, ?, SequenceChain<Result_, Integer>>
            toConsecutiveSequences(TriFunction<A, B, C, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        return new ConsecutiveSequencesTriConstraintCollector<>(resultMap, indexMap);
    }

    public static <A, B, C, Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
            TriConstraintCollector<A, B, C, ?, ConnectedRangeChain<Interval_, Point_, Difference_>>
            toConnectedRanges(TriFunction<? super A, ? super B, ? super C, ? extends Interval_> mapper,
                    Function<? super Interval_, ? extends Point_> startMap,
                    Function<? super Interval_, ? extends Point_> endMap,
                    BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        return new ConnectedRangesTriConstraintCollector<>(mapper, startMap, endMap,
                differenceFunction);
    }

    public static <A, B, C, Intermediate_, Result_> TriConstraintCollector<A, B, C, ?, Result_>
            collectAndThen(TriConstraintCollector<A, B, C, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return new AndThenTriCollector<>(delegate, mappingFunction);
    }

    public static <A, B, C, Balanced_> TriConstraintCollector<A, B, C, ?, LoadBalance<Balanced_>> loadBalance(
            TriFunction<A, B, C, Balanced_> balancedItemFunction, ToLongTriFunction<A, B, C> loadFunction,
            ToLongTriFunction<A, B, C> initialLoadFunction) {
        return new LoadBalanceTriCollector<>(balancedItemFunction, loadFunction, initialLoadFunction);
    }

}
