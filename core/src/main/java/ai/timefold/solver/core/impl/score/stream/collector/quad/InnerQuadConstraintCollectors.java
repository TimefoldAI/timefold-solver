package ai.timefold.solver.core.impl.score.stream.collector.quad;

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
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

public class InnerQuadConstraintCollectors {
    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Double> average(
            ToIntQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        return new AverageIntQuadCollector<>(mapper);
    }

    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Double> average(
            ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        return new AverageLongQuadCollector<>(mapper);
    }

    static <A, B, C, D, Mapped_, Average_> QuadConstraintCollector<A, B, C, D, ?, Average_> average(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
            Supplier<ReferenceAverageCalculator<Mapped_, Average_>> calculatorSupplier) {
        return new AverageReferenceQuadCollector<>(mapper, calculatorSupplier);
    }

    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, BigDecimal> averageBigDecimal(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends BigDecimal> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, BigDecimal> averageBigInteger(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends BigInteger> mapper) {
        return average(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Duration> averageDuration(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Duration> mapper) {
        return average(mapper, ReferenceAverageCalculator.duration());
    }

    public static <A, B, C, D, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            QuadConstraintCollector<A, B, C, D, ?, Result_>
            compose(
                    QuadConstraintCollector<A, B, C, D, ResultHolder1_, Result1_> first,
                    QuadConstraintCollector<A, B, C, D, ResultHolder2_, Result2_> second,
                    QuadConstraintCollector<A, B, C, D, ResultHolder3_, Result3_> third,
                    QuadConstraintCollector<A, B, C, D, ResultHolder4_, Result4_> fourth,
                    QuadFunction<Result1_, Result2_, Result3_, Result4_, Result_> composeFunction) {
        return new ComposeFourQuadCollector<>(
                first, second, third, fourth, composeFunction);
    }

    public static <A, B, C, D, ResultHolder1_, ResultHolder2_, ResultHolder3_, Result1_, Result2_, Result3_, Result_>
            QuadConstraintCollector<A, B, C, D, ?, Result_>
            compose(
                    QuadConstraintCollector<A, B, C, D, ResultHolder1_, Result1_> first,
                    QuadConstraintCollector<A, B, C, D, ResultHolder2_, Result2_> second,
                    QuadConstraintCollector<A, B, C, D, ResultHolder3_, Result3_> third,
                    TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction) {
        return new ComposeThreeQuadCollector<>(
                first, second, third, composeFunction);
    }

    public static <A, B, C, D, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
            QuadConstraintCollector<A, B, C, D, ?, Result_> compose(
                    QuadConstraintCollector<A, B, C, D, ResultHolder1_, Result1_> first,
                    QuadConstraintCollector<A, B, C, D, ResultHolder2_, Result2_> second,
                    BiFunction<Result1_, Result2_, Result_> composeFunction) {
        return new ComposeTwoQuadCollector<>(first,
                second, composeFunction);
    }

    public static <A, B, C, D, ResultContainer_, Result_> QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_>
            conditionally(
                    QuadPredicate<A, B, C, D> predicate,
                    QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> delegate) {
        return new ConditionalQuadCollector<>(predicate, delegate);
    }

    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Integer> count() {
        return CountIntQuadCollector.getInstance();
    }

    public static <A, B, C, D, Mapped_> QuadConstraintCollector<A, B, C, D, ?, Integer> countDistinct(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        return new CountDistinctIntQuadCollector<>(mapper);
    }

    public static <A, B, C, D, Mapped_> QuadConstraintCollector<A, B, C, D, ?, Long> countDistinctLong(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        return new CountDistinctLongQuadCollector<>(mapper);
    }

    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Long> countLong() {
        return CountLongQuadCollector.getInstance();
    }

    public static <A, B, C, D, Result_ extends Comparable<? super Result_>> QuadConstraintCollector<A, B, C, D, ?, Result_> max(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper) {
        return new MaxComparableQuadCollector<>(mapper);
    }

    public static <A, B, C, D, Result_> QuadConstraintCollector<A, B, C, D, ?, Result_> max(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MaxComparatorQuadCollector<>(mapper, comparator);
    }

    public static <A, B, C, D, Result_, Property_ extends Comparable<? super Property_>>
            QuadConstraintCollector<A, B, C, D, ?, Result_> max(
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MaxPropertyQuadCollector<>(mapper, propertyMapper);
    }

    public static <A, B, C, D, Result_ extends Comparable<? super Result_>> QuadConstraintCollector<A, B, C, D, ?, Result_> min(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper) {
        return new MinComparableQuadCollector<>(mapper);
    }

    public static <A, B, C, D, Result_> QuadConstraintCollector<A, B, C, D, ?, Result_> min(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MinComparatorQuadCollector<>(mapper, comparator);
    }

    public static <A, B, C, D, Result_, Property_ extends Comparable<? super Property_>>
            QuadConstraintCollector<A, B, C, D, ?, Result_> min(
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MinPropertyQuadCollector<>(mapper, propertyMapper);
    }

    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Integer> sum(
            ToIntQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        return new SumIntQuadCollector<>(mapper);
    }

    public static <A, B, C, D> QuadConstraintCollector<A, B, C, D, ?, Long> sum(
            ToLongQuadFunction<? super A, ? super B, ? super C, ? super D> mapper) {
        return new SumLongQuadCollector<>(mapper);
    }

    public static <A, B, C, D, Result_> QuadConstraintCollector<A, B, C, D, ?, Result_> sum(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper,
            Result_ zero,
            BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        return new SumReferenceQuadCollector<>(mapper, zero, adder, subtractor);
    }

    public static <A, B, C, D, Mapped_, Result_ extends Collection<Mapped_>>
            QuadConstraintCollector<A, B, C, D, ?, Result_> toCollection(
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
                    IntFunction<Result_> collectionFunction) {
        return new ToCollectionQuadCollector<>(mapper, collectionFunction);
    }

    public static <A, B, C, D, Mapped_> QuadConstraintCollector<A, B, C, D, ?, List<Mapped_>> toList(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        return new ToListQuadCollector<>(mapper);
    }

    public static <A, B, C, D, Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
            QuadConstraintCollector<A, B, C, D, ?, Result_> toMap(
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key_> keyFunction,
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    IntFunction<Set_> setFunction) {
        return new ToMultiMapQuadCollector<>(keyFunction, valueFunction, mapSupplier,
                setFunction);
    }

    public static <A, B, C, D, Key_, Value_, Result_ extends Map<Key_, Value_>>
            QuadConstraintCollector<A, B, C, D, ?, Result_> toMap(
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Key_> keyFunction,
                    QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    BinaryOperator<Value_> mergeFunction) {
        return new ToSimpleMapQuadCollector<>(keyFunction, valueFunction, mapSupplier,
                mergeFunction);
    }

    public static <A, B, C, D, Mapped_> QuadConstraintCollector<A, B, C, D, ?, Set<Mapped_>> toSet(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper) {
        return new ToSetQuadCollector<>(mapper);
    }

    public static <A, B, C, D, Mapped_> QuadConstraintCollector<A, B, C, D, ?, SortedSet<Mapped_>> toSortedSet(
            QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        return new ToSortedSetComparatorQuadCollector<>(mapper, comparator);
    }

    public static <A, B, C, D, Result_> QuadConstraintCollector<A, B, C, D, ?, SequenceChain<Result_, Integer>>
            toConsecutiveSequences(QuadFunction<A, B, C, D, Result_> resultMap, ToIntFunction<Result_> indexMap) {
        return new ConsecutiveSequencesQuadConstraintCollector<>(resultMap, indexMap);
    }

    public static <A, B, C, D, Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
            QuadConstraintCollector<A, B, C, D, ?, ConnectedRangeChain<Interval_, Point_, Difference_>>
            toConnectedRanges(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Interval_> mapper,
                    Function<? super Interval_, ? extends Point_> startMap,
                    Function<? super Interval_, ? extends Point_> endMap,
                    BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        return new ConnectedRangesQuadConstraintCollector<>(mapper, startMap, endMap,
                differenceFunction);
    }

    public static <A, B, C, D, Intermediate_, Result_> QuadConstraintCollector<A, B, C, D, ?, Result_>
            collectAndThen(QuadConstraintCollector<A, B, C, D, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return new AndThenQuadCollector<>(delegate, mappingFunction);
    }

    public static <A, B, C, D, Balanced_> QuadConstraintCollector<A, B, C, D, ?, LoadBalance<Balanced_>> loadBalance(
            QuadFunction<A, B, C, D, Balanced_> balancedItemFunction, ToLongQuadFunction<A, B, C, D> loadFunction,
            ToLongQuadFunction<A, B, C, D> initialLoadFunction) {
        return new LoadBalanceQuadCollector<>(balancedItemFunction, loadFunction, initialLoadFunction);
    }

}
