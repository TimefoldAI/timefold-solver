package ai.timefold.solver.core.impl.score.stream.collector.uni;

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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.ReferenceAverageCalculator;

public class InnerUniConstraintCollectors {
    public static <A> UniConstraintCollector<A, ?, Double> average(ToIntFunction<? super A> mapper) {
        return new AverageIntUniCollector<>(mapper);
    }

    public static <A> UniConstraintCollector<A, ?, Double> average(ToLongFunction<? super A> mapper) {
        return new AverageLongUniCollector<>(mapper);
    }

    public static <A> UniConstraintCollector<A, ?, BigDecimal> averageBigDecimal(
            Function<? super A, ? extends BigDecimal> mapper) {
        return new AverageReferenceUniCollector<>(mapper, ReferenceAverageCalculator.bigDecimal());
    }

    public static <A> UniConstraintCollector<A, ?, BigDecimal> averageBigInteger(
            Function<? super A, ? extends BigInteger> mapper) {
        return new AverageReferenceUniCollector<>(mapper, ReferenceAverageCalculator.bigInteger());
    }

    public static <A> UniConstraintCollector<A, ?, Duration> averageDuration(
            Function<? super A, ? extends Duration> mapper) {
        return new AverageReferenceUniCollector<>(mapper, ReferenceAverageCalculator.duration());
    }

    public static <A, ResultHolder1_, ResultHolder2_, ResultHolder3_, ResultHolder4_, Result1_, Result2_, Result3_, Result4_, Result_>
            UniConstraintCollector<A, ?, Result_>
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
            UniConstraintCollector<A, ?, Result_>
            compose(
                    UniConstraintCollector<A, ResultHolder1_, Result1_> first,
                    UniConstraintCollector<A, ResultHolder2_, Result2_> second,
                    UniConstraintCollector<A, ResultHolder3_, Result3_> third,
                    TriFunction<Result1_, Result2_, Result3_, Result_> composeFunction) {
        return new ComposeThreeUniCollector<>(
                first, second, third, composeFunction);
    }

    public static <A, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
            UniConstraintCollector<A, ?, Result_> compose(
                    UniConstraintCollector<A, ResultHolder1_, Result1_> first,
                    UniConstraintCollector<A, ResultHolder2_, Result2_> second,
                    BiFunction<Result1_, Result2_, Result_> composeFunction) {
        return new ComposeTwoUniCollector<>(first, second,
                composeFunction);
    }

    public static <A, ResultContainer_, Result_> UniConstraintCollector<A, ResultContainer_, Result_> conditionally(
            Predicate<A> predicate, UniConstraintCollector<A, ResultContainer_, Result_> delegate) {
        return new ConditionalUniCollector<>(predicate, delegate);
    }

    public static <A> UniConstraintCollector<A, ?, Integer> count() {
        return CountIntUniCollector.getInstance();
    }

    public static <A, Mapped_> UniConstraintCollector<A, ?, Integer> countDistinct(
            Function<? super A, ? extends Mapped_> mapper) {
        return new CountDistinctIntUniCollector<>(mapper);
    }

    public static <A, Mapped_> UniConstraintCollector<A, ?, Long> countDistinctLong(
            Function<? super A, ? extends Mapped_> mapper) {
        return new CountDistinctLongUniCollector<>(mapper);
    }

    public static <A> UniConstraintCollector<A, ?, Long> countLong() {
        return CountLongUniCollector.getInstance();
    }

    public static <A, Result_ extends Comparable<? super Result_>> UniConstraintCollector<A, ?, Result_> max(
            Function<? super A, ? extends Result_> mapper) {
        return new MaxComparableUniCollector<>(mapper);
    }

    public static <A, Result_> UniConstraintCollector<A, ?, Result_> max(Function<? super A, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MaxComparatorUniCollector<>(mapper, comparator);
    }

    public static <A, Result_, Property_ extends Comparable<? super Property_>> UniConstraintCollector<A, ?, Result_>
            max(
                    Function<? super A, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MaxPropertyUniCollector<>(mapper, propertyMapper);
    }

    public static <A, Result_ extends Comparable<? super Result_>> UniConstraintCollector<A, ?, Result_> min(
            Function<? super A, ? extends Result_> mapper) {
        return new MinComparableUniCollector<>(mapper);
    }

    public static <A, Result_> UniConstraintCollector<A, ?, Result_> min(Function<? super A, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        return new MinComparatorUniCollector<>(mapper, comparator);
    }

    public static <A, Result_, Property_ extends Comparable<? super Property_>> UniConstraintCollector<A, ?, Result_>
            min(
                    Function<? super A, ? extends Result_> mapper,
                    Function<? super Result_, ? extends Property_> propertyMapper) {
        return new MinPropertyUniCollector<>(mapper, propertyMapper);
    }

    public static <A> UniConstraintCollector<A, ?, Integer> sum(ToIntFunction<? super A> mapper) {
        return new SumIntUniCollector<>(mapper);
    }

    public static <A> UniConstraintCollector<A, ?, Long> sum(ToLongFunction<? super A> mapper) {
        return new SumLongUniCollector<>(mapper);
    }

    public static <A, Result_> UniConstraintCollector<A, ?, Result_> sum(Function<? super A, ? extends Result_> mapper,
            Result_ zero, BinaryOperator<Result_> adder,
            BinaryOperator<Result_> subtractor) {
        return new SumReferenceUniCollector<>(mapper, zero, adder, subtractor);
    }

    public static <A, Mapped_, Result_ extends Collection<Mapped_>> UniConstraintCollector<A, ?, Result_>
            toCollection(
                    Function<? super A, ? extends Mapped_> mapper,
                    IntFunction<Result_> collectionFunction) {
        return new ToCollectionUniCollector<>(mapper, collectionFunction);
    }

    public static <A, Mapped_> UniConstraintCollector<A, ?, List<Mapped_>> toList(
            Function<? super A, ? extends Mapped_> mapper) {
        return new ToListUniCollector<>(mapper);
    }

    public static <A, Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
            UniConstraintCollector<A, ?, Result_> toMap(
                    Function<? super A, ? extends Key_> keyFunction,
                    Function<? super A, ? extends Value_> valueFunction,
                    Supplier<Result_> mapSupplier,
                    IntFunction<Set_> setFunction) {
        return new ToMultiMapUniCollector<>(keyFunction, valueFunction, mapSupplier, setFunction);
    }

    public static <A, Key_, Value_, Result_ extends Map<Key_, Value_>> UniConstraintCollector<A, ?, Result_> toMap(
            Function<? super A, ? extends Key_> keyFunction,
            Function<? super A, ? extends Value_> valueFunction,
            Supplier<Result_> mapSupplier,
            BinaryOperator<Value_> mergeFunction) {
        return new ToSimpleMapUniCollector<>(keyFunction, valueFunction, mapSupplier, mergeFunction);
    }

    public static <A, Mapped_> UniConstraintCollector<A, ?, Set<Mapped_>> toSet(Function<? super A, ? extends Mapped_> mapper) {
        return new ToSetUniCollector<>(mapper);
    }

    public static <A, Mapped_> UniConstraintCollector<A, ?, SortedSet<Mapped_>> toSortedSet(
            Function<? super A, ? extends Mapped_> mapper,
            Comparator<? super Mapped_> comparator) {
        return new ToSortedSetComparatorUniCollector<>(mapper, comparator);
    }

    public static <A> UniConstraintCollector<A, ?, SequenceChain<A, Integer>>
            toConsecutiveSequences(ToIntFunction<A> indexMap) {
        return new ConsecutiveSequencesUniConstraintCollector<>(indexMap);
    }

    public static <A, Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
            UniConstraintCollector<A, ?, ConnectedRangeChain<Interval_, Point_, Difference_>>
            toConnectedRanges(Function<? super A, ? extends Interval_> mapper,
                    Function<? super Interval_, ? extends Point_> startMap,
                    Function<? super Interval_, ? extends Point_> endMap,
                    BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        return new ConnectedRangesUniConstraintCollector<>(mapper, startMap, endMap,
                differenceFunction);
    }

    public static <A, Intermediate_, Result_> UniConstraintCollector<A, ?, Result_>
            collectAndThen(UniConstraintCollector<A, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return new AndThenUniCollector<>(delegate, mappingFunction);
    }

    public static <A, Balanced_> UniConstraintCollector<A, ?, LoadBalance<Balanced_>> loadBalance(
            Function<A, Balanced_> balancedItemFunction, ToLongFunction<A> loadFunction,
            ToLongFunction<A> initialLoadFunction) {
        return new LoadBalanceUniCollector<>(balancedItemFunction, loadFunction, initialLoadFunction);
    }

}
