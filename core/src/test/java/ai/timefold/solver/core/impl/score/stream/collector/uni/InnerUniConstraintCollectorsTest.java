package ai.timefold.solver.core.impl.score.stream.collector.uni;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.compose;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.max;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.min;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.asMap;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.asSet;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.asSortedMap;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.asSortedSet;
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
import java.util.ArrayList;
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
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractConstraintCollectorsTest;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

final class InnerUniConstraintCollectorsTest extends AbstractConstraintCollectorsTest {

    @Override
    @Test
    public void count() {
        UniConstraintCollector<Long, ?, Long> collector = ConstraintCollectors.count();
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
        UniConstraintCollector<Long, ?, Long> collector = ConstraintCollectors.countDistinct(Function.identity());
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
        UniConstraintCollector<Long, ?, Long> collector = ConstraintCollectors.sum(l -> l);
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
        UniConstraintCollector<Long, ?, Quadruple<Long, Long, Long, Double>> collector =
                compose(ConstraintCollectors.count(), min(i -> i), max(i -> i),
                        ConstraintCollectors.average(i -> i),
                        (QuadFunction<Long, Long, Long, Double, Quadruple<Long, Long, Long, Double>>) Quadruple::new);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, new Quadruple<>(0L, null, null, null));
        // Add first value.
        long firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, new Quadruple<>(1L, 4L, 4L, 4D));
        // Add second value, lesser than the first.
        long secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, new Quadruple<>(2L, 1L, 4L, 2.5D));
        // Add third value, same as the second.
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, new Quadruple<>(3L, 1L, 4L, 2D));
        // Retract one instance of the second value.
        secondRetractor.run();
        assertResult(collector, container, new Quadruple<>(2L, 1L, 4L, 2.5D));
        // Retract final instance of the second value.
        thirdRetractor.run();
        assertResult(collector, container, new Quadruple<>(1L, 4L, 4L, 4D));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, new Quadruple<>(0L, null, null, null));
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
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        var firstValue = 2;
        var firstRetractor = accumulate(collector, container, firstValue);
        assertResult(collector, container, 10L);
        // Add second value, we have two now.
        var secondValue = 1;
        var secondRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 20L);
        // Add third value, same as the second. We now have three values, two of which are the same.
        var thirdRetractor = accumulate(collector, container, secondValue);
        assertResult(collector, container, 30L);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 20L);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 10L);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0L);
    }

    private static <A, Container_, Result_> Runnable accumulate(
            UniConstraintCollector<A, Container_, Result_> collector, Object container, A value) {
        return collector.accumulator().apply((Container_) container, value);
    }

    private static <A, Container_, Result_> UniConstraintCollectorAccumulatedValue<A> insert(
            UniConstraintCollector<A, Container_, Result_> collector, Object container, A value) {
        var slot = collector.incrementalAccumulator().intoGroup((Container_) container);
        slot.add(value);
        return slot;
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

    @Test
    public void countUpdate() {
        UniConstraintCollector<Long, ?, Long> collector = ConstraintCollectors.count();
        Object container = collector.supplier().get();
        var slot = insert(collector, container, 1L);
        assertResult(collector, container, 1L);
        slot.update(42L); // no-op for count
        assertResult(collector, container, 1L);
        slot.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void conditionallyUpdate() {
        UniConstraintCollector<Integer, Object, Integer> collector =
                ConstraintCollectors.conditionally((Integer i) -> i > 1, min());
        Object container = collector.supplier().get();
        var slot = insert(collector, container, 2); // active (2 > 1)
        assertResult(collector, container, 2);
        slot.update(1); // active → inactive (1 is not > 1)
        assertResult(collector, container, null);
        slot.update(3); // inactive → active (3 > 1)
        assertResult(collector, container, 3);
        slot.update(4); // active → active
        assertResult(collector, container, 4);
        slot.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void compose2Update() {
        UniConstraintCollector<Integer, ?, Pair<Integer, Integer>> collector =
                compose(min(i -> i), max(i -> i),
                        (BiFunction<Integer, Integer, Pair<Integer, Integer>>) Pair::new);
        Object container = collector.supplier().get();
        var slot1 = insert(collector, container, 2);
        var slot2 = insert(collector, container, 4);
        assertResult(collector, container, new Pair<>(2, 4));
        slot1.update(3); // 2 → 3; min becomes 3
        assertResult(collector, container, new Pair<>(3, 4));
        slot2.remove();
        slot1.remove();
        assertResult(collector, container, new Pair<>(null, null));
    }

    @Test
    public void collectAndThenUpdate() {
        var collector = ConstraintCollectors.collectAndThen(ConstraintCollectors.count(), i -> i * 10);
        var container = collector.supplier().get();
        var slot = insert(collector, container, 1);
        assertResult(collector, container, 10L);
        slot.update(99); // count no-op; result unchanged
        assertResult(collector, container, 10L);
        slot.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void sumUpdate() {
        UniConstraintCollector<Long, ?, Long> collector = ConstraintCollectors.sum(l -> l);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2L);
        assertResult(collector, container, 2L);
        slot1.update(5L);
        assertResult(collector, container, 5L);
        slot1.update(5L); // no-op
        assertResult(collector, container, 5L);
        var slot2 = insert(collector, container, 3L);
        assertResult(collector, container, 8L);
        slot1.update(1L);
        assertResult(collector, container, 4L);
        slot2.remove();
        assertResult(collector, container, 1L);
        slot1.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void averageUpdate() {
        UniConstraintCollector<Integer, ?, Double> collector = ConstraintCollectors.average(i -> i);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4);
        var slot2 = insert(collector, container, 2);
        assertResult(collector, container, 3.0D);
        slot1.update(6); // (6+2)/2 = 4.0; count unchanged
        assertResult(collector, container, 4.0D);
        slot1.update(6); // no-op
        assertResult(collector, container, 4.0D);
        slot2.remove();
        assertResult(collector, container, 6.0D);
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void countDistinctUpdate() {
        UniConstraintCollector<String, ?, Long> collector = ConstraintCollectors.countDistinct(Function.identity());
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, "a");
        var slot2 = insert(collector, container, "b");
        assertResult(collector, container, 2L);
        slot1.update("b"); // both map to "b"
        assertResult(collector, container, 1L);
        slot1.update("b"); // no-op: Objects.equals short-circuit
        assertResult(collector, container, 1L);
        slot1.update("c"); // "b" and "c"
        assertResult(collector, container, 2L);
        slot2.remove();
        assertResult(collector, container, 1L);
        slot1.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void sumBigDecimalUpdate() {
        UniConstraintCollector<BigDecimal, ?, BigDecimal> collector = ConstraintCollectors.sumBigDecimal(l -> l);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, BigDecimal.ONE);
        var slot2 = insert(collector, container, BigDecimal.TEN);
        assertResult(collector, container, BigDecimal.valueOf(11));
        var bd4 = BigDecimal.valueOf(4);
        slot1.update(bd4);
        assertResult(collector, container, BigDecimal.valueOf(14));
        slot1.update(bd4); // no-op: same reference, == short-circuit
        assertResult(collector, container, BigDecimal.valueOf(14));
        slot2.remove();
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot1.remove();
        assertResult(collector, container, BigDecimal.ZERO);
    }

    @Test
    public void sumBigIntegerUpdate() {
        UniConstraintCollector<BigInteger, ?, BigInteger> collector = ConstraintCollectors.sumBigInteger(l -> l);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, BigInteger.ONE);
        var slot2 = insert(collector, container, BigInteger.TEN);
        assertResult(collector, container, BigInteger.valueOf(11));
        var bi4 = BigInteger.valueOf(4);
        slot1.update(bi4);
        assertResult(collector, container, BigInteger.valueOf(14));
        slot1.update(bi4); // no-op: same reference, == short-circuit
        assertResult(collector, container, BigInteger.valueOf(14));
        slot2.remove();
        assertResult(collector, container, BigInteger.valueOf(4));
        slot1.remove();
        assertResult(collector, container, BigInteger.ZERO);
    }

    @Test
    public void sumDurationUpdate() {
        UniConstraintCollector<Duration, ?, Duration> collector = ConstraintCollectors.sumDuration(l -> l);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, Duration.ofSeconds(1));
        var slot2 = insert(collector, container, Duration.ofSeconds(2));
        assertResult(collector, container, Duration.ofSeconds(3));
        var d4 = Duration.ofSeconds(4);
        slot1.update(d4);
        assertResult(collector, container, Duration.ofSeconds(6));
        slot1.update(d4); // no-op: same reference, == short-circuit
        assertResult(collector, container, Duration.ofSeconds(6));
        slot2.remove();
        assertResult(collector, container, Duration.ofSeconds(4));
        slot1.remove();
        assertResult(collector, container, Duration.ZERO);
    }

    @Test
    public void sumPeriodUpdate() {
        UniConstraintCollector<Period, ?, Period> collector = ConstraintCollectors.sumPeriod(l -> l);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, Period.ofDays(1));
        var slot2 = insert(collector, container, Period.ofDays(2));
        assertResult(collector, container, Period.ofDays(3));
        var p4 = Period.ofDays(4);
        slot1.update(p4);
        assertResult(collector, container, Period.ofDays(6));
        slot1.update(p4); // no-op: same reference, == short-circuit
        assertResult(collector, container, Period.ofDays(6));
        slot2.remove();
        assertResult(collector, container, Period.ofDays(4));
        slot1.remove();
        assertResult(collector, container, Period.ZERO);
    }

    @Test
    public void averageBigDecimalUpdate() {
        UniConstraintCollector<Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigDecimal(i -> BigDecimal.valueOf(i));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4);
        var slot2 = insert(collector, container, 2);
        assertResult(collector, container, BigDecimal.valueOf(3));
        slot1.update(6); // (6+2)/2 = 4; count unchanged
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot1.update(6); // no-op: same input value
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot2.remove();
        assertResult(collector, container, BigDecimal.valueOf(6));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void averageBigIntegerUpdate() {
        UniConstraintCollector<Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigInteger(i -> BigInteger.valueOf(i));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4);
        var slot2 = insert(collector, container, 2);
        assertResult(collector, container, BigDecimal.valueOf(3));
        slot1.update(6);
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot1.update(6); // no-op: same input value
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot2.remove();
        assertResult(collector, container, BigDecimal.valueOf(6));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void averageDurationUpdate() {
        UniConstraintCollector<Integer, ?, Duration> collector =
                ConstraintCollectors.averageDuration(i -> Duration.ofSeconds(i));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4);
        var slot2 = insert(collector, container, 2);
        assertResult(collector, container, Duration.ofSeconds(3));
        slot1.update(6); // (6+2)/2 = 4; count unchanged
        assertResult(collector, container, Duration.ofSeconds(4));
        slot1.update(6); // no-op: same input value
        assertResult(collector, container, Duration.ofSeconds(4));
        slot2.remove();
        assertResult(collector, container, Duration.ofSeconds(6));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void toConsecutiveSequencesUpdate() {
        var collector = ConstraintCollectors.toConsecutiveSequences(Integer::intValue);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1);
        var slot2 = insert(collector, container, 3); // gap of 2 — two sequences
        assertResultRecursive(collector, container, buildSequenceChain(1, 3));
        slot1.update(2); // 2 and 3 are consecutive — one sequence
        assertResultRecursive(collector, container, buildSequenceChain(2, 3));
        slot1.update(2); // same value → result unchanged
        assertResultRecursive(collector, container, buildSequenceChain(2, 3));
        slot2.remove();
        assertResultRecursive(collector, container, buildSequenceChain(2));
        slot1.remove();
        assertResultRecursive(collector, container, buildSequenceChain());
    }

    @Test
    public void consecutiveUsageUpdate() {
        var collector = ConstraintCollectors.toConnectedRanges(Interval::start, Interval::end, (a, b) -> b - a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, new Interval(1, 3));
        var slot2 = insert(collector, container, new Interval(10, 20)); // disjoint
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3), new Interval(10, 20)));
        var i812 = new Interval(8, 12);
        slot1.update(i812); // now overlaps with (10,20)
        assertResult(collector, container, buildConsecutiveUsage(new Interval(8, 12), new Interval(10, 20)));
        slot1.update(i812); // same value → result unchanged
        assertResult(collector, container, buildConsecutiveUsage(new Interval(8, 12), new Interval(10, 20)));
        slot2.remove();
        assertResult(collector, container, buildConsecutiveUsage(new Interval(8, 12)));
        slot1.remove();
        assertResult(collector, container, buildConsecutiveUsage());
    }

    @Test
    public void toListUpdate() {
        var collector = ConstraintCollectors.<Integer> toList();
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1);
        var slot2 = insert(collector, container, 2);
        assertResult(collector, container, asList(1, 2));
        slot1.update(3);
        assertResult(collector, container, asList(3, 2));
        slot1.update(3); // no-op
        assertResult(collector, container, asList(3, 2));
        slot2.remove();
        assertResult(collector, container, singletonList(3));
        slot1.remove();
        assertResult(collector, container, emptyList());
    }

    @Test
    public void toSetUpdate() {
        var collector = ConstraintCollectors.<Integer> toSet();
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1);
        var slot2 = insert(collector, container, 2);
        assertResult(collector, container, asSet(1, 2));
        slot1.update(3);
        assertResult(collector, container, asSet(2, 3));
        slot1.update(3); // Objects.equals short-circuit
        assertResult(collector, container, asSet(2, 3));
        slot2.remove();
        assertResult(collector, container, singleton(3));
        slot1.remove();
        assertResult(collector, container, emptySet());
    }

    @Test
    public void toSortedSetUpdate() {
        var collector = ConstraintCollectors.<Integer> toSortedSet();
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1);
        var slot2 = insert(collector, container, 2);
        assertResult(collector, container, asSortedSet(1, 2));
        slot1.update(3);
        assertResult(collector, container, asSortedSet(2, 3));
        slot1.update(3); // Objects.equals short-circuit
        assertResult(collector, container, asSortedSet(2, 3));
        slot2.remove();
        assertResult(collector, container, asSortedSet(3));
        slot1.remove();
        assertResult(collector, container, emptySortedSet());
    }

    @Test
    public void toCollectionUpdate() {
        var collector = InnerUniConstraintCollectors.<Integer, Integer, ArrayList<Integer>> toCollection(
                Function.identity(), ArrayList::new);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1);
        var slot2 = insert(collector, container, 2);
        assertResult(collector, container, new ArrayList<>(asList(1, 2)));
        slot1.update(3);
        assertResult(collector, container, new ArrayList<>(asList(3, 2)));
        slot1.update(3); // no-op
        assertResult(collector, container, new ArrayList<>(asList(3, 2)));
        slot2.remove();
        assertResult(collector, container, new ArrayList<>(singletonList(3)));
        slot1.remove();
        assertResult(collector, container, new ArrayList<>());
    }

    @Test
    public void toMapUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer> toMap(
                Function.identity(), Function.identity());
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2);
        var slot2 = insert(collector, container, 1);
        assertResult(collector, container, asMap(2, singleton(2), 1, singleton(1)));
        slot1.update(3);
        assertResult(collector, container, asMap(1, singleton(1), 3, singleton(3)));
        slot1.update(3); // Objects.equals short-circuit on Pair(3,3)
        assertResult(collector, container, asMap(1, singleton(1), 3, singleton(3)));
        slot2.remove();
        assertResult(collector, container, asMap(3, singleton(3)));
        slot1.remove();
        assertResult(collector, container, emptyMap());
    }

    @Test
    public void toMapMergedUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer> toMap(
                Function.identity(), Function.identity(), Integer::sum);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2);
        var slot2 = insert(collector, container, 1);
        assertResult(collector, container, asMap(2, 2, 1, 1));
        slot1.update(3);
        assertResult(collector, container, asMap(1, 1, 3, 3));
        slot1.update(3); // Objects.equals short-circuit on Pair(3,3)
        assertResult(collector, container, asMap(1, 1, 3, 3));
        slot2.remove();
        assertResult(collector, container, asMap(3, 3));
        slot1.remove();
        assertResult(collector, container, emptyMap());
    }

    @Test
    public void toSortedMapUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer> toSortedMap(
                Function.identity(), Function.identity());
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2);
        var slot2 = insert(collector, container, 1);
        assertResult(collector, container, asSortedMap(1, singleton(1), 2, singleton(2)));
        slot1.update(3);
        assertResult(collector, container, asSortedMap(1, singleton(1), 3, singleton(3)));
        slot1.update(3); // Objects.equals short-circuit on Pair(3,3)
        assertResult(collector, container, asSortedMap(1, singleton(1), 3, singleton(3)));
        slot2.remove();
        assertResult(collector, container, asSortedMap(3, singleton(3)));
        slot1.remove();
        assertResult(collector, container, emptySortedMap());
    }

    @Test
    public void minComparableUpdate() {
        UniConstraintCollector<Integer, ?, Integer> collector = min();
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 5);
        var slot2 = insert(collector, container, 3);
        assertResult(collector, container, 3);
        slot2.update(6);
        assertResult(collector, container, 5);
        slot2.update(6); // Objects.equals short-circuit
        assertResult(collector, container, 5);
        slot1.update(1);
        assertResult(collector, container, 1);
        slot2.remove();
        assertResult(collector, container, 1);
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void maxComparableUpdate() {
        UniConstraintCollector<Integer, ?, Integer> collector = max();
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 3);
        var slot2 = insert(collector, container, 5);
        assertResult(collector, container, 5);
        slot2.update(2);
        assertResult(collector, container, 3);
        slot2.update(2); // Objects.equals short-circuit
        assertResult(collector, container, 3);
        slot1.update(7);
        assertResult(collector, container, 7);
        slot2.remove();
        assertResult(collector, container, 7);
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void minNotComparableUpdate() {
        var collector = min(Function.identity(), o -> (String) o);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, (Object) "b");
        var slot2 = insert(collector, container, (Object) "a");
        assertResult(collector, container, "a");
        slot2.update("c");
        assertResult(collector, container, "b");
        slot2.update("c"); // Objects.equals short-circuit
        assertResult(collector, container, "b");
        slot1.update("a");
        assertResult(collector, container, "a");
        slot2.remove();
        assertResult(collector, container, "a");
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void maxNotComparableUpdate() {
        UniConstraintCollector<String, ?, String> collector = max(Function.identity(), o -> o);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, "b");
        var slot2 = insert(collector, container, "a");
        assertResult(collector, container, "b");
        slot2.update("c");
        assertResult(collector, container, "c");
        slot2.update("c"); // Objects.equals short-circuit
        assertResult(collector, container, "c");
        slot1.update("a");
        assertResult(collector, container, "c");
        slot2.remove();
        assertResult(collector, container, "a");
        slot1.remove();
        assertResult(collector, container, null);
    }

}
