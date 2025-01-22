package ai.timefold.solver.core.impl.score.stream.collector.uni;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.compose;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.max;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.min;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.asMap;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.asSet;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.asSortedMap;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.asSortedSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.emptySortedMap;
import static java.util.Collections.emptySortedSet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractConstraintCollectorsTest;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

final class InnerUniConstraintCollectorsTest extends AbstractConstraintCollectorsTest {

    @Override
    @Test
    public void count() {
        UniConstraintCollector<Integer, ?, Integer> collector = ConstraintCollectors.count();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0);
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 1);
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 3);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 2);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 1);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0);
    }

    @Override
    @Test
    public void countLong() {
        UniConstraintCollector<Long, ?, Long> collector = ConstraintCollectors.countLong();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        long firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 1L);
        // Add second value, we have two now.
        long secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2L);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 3L);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 2L);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 1L);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0L);
    }

    @Override
    @Test
    public void countDistinct() {
        UniConstraintCollector<Integer, ?, Integer> collector = ConstraintCollectors.countDistinct();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0);
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 1);
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2);
        // Add third value, same as the second. We now have two distinct values.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2);
        // Retract one instance of the second value; we still only have two distinct values.
        secondRetractor.run();
        assertResult(collector, container, 2);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 1);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0);
    }

    @Override
    @Test
    public void countDistinctLong() {
        UniConstraintCollector<Long, ?, Long> collector = ConstraintCollectors.countDistinctLong(Function.identity());
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        long firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 1L);
        // Add second value, we have two now.
        long secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2L);
        // Add third value, same as the second. We still have two distinct values.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2L);
        // Retract one instance of the second value. We still have two distinct values.
        secondRetractor.run();
        assertResult(collector, container, 2L);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 1L);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0L);
    }

    @Override
    @Test
    public void sum() {
        UniConstraintCollector<Integer, ?, Integer> collector = ConstraintCollectors.sum(i -> i);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0);
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 2);
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 3);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 4);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 3);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 2);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0);
    }

    @Override
    @Test
    public void sumLong() {
        UniConstraintCollector<Long, ?, Long> collector = ConstraintCollectors.sumLong(l -> l);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        long firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 2L);
        // Add second value, we have two now.
        long secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 3L);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 4L);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 3L);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 2L);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0L);
    }

    @Override
    @Test
    public void sumBigDecimal() {
        UniConstraintCollector<BigDecimal, ?, BigDecimal> collector = ConstraintCollectors.sumBigDecimal(l -> l);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, BigDecimal.ZERO);
        // Add first value, we have one now.
        BigDecimal firstValue = BigDecimal.ONE;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, BigDecimal.ONE);
        // Add second value, we have two now.
        BigDecimal secondValue = BigDecimal.TEN;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, BigDecimal.valueOf(11));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, BigDecimal.valueOf(21));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, BigDecimal.valueOf(11));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, BigDecimal.ONE);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, BigDecimal.ZERO);
    }

    @Override
    @Test
    public void sumBigInteger() {
        UniConstraintCollector<BigInteger, ?, BigInteger> collector = ConstraintCollectors.sumBigInteger(l -> l);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, BigInteger.ZERO);
        // Add first value, we have one now.
        BigInteger firstValue = BigInteger.ONE;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, BigInteger.ONE);
        // Add second value, we have two now.
        BigInteger secondValue = BigInteger.TEN;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, BigInteger.valueOf(11));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, BigInteger.valueOf(21));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, BigInteger.valueOf(11));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, BigInteger.ONE);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, BigInteger.ZERO);
    }

    @Override
    @Test
    public void sumDuration() {
        UniConstraintCollector<Duration, ?, Duration> collector = ConstraintCollectors.sumDuration(l -> l);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Duration.ZERO);
        // Add first value, we have one now.
        Duration firstValue = Duration.ofSeconds(1);
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, firstValue);
        // Add second value, we have two now.
        Duration secondValue = Duration.ofMinutes(1);
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, Duration.ofSeconds(61));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, Duration.ofSeconds(121));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, Duration.ofSeconds(61));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, firstValue);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, Duration.ZERO);
    }

    @Override
    @Test
    public void sumPeriod() {
        UniConstraintCollector<Period, ?, Period> collector = ConstraintCollectors.sumPeriod(l -> l);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Period.ZERO);
        // Add first value, we have one now.
        Period firstValue = Period.ofDays(1);
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, firstValue);
        // Add second value, we have two now.
        Period secondValue = Period.ofDays(2);
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, Period.ofDays(3));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, Period.ofDays(5));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, Period.ofDays(3));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, firstValue);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, Period.ZERO);
    }

    @Override
    @Test
    public void minComparable() {
        UniConstraintCollector<Integer, ?, Integer> collector = min();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the min
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, firstValue);
        // add second value, lesser than the first, becomes the new min
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, secondValue);
        // add third value, same as the second, result does not change
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, secondValue);
        // retract one instance of the second value; second value is still the min value, nothing should change
        secondRetractor.run();
        assertResult(collector, container, secondValue);
        // retract final instance of the second value; first value is now the min value
        thirdRetractor.run();
        assertResult(collector, container, firstValue);
        // retract last value; there are no values now
        firstRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void minNotComparable() {
        UniConstraintCollector<Object, ?, Object> collector = min(Function.identity(), o -> (String) o);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the min
        String firstValue = "b";
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, firstValue);
        // add second value, lesser than the first, becomes the new min
        String secondValue = "a";
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, secondValue);
        // add third value, same as the second, result does not change
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, secondValue);
        // retract one instance of the second value; second value is still the min value, nothing should change
        secondRetractor.run();
        assertResult(collector, container, secondValue);
        // retract final instance of the second value; first value is now the min value
        thirdRetractor.run();
        assertResult(collector, container, firstValue);
        // retract last value; there are no values now
        firstRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void maxComparable() {
        UniConstraintCollector<Integer, ?, Integer> collector = max();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the max
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, firstValue);
        // add second value, lesser than the first, result does not change
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, firstValue);
        // add third value, same as the first, result does not change
        Runnable thirdRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, firstValue);
        // retract one instance of the first value; first value is still the max value, nothing should change
        firstRetractor.run();
        assertResult(collector, container, firstValue);
        // retract final instance of the first value; second value is now the max value
        thirdRetractor.run();
        assertResult(collector, container, secondValue);
        // retract last value; there are no values now
        secondRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void maxNotComparable() {
        UniConstraintCollector<String, ?, String> collector = max(Function.identity(), o -> o);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the max
        String firstValue = "b";
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, firstValue);
        // add second value, lesser than the first, result does not change
        String secondValue = "a";
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, firstValue);
        // add third value, same as the first, result does not change
        Runnable thirdRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, firstValue);
        // retract one instance of the first value; first value is still the max value, nothing should change
        firstRetractor.run();
        assertResult(collector, container, firstValue);
        // retract final instance of the first value; second value is now the max value
        thirdRetractor.run();
        assertResult(collector, container, secondValue);
        // retract last value; there are no values now
        secondRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void average() {
        UniConstraintCollector<Integer, ?, Double> collector = ConstraintCollectors.average(i -> i);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 4.0D);
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2.5D);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2.0D);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 2.5D);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 4.0);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void averageLong() {
        UniConstraintCollector<Integer, ?, Double> collector = ConstraintCollectors.averageLong(i -> i);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 4.0D);
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2.5D);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2.0D);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 2.5D);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 4.0);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void averageBigDecimal() {
        UniConstraintCollector<Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigDecimal(i -> BigDecimal.valueOf(i));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, BigDecimal.valueOf(4));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, BigDecimal.valueOf(2)); // Scale = 0.
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, BigDecimal.valueOf(2));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, BigDecimal.valueOf(2)); // Scale = 0.
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, BigDecimal.valueOf(4));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void averageBigInteger() {
        UniConstraintCollector<Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigInteger(i -> BigInteger.valueOf(i));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, BigDecimal.valueOf(4));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, BigDecimal.valueOf(2)); // Scale = 0.
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, BigDecimal.valueOf(2));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, BigDecimal.valueOf(2)); // Scale = 0.
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, BigDecimal.valueOf(4));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void averageDuration() {
        UniConstraintCollector<Integer, ?, Duration> collector =
                ConstraintCollectors.averageDuration(i -> Duration.ofSeconds(i));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, Duration.ofSeconds(4));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, Duration.ofMillis(2500));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, Duration.ofSeconds(2));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, Duration.ofMillis(2500));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, Duration.ofSeconds(4));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void toSet() {
        UniConstraintCollector<Integer, ?, Set<Integer>> collector = ConstraintCollectors.toSet();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySet());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, singleton(firstValue));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asSet(firstValue, secondValue));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asSet(firstValue, secondValue));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSet(firstValue, secondValue));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, singleton(firstValue));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySet());
    }

    @Override
    @Test
    public void toSortedSet() {
        UniConstraintCollector<Integer, ?, SortedSet<Integer>> collector = ConstraintCollectors.toSortedSet();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedSet());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, asSortedSet(firstValue));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asSortedSet(firstValue, secondValue));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asSortedSet(firstValue, secondValue));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSortedSet(firstValue, secondValue));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asSortedSet(firstValue));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySortedSet());
    }

    @Override
    @Test
    public void toList() {
        UniConstraintCollector<Integer, ?, List<Integer>> collector = ConstraintCollectors.toList();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyList());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, singletonList(firstValue));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asList(firstValue, secondValue));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asList(firstValue, secondValue, secondValue));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asList(firstValue, secondValue));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, singletonList(firstValue));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptyList());
    }

    @Override
    @Test
    public void toMap() {
        UniConstraintCollector<Integer, ?, Map<Integer, Set<Integer>>> collector = ConstraintCollectors
                .toMap(Function.identity(), Function.identity());
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, asMap(2, singleton(2)));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asMap(2, singleton(2), 1, singleton(1)));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asMap(2, singleton(2), 1, singleton(1)));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asMap(2, singleton(2), 1, singleton(1)));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asMap(2, singleton(2)));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptyMap());
    }

    @Override
    @Test
    public void toMapMerged() {
        var counter = new AtomicInteger(0);
        var collector = ConstraintCollectors.toMap(Function.identity(), (Integer key) -> {
            // Test situations where the same key maps to two different values.
            var value = counter.incrementAndGet();
            return value % 2 == 1 ? key : key + 1;
        }, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, asMap(2, 2));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asMap(2, 2, 1, 2));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asMap(2, 2, 1, 3));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asMap(2, 2, 1, 1));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asMap(2, 2));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptyMap());
    }

    @Override
    @Test
    public void toSortedMap() {
        UniConstraintCollector<Integer, ?, SortedMap<Integer, Set<Integer>>> collector = ConstraintCollectors
                .toSortedMap(a -> a, Function.identity());
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, asSortedMap(2, singleton(2)));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asSortedMap(2, singleton(2), 1, singleton(1)));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asSortedMap(2, singleton(2), 1, singleton(1)));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSortedMap(2, singleton(2), 1, singleton(1)));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asSortedMap(2, singleton(2)));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySortedMap());
    }

    @Override
    @Test
    public void toSortedMapMerged() {
        var counter = new AtomicInteger(0);
        UniConstraintCollector<Integer, ?, SortedMap<Integer, Integer>> collector =
                ConstraintCollectors.toSortedMap(Function.identity(), key -> {
                    // Test situations where the same key maps to two different values.
                    var value = counter.incrementAndGet();
                    return value % 2 == 1 ? key : key + 1;
                }, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, asSortedMap(2, 2));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asSortedMap(2, 2, 1, 2));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, asSortedMap(2, 2, 1, 3));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSortedMap(2, 2, 1, 1));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asSortedMap(2, 2));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySortedMap());
    }

    @Override
    @Test
    public void conditionally() {
        UniConstraintCollector<Integer, Object, Integer> collector = ConstraintCollectors.conditionally(
                (Integer i) -> i > 1,
                min());
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 2);
        // Add second value; it is skipped even though it is lesser than the first value.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 2);
        // Add third value, same as the first. We now have two values, both of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 2);
        // Retract one instance of the first value; we only have one value now.
        firstRetractor.run();
        assertResult(collector, container, 2);
        // Retract the skipped value.
        secondRetractor.run();
        assertResult(collector, container, 2);
        // Retract last value; there are no values now.
        thirdRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void compose2() {
        UniConstraintCollector<Integer, ?, Pair<Integer, Integer>> collector =
                compose(min(i -> i), max(i -> i),
                        (BiFunction<Integer, Integer, Pair<Integer, Integer>>) Pair::new);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, new Pair<>(null, null));
        // Add first value.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, new Pair<>(2, 2));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, new Pair<>(1, 2));
        // Add third value, same as the second, result does not change.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, new Pair<>(1, 2));
        // Retract one instance of the second value; nothing should change.
        secondRetractor.run();
        assertResult(collector, container, new Pair<>(1, 2));
        // Retract final instance of the second value.
        thirdRetractor.run();
        assertResult(collector, container, new Pair<>(2, 2));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, new Pair<>(null, null));
    }

    @Override
    @Test
    public void compose3() {
        UniConstraintCollector<Integer, ?, Triple<Integer, Integer, Double>> collector =
                compose(min(i -> i), max(i -> i), ConstraintCollectors.average(i -> i),
                        (TriFunction<Integer, Integer, Double, Triple<Integer, Integer, Double>>) Triple::of);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Triple.of(null, null, null));
        // Add first value.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, Triple.of(4, 4, 4D));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, Triple.of(1, 4, 2.5D));
        // Add third value, same as the second.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, Triple.of(1, 4, 2D));
        // Retract one instance of the second value.
        secondRetractor.run();
        assertResult(collector, container, Triple.of(1, 4, 2.5D));
        // Retract final instance of the second value.
        thirdRetractor.run();
        assertResult(collector, container, Triple.of(4, 4, 4D));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, Triple.of(null, null, null));
    }

    @Override
    @Test
    public void compose4() {
        UniConstraintCollector<Integer, ?, Quadruple<Integer, Integer, Integer, Double>> collector =
                compose(ConstraintCollectors.count(), min(i -> i), max(i -> i),
                        ConstraintCollectors.average(i -> i),
                        (QuadFunction<Integer, Integer, Integer, Double, Quadruple<Integer, Integer, Integer, Double>>) Quadruple::new);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, new Quadruple<>(0, null, null, null));
        // Add first value.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, new Quadruple<>(1, 4, 4, 4D));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, new Quadruple<>(2, 1, 4, 2.5D));
        // Add third value, same as the second.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, new Quadruple<>(3, 1, 4, 2D));
        // Retract one instance of the second value.
        secondRetractor.run();
        assertResult(collector, container, new Quadruple<>(2, 1, 4, 2.5D));
        // Retract final instance of the second value.
        thirdRetractor.run();
        assertResult(collector, container, new Quadruple<>(1, 4, 4, 4D));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, new Quadruple<>(0, null, null, null));
    }

    @Override
    @Test
    public void toConsecutiveSequences() {
        // Do a basic test w/o edge cases; edge cases are covered in ConsecutiveSetTreeTest
        var collector = ConstraintCollectors.toConsecutiveSequences(Integer::intValue);
        var container = collector.supplier().get();
        // Add first value, sequence is [2]
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResultRecursive(collector, container, buildSequenceChain(2));
        // Add second value, sequence is [1,2]
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResultRecursive(collector, container, buildSequenceChain(1, 2));
        // Add third value, same as the second. Sequence is [{1,1},2}]
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResultRecursive(collector, container, buildSequenceChain(1, 1, 2));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResultRecursive(collector, container, buildSequenceChain(1, 2));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResultRecursive(collector, container, buildSequenceChain(2));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResultRecursive(collector, container, buildSequenceChain());
    }

    @Override
    @Test
    public void consecutiveUsage() {
        var collector =
                ConstraintCollectors.toConnectedRanges(
                        Interval::start,
                        Interval::end, (a, b) -> b - a);
        var container = collector.supplier().get();
        // Add first value, sequence is [(1,3)]
        var firstRetractor = accumulate(collector, container, new Interval(1, 3));
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3)));
        // Add second value, sequence is [(1,3),(2,4)]
        var secondRetractor = accumulate(collector, container, new Interval(2, 4));
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3), new Interval(2, 4)));
        // Add third value, same as the second. Sequence is [{1,1},2}]
        var thirdRetractor = accumulate(collector, container, new Interval(2, 4));
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3), new Interval(2, 4), new Interval(2, 4)));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3), new Interval(2, 4)));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3)));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, buildConsecutiveUsage());
    }

    @Override
    @Test
    public void consecutiveUsageDynamic() {
        var dynamicCollector =
                ConstraintCollectors.toConnectedRanges(
                        DynamicInterval::getStart,
                        DynamicInterval::getEnd, (a, b) -> b - a);

        var first = new DynamicInterval(0);
        var second = new DynamicInterval(10);
        var third = new DynamicInterval(20);
        var container = dynamicCollector.supplier().get();

        // Add first value, sequence is [[(0, 10)]]
        var firstRetractor = accumulate(dynamicCollector, container, first);
        assertResult(dynamicCollector, container, buildDynamicConsecutiveUsage(new DynamicInterval(0)));

        // Add third value, sequence is [[(0, 10)], [(20, 30)]]
        accumulate(dynamicCollector, container, third);
        assertResult(dynamicCollector, container,
                buildDynamicConsecutiveUsage(new DynamicInterval(0), new DynamicInterval(20)));

        // Add second value, sequence is [[(0, 10), (10, 20), (20, 30)]]
        accumulate(dynamicCollector, container, second);
        assertResult(dynamicCollector, container,
                buildDynamicConsecutiveUsage(new DynamicInterval(0), new DynamicInterval(10), new DynamicInterval(20)));

        // Change first value, retract it, then re-add it
        first.setStart(-5);
        firstRetractor.run();
        accumulate(dynamicCollector, container, first);

        assertResult(dynamicCollector, container,
                buildDynamicConsecutiveUsage(new DynamicInterval(-5), new DynamicInterval(10), new DynamicInterval(20)));
    }

    @Override
    @Test
    public void loadBalance() {
        var collector = ConstraintCollectors.<LoadBalanced, String> loadBalance(a -> a.value, a -> a.metric);
        var container = collector.supplier().get();

        // Default state.
        assertUnfairness(collector, container, BigDecimal.ZERO);
        // Add first value, we have one now.
        var firstValue = new LoadBalanced("A", 2);
        var firstRetractor = accumulate(collector, container, firstValue);
        assertUnfairness(collector, container, BigDecimal.ZERO);
        // Add second value, we have two now.
        var secondValue = new LoadBalanced("B", 1);
        var secondRetractor = accumulate(collector, container, secondValue);
        // sqrt((3-2.5)^2 + (2 - 2.5)^2) = sqrt(0.5^2 + 0.5^2) = sqrt(0.25 + 0.25) = sqrt(0.5)
        assertUnfairness(collector, container, BigDecimal.valueOf(0.707107));
        // Add third value, same as the second. We now have three values, two of which are the same.
        var thirdRetractor = accumulate(collector, container, secondValue);
        assertUnfairness(collector, container, BigDecimal.ZERO); // Perfectly fair again.
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertUnfairness(collector, container, BigDecimal.valueOf(0.707107));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertUnfairness(collector, container, BigDecimal.ZERO);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertUnfairness(collector, container, BigDecimal.ZERO);
    }

    private record LoadBalanced(String value, int metric) {

    }

    @Override
    @Test
    public void collectAndThen() {
        var collector = ConstraintCollectors.collectAndThen(ConstraintCollectors.count(), i -> i * 10);
        var container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0);
        // Add first value, we have one now.
        var firstValue = 2;
        var firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 10);
        // Add second value, we have two now.
        var secondValue = 1;
        var secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 20);
        // Add third value, same as the second. We now have three values, two of which are the same.
        var thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 30);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 20);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 10);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0);
    }

    private static <A, Container_, Result_> Runnable accumulate(
            UniConstraintCollector<A, Container_, Result_> collector, Object container, A value) {
        return collector.accumulator().apply((Container_) container, value);
    }

    private static <A, Container_, Result_> void assertResult(
            UniConstraintCollector<A, Container_, Result_> collector, Object container, Result_ expectedResult) {
        Result_ actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult)
                .as("Collector (" + collector + ") did not produce expected result.")
                .isEqualTo(expectedResult);
    }

    private static <A, Container_, Result_> void assertResultRecursive(UniConstraintCollector<A, Container_, Result_> collector,
            Object container, Result_ expectedResult) {
        var actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult)
                .as("Collector (" + collector + ") did not produce expected result.")
                .usingRecursiveComparison()
                .ignoringFields("sourceTree", "indexFunction", "sequenceList", "startItemToSequence")
                .isEqualTo(expectedResult);
    }

    private static <Container_> void assertUnfairness(
            UniConstraintCollector<LoadBalanced, Container_, LoadBalance<String>> collector, Object container,
            BigDecimal expectedValue) {
        LoadBalance<String> actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult.unfairness())
                .as("Collector (" + collector + ") did not produce expected result.")
                .isEqualTo(expectedValue);
    }

}
