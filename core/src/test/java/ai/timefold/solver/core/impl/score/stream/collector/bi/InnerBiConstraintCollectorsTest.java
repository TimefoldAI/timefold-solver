package ai.timefold.solver.core.impl.score.stream.collector.bi;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.compose;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.countBi;
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
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractConstraintCollectorsTest;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

final class InnerBiConstraintCollectorsTest extends AbstractConstraintCollectorsTest {

    @Override
    @Test
    public void count() {
        BiConstraintCollector<Integer, Integer, ?, Long> collector = countBi();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, 1L);
        // Add second value, we have two now.
        int secondValueA = 1;
        int secondValueB = 2;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, 2L);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
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
        BiConstraintCollector<Integer, Integer, ?, Long> collector = ConstraintCollectors.countDistinct(Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, 1L);
        // Add second value, we have two now.
        int secondValueA = 1;
        int secondValueB = 2;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, 2L);
        // Add third value, same as the second. We now have two distinct values.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, 2L);
        // Retract one instance of the second value; we still only have two distinct values.
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
        BiConstraintCollector<Integer, Integer, ?, Long> collector = ConstraintCollectors.sum(Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, 5L);
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, 14L);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, 23L);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 14L);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 5L);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0L);
    }

    @Override
    @Test
    public void sumBigDecimal() {
        BiConstraintCollector<Integer, Integer, ?, BigDecimal> collector = ConstraintCollectors
                .sumBigDecimal((a, b) -> BigDecimal.valueOf(a + b));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, BigDecimal.ZERO);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, BigDecimal.valueOf(5));
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, BigDecimal.valueOf(14));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, BigDecimal.valueOf(23));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, BigDecimal.valueOf(14));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, BigDecimal.valueOf(5));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, BigDecimal.ZERO);
    }

    @Override
    @Test
    public void sumBigInteger() {
        BiConstraintCollector<Integer, Integer, ?, BigInteger> collector = ConstraintCollectors
                .sumBigInteger((a, b) -> BigInteger.valueOf(a + b));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, BigInteger.ZERO);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, BigInteger.valueOf(5));
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, BigInteger.valueOf(14));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, BigInteger.valueOf(23));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, BigInteger.valueOf(14));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, BigInteger.valueOf(5));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, BigInteger.ZERO);
    }

    @Override
    @Test
    public void sumDuration() {
        BiConstraintCollector<Integer, Integer, ?, Duration> collector = ConstraintCollectors
                .sumDuration((a, b) -> Duration.ofSeconds(a + b));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Duration.ZERO);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, Duration.ofSeconds(5));
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, Duration.ofSeconds(14));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, Duration.ofSeconds(23));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, Duration.ofSeconds(14));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, Duration.ofSeconds(5));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, Duration.ZERO);
    }

    @Override
    @Test
    public void sumPeriod() {
        BiConstraintCollector<Integer, Integer, ?, Period> collector = ConstraintCollectors
                .sumPeriod((a, b) -> Period.ofDays(a + b));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Period.ZERO);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, Period.ofDays(5));
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, Period.ofDays(14));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, Period.ofDays(23));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, Period.ofDays(14));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, Period.ofDays(5));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, Period.ZERO);
    }

    @Override
    @Test
    public void minComparable() {
        /*
         * LocalDateTime is chosen because it doesn't implement Comparable<LocalDateTime>.
         * Rather it implements Comparable<? super LocalDateTime>,
         * exercising the PECS principle in our generics
         * in a way that Integer would not.
         */
        var baseLocalDateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        var collector = min((Integer a, Integer b) -> baseLocalDateTime.plusMinutes(a + b));
        var container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the min
        int firstValueA = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(2));
        // add second value, lesser than the first, becomes the new min
        int secondValueA = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(1));
        // add third value, same as the second, result does not change
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(1));
        // retract one instance of the second value; second value is still the min value, nothing should change
        secondRetractor.run();
        assertResult(collector, container, baseLocalDateTime.plusMinutes(1));
        // retract final instance of the second value; first value is now the min value
        thirdRetractor.run();
        assertResult(collector, container, baseLocalDateTime.plusMinutes(2));
        // retract last value; there are no values now
        firstRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void minNotComparable() {
        BiConstraintCollector<String, String, ?, String> collector = min((a, b) -> a, o -> o);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the min
        String firstValue = "b";
        Runnable firstRetractor = accumulate(collector, container, firstValue, null);
        assertResult(collector, container, firstValue);
        // add second value, lesser than the first, becomes the new min
        String secondValue = "a";
        Runnable secondRetractor = accumulate(collector, container, secondValue, null);
        assertResult(collector, container, secondValue);
        // add third value, same as the second, result does not change
        Runnable thirdRetractor = accumulate(collector, container, secondValue, null);
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
        /*
         * LocalDateTime is chosen because it doesn't implement Comparable<LocalDateTime>.
         * Rather it implements Comparable<? super LocalDateTime>,
         * exercising the PECS principle in our generics
         * in a way that Integer would not.
         */
        var baseLocalDateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        var collector = max((Integer a, Integer b) -> baseLocalDateTime.plusMinutes(a + b));
        var container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the max
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(2));
        // add second value, lesser than the first, result does not change
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(2));
        // add third value, same as the first, result does not change
        Runnable thirdRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(2));
        // retract one instance of the first value; first value is still the max value, nothing should change
        firstRetractor.run();
        assertResult(collector, container, baseLocalDateTime.plusMinutes(2));
        // retract final instance of the first value; second value is now the max value
        thirdRetractor.run();
        assertResult(collector, container, baseLocalDateTime.plusMinutes(1));
        // retract last value; there are no values now
        secondRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void maxNotComparable() {
        BiConstraintCollector<String, String, ?, String> collector = max((a, b) -> a, o -> o);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the max
        String firstValue = "b";
        Runnable firstRetractor = accumulate(collector, container, firstValue, null);
        assertResult(collector, container, firstValue);
        // add second value, lesser than the first, result does not change
        String secondValue = "a";
        Runnable secondRetractor = accumulate(collector, container, secondValue, null);
        assertResult(collector, container, firstValue);
        // add third value, same as the first, result does not change
        Runnable thirdRetractor = accumulate(collector, container, firstValue, null);
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
        BiConstraintCollector<Integer, Integer, ?, Double> collector = ConstraintCollectors.average(Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, 4.0D);
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, 2.5D);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
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
        BiConstraintCollector<Integer, Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigDecimal((i, i2) -> BigDecimal.valueOf(i + i2));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, BigDecimal.valueOf(4));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, BigDecimal.valueOf(2)); // Scale = 0.
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
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
        BiConstraintCollector<Integer, Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigInteger((i, i2) -> BigInteger.valueOf(i + i2));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, BigDecimal.valueOf(4));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, BigDecimal.valueOf(2)); // Scale = 0.
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
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
        BiConstraintCollector<Integer, Integer, ?, Duration> collector =
                ConstraintCollectors.averageDuration((i, i2) -> Duration.ofSeconds(i + i2));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, Duration.ofSeconds(4));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, Duration.ofMillis(2500));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
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
        BiConstraintCollector<Integer, Integer, ?, Set<Integer>> collector = ConstraintCollectors.toSet(Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySet());
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, singleton(3));
        // Add second value, we have two now.
        int secondValueA = 3;
        int secondValueB = 4;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, asSet(3, 7));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, asSet(3, 7));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSet(3, 7));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, singleton(3));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySet());
    }

    @Override
    @Test
    public void toSortedSet() {
        BiConstraintCollector<Integer, Integer, ?, SortedSet<Integer>> collector =
                ConstraintCollectors.toSortedSet((BiFunction<Integer, Integer, Integer>) Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedSet());
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, asSortedSet(3));
        // Add second value, we have two now.
        int secondValueA = 3;
        int secondValueB = 4;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, asSortedSet(3, 7));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, asSortedSet(3, 7));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSortedSet(3, 7));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asSortedSet(3));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySortedSet());
    }

    @Override
    @Test
    public void toList() {
        BiConstraintCollector<Integer, Integer, ?, List<Integer>> collector = ConstraintCollectors.toList(Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyList());
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB);
        assertResult(collector, container, singletonList(3));
        // Add second value, we have two now.
        int secondValueA = 3;
        int secondValueB = 4;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, asList(3, 7));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB);
        assertResult(collector, container, asList(3, 7, 7));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asList(3, 7));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, singletonList(3));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptyList());
    }

    @Override
    @Test
    public void toMap() {
        BiConstraintCollector<Integer, Integer, ?, Map<Integer, Set<Integer>>> collector = ConstraintCollectors
                .toMap(Integer::sum, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, asMap(2, singleton(2)));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, asMap(2, singleton(2), 1, singleton(1)));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
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

    @Test
    public void toMapDuplicates() {
        BiConstraintCollector<String, Integer, ?, Map<String, Set<Integer>>> collector =
                ConstraintCollectors.toMap((a, b) -> a, (a, b) -> b);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyMap());
        // Add first value, we have one now.
        String firstValue = "A";
        Runnable firstRetractor = accumulate(collector, container, firstValue, 1);
        assertResult(collector, container, asMap("A", singleton(1)));
        // Add second value, we have two now.
        String secondValue = "B";
        Runnable secondRetractor = accumulate(collector, container, secondValue, 1);
        assertResult(collector, container, asMap("A", singleton(1), "B", singleton(1)));
        // Add third value, different from the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 2);
        assertResult(collector, container, asMap("A", singleton(1), "B", asSet(1, 2)));
        // Retract one instance from the second key; we only have one there now, but still two keys.
        secondRetractor.run();
        assertResult(collector, container, asMap("A", singleton(1), "B", singleton(2)));
        // Retract final instance of the second value; we only have one key now.
        thirdRetractor.run();
        assertResult(collector, container, asMap("A", singleton(1)));
        // Retract last value; there are no keys now.
        firstRetractor.run();
        assertResult(collector, container, emptyMap());
    }

    @Override
    @Test
    public void toMapMerged() {
        BiConstraintCollector<Integer, Integer, ?, Map<Integer, Integer>> collector =
                ConstraintCollectors.toMap((a, b) -> a, (a, b) -> b, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 1);
        assertResult(collector, container, asMap(2, 1));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 1);
        assertResult(collector, container, asMap(2, 1, 1, 1));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 2);
        assertResult(collector, container, asMap(2, 1, 1, 3));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asMap(2, 1, 1, 2));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asMap(2, 1));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptyMap());
    }

    @Override
    @Test
    public void toSortedMap() {
        BiConstraintCollector<Integer, Integer, ?, SortedMap<Integer, Set<Integer>>> collector = ConstraintCollectors
                .toSortedMap(Integer::sum, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, asSortedMap(2, singleton(2)));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, asSortedMap(2, singleton(2), 1, singleton(1)));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
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
        BiConstraintCollector<Integer, Integer, ?, SortedMap<Integer, Integer>> collector = ConstraintCollectors
                .toSortedMap((a, b) -> a, (a, b) -> b, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 1);
        assertResult(collector, container, asSortedMap(2, 1));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 1);
        assertResult(collector, container, asSortedMap(2, 1, 1, 1));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 2);
        assertResult(collector, container, asSortedMap(2, 1, 1, 3));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSortedMap(2, 1, 1, 2));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asSortedMap(2, 1));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySortedMap());
    }

    @Override
    @Test
    public void conditionally() {
        BiConstraintCollector<Integer, Integer, Object, Integer> collector = ConstraintCollectors.conditionally(
                (i, i2) -> i < 2,
                max(Integer::sum, i -> i));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, 1);
        // Add second value; it is skipped even though it is greater than the first value.
        int secondValue = 2;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, 1);
        // Add third value, same as the first. We now have two values, both of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, 1);
        // Retract one instance of the first value; we only have one value now.
        firstRetractor.run();
        assertResult(collector, container, 1);
        // Retract the skipped value.
        secondRetractor.run();
        assertResult(collector, container, 1);
        // Retract last value; there are no values now.
        thirdRetractor.run();
        assertResult(collector, container, null);
    }

    @Override
    @Test
    public void compose2() {
        BiConstraintCollector<Integer, Integer, ?, Pair<Integer, Integer>> collector =
                compose(min(Integer::sum, i -> i),
                        max(Integer::sum, i -> i),
                        Pair::new);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, new Pair<>(null, null));
        // Add first value.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, new Pair<>(2, 2));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, new Pair<>(1, 2));
        // Add third value, same as the second, result does not change.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
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
        BiConstraintCollector<Integer, Integer, ?, Triple<Integer, Integer, Double>> collector =
                compose(min(Integer::sum, i -> i),
                        max(Integer::sum, i -> i),
                        ConstraintCollectors.average(Integer::sum),
                        Triple::of);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Triple.of(null, null, null));
        // Add first value.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, Triple.of(4, 4, 4D));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, Triple.of(1, 4, 2.5D));
        // Add third value, same as the second.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
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
        BiConstraintCollector<Integer, Integer, ?, Quadruple<Long, Integer, Integer, Double>> collector =
                compose(countBi(),
                        min(Integer::sum, i -> i),
                        max(Integer::sum, i -> i),
                        ConstraintCollectors.average(Integer::sum),
                        Quadruple::new);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, new Quadruple<>(0L, null, null, null));
        // Add first value.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, new Quadruple<>(1L, 4, 4, 4D));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, new Quadruple<>(2L, 1, 4, 2.5D));
        // Add third value, same as the second.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, new Quadruple<>(3L, 1, 4, 2D));
        // Retract one instance of the second value.
        secondRetractor.run();
        assertResult(collector, container, new Quadruple<>(2L, 1, 4, 2.5D));
        // Retract final instance of the second value.
        thirdRetractor.run();
        assertResult(collector, container, new Quadruple<>(1L, 4, 4, 4D));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, new Quadruple<>(0L, null, null, null));
    }

    @Override
    @Test
    public void toConsecutiveSequences() {
        // Do a basic test w/o edge cases; edge cases are covered in ConsecutiveSetTreeTest
        var collector = ConstraintCollectors.toConsecutiveSequences(Integer::sum, Integer::intValue);
        var container = collector.supplier().get();
        // Add first value, sequence is [2]
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResultRecursive(collector, container, buildSequenceChain(2));
        // Add second value, sequence is [1,2]
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResultRecursive(collector, container, buildSequenceChain(1, 2));
        // Add third value, same as the second. Sequence is [{1,1},2}]
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0);
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
                ConstraintCollectors.toConnectedRanges(Interval::new,
                        Interval::start,
                        Interval::end, (a, b) -> b - a);
        var container = collector.supplier().get();
        // Add first value, sequence is [(1,3)]
        var firstRetractor = accumulate(collector, container, 1, 3);
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3)));
        // Add second value, sequence is [(1,3),(2,4)]
        var secondRetractor = accumulate(collector, container, 2, 4);
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3), new Interval(2, 4)));
        // Add third value, same as the second. Sequence is [{1,1},2}]
        var thirdRetractor = accumulate(collector, container, 2, 4);
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
                ConstraintCollectors.toConnectedRanges((DynamicInterval a, Object b) -> a,
                        DynamicInterval::getStart,
                        DynamicInterval::getEnd, (a, b) -> b - a);

        var first = new DynamicInterval(0);
        var second = new DynamicInterval(10);
        var third = new DynamicInterval(20);
        var container = dynamicCollector.supplier().get();

        // Add first value, sequence is [[(0, 10)]]
        var firstRetractor = accumulate(dynamicCollector, container, first, null);
        assertResult(dynamicCollector, container, buildDynamicConsecutiveUsage(new DynamicInterval(0)));

        // Add third value, sequence is [[(0, 10)], [(20, 30)]]
        accumulate(dynamicCollector, container, third, null);
        assertResult(dynamicCollector, container,
                buildDynamicConsecutiveUsage(new DynamicInterval(0), new DynamicInterval(20)));

        // Add second value, sequence is [[(0, 10), (10, 20), (20, 30)]]
        accumulate(dynamicCollector, container, second, null);
        assertResult(dynamicCollector, container,
                buildDynamicConsecutiveUsage(new DynamicInterval(0), new DynamicInterval(10), new DynamicInterval(20)));

        // Change first value, retract it, then re-add it
        first.setStart(-5);
        firstRetractor.run();
        accumulate(dynamicCollector, container, first, null);

        assertResult(dynamicCollector, container,
                buildDynamicConsecutiveUsage(new DynamicInterval(-5), new DynamicInterval(10), new DynamicInterval(20)));
    }

    @Override
    @Test
    public void loadBalance() {
        var collector = ConstraintCollectors.<String, Integer, String> loadBalance((a, b) -> a, (a, b) -> b);
        var container = collector.supplier().get();

        // Default state.
        assertUnfairness(collector, container, BigDecimal.ZERO);
        // Add first value, we have one now.
        var firstRetractor = accumulate(collector, container, "A", 2);
        assertUnfairness(collector, container, BigDecimal.ZERO);
        // Add second value, we have two now.
        var secondRetractor = accumulate(collector, container, "B", 1);
        // sqrt((3-2.5)^2 + (2 - 2.5)^2) = sqrt(0.5^2 + 0.5^2) = sqrt(0.25 + 0.25) = sqrt(0.5)
        assertUnfairness(collector, container, BigDecimal.valueOf(0.707107));
        // Add third value, same as the second. We now have three values, two of which are the same.
        var thirdRetractor = accumulate(collector, container, "B", 1);
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

    @Override
    @Test
    public void collectAndThen() {
        var collector = ConstraintCollectors.collectAndThen(countBi(), i -> i * 10);
        var container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        var firstValue = 2;
        var firstRetractor = accumulate(collector, container, firstValue, 0);
        assertResult(collector, container, 10L);
        // Add second value, we have two now.
        var secondValue = 1;
        var secondRetractor = accumulate(collector, container, secondValue, 0);
        assertResult(collector, container, 20L);
        // Add third value, same as the second. We now have three values, two of which are the same.
        var thirdRetractor = accumulate(collector, container, secondValue, 0);
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

    private static <A, B, Container_, Result_> Runnable accumulate(
            BiConstraintCollector<A, B, Container_, Result_> collector, Object container, A valueA, B valueB) {
        return collector.accumulator().apply((Container_) container, valueA, valueB);
    }

    private static <A, B, Container_, Result_> BiConstraintCollectorAccumulatedValue<A, B> insert(
            BiConstraintCollector<A, B, Container_, Result_> collector, Object container, A a, B b) {
        var slot = collector.incrementalAccumulator().intoGroup((Container_) container);
        slot.add(a, b);
        return slot;
    }

    @Test
    public void countUpdate() {
        BiConstraintCollector<Integer, Integer, ?, Long> collector = countBi();
        Object container = collector.supplier().get();
        var slot = insert(collector, container, 1, 0);
        assertResult(collector, container, 1L);
        slot.update(42, 0); // no-op for count
        assertResult(collector, container, 1L);
        slot.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void conditionallyUpdate() {
        BiConstraintCollector<Integer, Integer, Object, Integer> collector =
                ConstraintCollectors.conditionally((Integer a, Integer b) -> a < 2,
                        min(Integer::sum, i -> i));
        Object container = collector.supplier().get();
        var slot = insert(collector, container, 1, 0); // active (1 < 2)
        assertResult(collector, container, 1);
        slot.update(2, 0); // active → inactive (2 is not < 2)
        assertResult(collector, container, null);
        slot.update(0, 0); // inactive → active (0 < 2)
        assertResult(collector, container, 0);
        slot.update(3, 0); // active → inactive
        assertResult(collector, container, null);
        slot.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void compose2Update() {
        BiConstraintCollector<Integer, Integer, ?, Pair<Integer, Integer>> collector =
                compose(min(Integer::sum, i -> i),
                        max(Integer::sum, i -> i),
                        (BiFunction<Integer, Integer, Pair<Integer, Integer>>) Pair::new);
        Object container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0);
        var slot2 = insert(collector, container, 4, 0);
        assertResult(collector, container, new Pair<>(2, 4));
        slot1.update(3, 0); // 2 → 3; min becomes 3
        assertResult(collector, container, new Pair<>(3, 4));
        slot2.remove();
        slot1.remove();
        assertResult(collector, container, new Pair<>(null, null));
    }

    @Test
    public void collectAndThenUpdate() {
        var collector = ConstraintCollectors.collectAndThen(countBi(), i -> i * 10);
        var container = collector.supplier().get();
        var slot = insert(collector, container, 1, 0);
        assertResult(collector, container, 10L);
        slot.update(99, 0); // count no-op; result unchanged
        assertResult(collector, container, 10L);
        slot.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void sumUpdate() {
        BiConstraintCollector<Integer, Integer, ?, Long> collector = ConstraintCollectors.sum(Integer::sum);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0);
        assertResult(collector, container, 2L);
        slot1.update(5, 0);
        assertResult(collector, container, 5L);
        slot1.update(5, 0); // no-op
        assertResult(collector, container, 5L);
        var slot2 = insert(collector, container, 3, 0);
        assertResult(collector, container, 8L);
        slot1.update(1, 0);
        assertResult(collector, container, 4L);
        slot2.remove();
        assertResult(collector, container, 1L);
        slot1.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void averageUpdate() {
        BiConstraintCollector<Integer, Integer, ?, Double> collector = ConstraintCollectors.average(Integer::sum);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4, 0);
        var slot2 = insert(collector, container, 2, 0);
        assertResult(collector, container, 3.0D);
        slot1.update(6, 0); // (6+2)/2 = 4.0; count unchanged
        assertResult(collector, container, 4.0D);
        slot1.update(6, 0); // no-op
        assertResult(collector, container, 4.0D);
        slot2.remove();
        assertResult(collector, container, 6.0D);
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void countDistinctUpdate() {
        BiConstraintCollector<String, String, ?, Long> collector = ConstraintCollectors.countDistinct((a, b) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, "a", null);
        var slot2 = insert(collector, container, "b", null);
        assertResult(collector, container, 2L);
        slot1.update("b", null); // both map to "b"
        assertResult(collector, container, 1L);
        slot1.update("b", null); // no-op: Objects.equals short-circuit
        assertResult(collector, container, 1L);
        slot1.update("c", null); // "b" and "c"
        assertResult(collector, container, 2L);
        slot2.remove();
        assertResult(collector, container, 1L);
        slot1.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void sumBigDecimalUpdate() {
        BiConstraintCollector<BigDecimal, BigDecimal, ?, BigDecimal> collector =
                ConstraintCollectors.sumBigDecimal((a, b) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, BigDecimal.ONE, null);
        var slot2 = insert(collector, container, BigDecimal.TEN, null);
        assertResult(collector, container, BigDecimal.valueOf(11));
        var bd4 = BigDecimal.valueOf(4);
        slot1.update(bd4, null);
        assertResult(collector, container, BigDecimal.valueOf(14));
        slot1.update(bd4, null); // no-op: same reference, == short-circuit
        assertResult(collector, container, BigDecimal.valueOf(14));
        slot2.remove();
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot1.remove();
        assertResult(collector, container, BigDecimal.ZERO);
    }

    @Test
    public void sumBigIntegerUpdate() {
        BiConstraintCollector<BigInteger, BigInteger, ?, BigInteger> collector =
                ConstraintCollectors.sumBigInteger((a, b) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, BigInteger.ONE, null);
        var slot2 = insert(collector, container, BigInteger.TEN, null);
        assertResult(collector, container, BigInteger.valueOf(11));
        var bi4 = BigInteger.valueOf(4);
        slot1.update(bi4, null);
        assertResult(collector, container, BigInteger.valueOf(14));
        slot1.update(bi4, null); // no-op: same reference, == short-circuit
        assertResult(collector, container, BigInteger.valueOf(14));
        slot2.remove();
        assertResult(collector, container, BigInteger.valueOf(4));
        slot1.remove();
        assertResult(collector, container, BigInteger.ZERO);
    }

    @Test
    public void sumDurationUpdate() {
        BiConstraintCollector<Duration, Duration, ?, Duration> collector =
                ConstraintCollectors.sumDuration((a, b) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, Duration.ofSeconds(1), null);
        var slot2 = insert(collector, container, Duration.ofSeconds(2), null);
        assertResult(collector, container, Duration.ofSeconds(3));
        var d4 = Duration.ofSeconds(4);
        slot1.update(d4, null);
        assertResult(collector, container, Duration.ofSeconds(6));
        slot1.update(d4, null); // no-op: same reference, == short-circuit
        assertResult(collector, container, Duration.ofSeconds(6));
        slot2.remove();
        assertResult(collector, container, Duration.ofSeconds(4));
        slot1.remove();
        assertResult(collector, container, Duration.ZERO);
    }

    @Test
    public void sumPeriodUpdate() {
        BiConstraintCollector<Period, Period, ?, Period> collector =
                ConstraintCollectors.sumPeriod((a, b) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, Period.ofDays(1), null);
        var slot2 = insert(collector, container, Period.ofDays(2), null);
        assertResult(collector, container, Period.ofDays(3));
        var p4 = Period.ofDays(4);
        slot1.update(p4, null);
        assertResult(collector, container, Period.ofDays(6));
        slot1.update(p4, null); // no-op: same reference, == short-circuit
        assertResult(collector, container, Period.ofDays(6));
        slot2.remove();
        assertResult(collector, container, Period.ofDays(4));
        slot1.remove();
        assertResult(collector, container, Period.ZERO);
    }

    @Test
    public void averageBigDecimalUpdate() {
        BiConstraintCollector<Integer, Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigDecimal((a, b) -> BigDecimal.valueOf(a + b));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4, 0);
        var slot2 = insert(collector, container, 2, 0);
        assertResult(collector, container, BigDecimal.valueOf(3));
        slot1.update(6, 0); // (6+2)/2 = 4; count unchanged
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot1.update(6, 0); // no-op: same input value
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot2.remove();
        assertResult(collector, container, BigDecimal.valueOf(6));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void averageBigIntegerUpdate() {
        BiConstraintCollector<Integer, Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigInteger((a, b) -> BigInteger.valueOf(a + b));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4, 0);
        var slot2 = insert(collector, container, 2, 0);
        assertResult(collector, container, BigDecimal.valueOf(3));
        slot1.update(6, 0);
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot1.update(6, 0); // no-op: same input value
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot2.remove();
        assertResult(collector, container, BigDecimal.valueOf(6));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void averageDurationUpdate() {
        BiConstraintCollector<Integer, Integer, ?, Duration> collector =
                ConstraintCollectors.averageDuration((a, b) -> Duration.ofSeconds(a + b));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4, 0);
        var slot2 = insert(collector, container, 2, 0);
        assertResult(collector, container, Duration.ofSeconds(3));
        slot1.update(6, 0); // (6+2)/2 = 4; count unchanged
        assertResult(collector, container, Duration.ofSeconds(4));
        slot1.update(6, 0); // no-op: same input value
        assertResult(collector, container, Duration.ofSeconds(4));
        slot2.remove();
        assertResult(collector, container, Duration.ofSeconds(6));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void toConsecutiveSequencesUpdate() {
        var collector = ConstraintCollectors.toConsecutiveSequences(Integer::sum, Integer::intValue);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0);
        var slot2 = insert(collector, container, 3, 0); // gap of 2 — two sequences
        assertResultRecursive(collector, container, buildSequenceChain(1, 3));
        slot1.update(2, 0); // 2 and 3 are consecutive — one sequence
        assertResultRecursive(collector, container, buildSequenceChain(2, 3));
        slot1.update(2, 0); // same value → result unchanged
        assertResultRecursive(collector, container, buildSequenceChain(2, 3));
        slot2.remove();
        assertResultRecursive(collector, container, buildSequenceChain(2));
        slot1.remove();
        assertResultRecursive(collector, container, buildSequenceChain());
    }

    @Test
    public void consecutiveUsageUpdate() {
        var collector = ConstraintCollectors.toConnectedRanges(Interval::new,
                Interval::start,
                Interval::end, (a, b) -> b - a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 3);
        var slot2 = insert(collector, container, 10, 20); // disjoint
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3), new Interval(10, 20)));
        var i812 = new Interval(8, 12);
        slot1.update(8, 12); // now overlaps with (10,20)
        assertResult(collector, container, buildConsecutiveUsage(new Interval(8, 12), new Interval(10, 20)));
        slot1.update(8, 12); // same value → result unchanged
        assertResult(collector, container, buildConsecutiveUsage(new Interval(8, 12), new Interval(10, 20)));
        slot2.remove();
        assertResult(collector, container, buildConsecutiveUsage(i812));
        slot1.remove();
        assertResult(collector, container, buildConsecutiveUsage());
    }

    @Test
    public void toListUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer> toList((a, b) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0);
        var slot2 = insert(collector, container, 2, 0);
        assertResult(collector, container, asList(1, 2));
        slot1.update(3, 0);
        assertResult(collector, container, asList(3, 2));
        slot1.update(3, 0); // no-op
        assertResult(collector, container, asList(3, 2));
        slot2.remove();
        assertResult(collector, container, singletonList(3));
        slot1.remove();
        assertResult(collector, container, emptyList());
    }

    @Test
    public void toSetUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer> toSet((a, b) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0);
        var slot2 = insert(collector, container, 2, 0);
        assertResult(collector, container, asSet(1, 2));
        slot1.update(3, 0);
        assertResult(collector, container, asSet(2, 3));
        slot1.update(3, 0); // Objects.equals short-circuit
        assertResult(collector, container, asSet(2, 3));
        slot2.remove();
        assertResult(collector, container, asSet(3));
        slot1.remove();
        assertResult(collector, container, emptySet());
    }

    @Test
    public void toSortedSetUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer> toSortedSet((a, b) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0);
        var slot2 = insert(collector, container, 2, 0);
        assertResult(collector, container, asSortedSet(1, 2));
        slot1.update(3, 0);
        assertResult(collector, container, asSortedSet(2, 3));
        slot1.update(3, 0); // Objects.equals short-circuit
        assertResult(collector, container, asSortedSet(2, 3));
        slot2.remove();
        assertResult(collector, container, asSortedSet(3));
        slot1.remove();
        assertResult(collector, container, emptySortedSet());
    }

    @Test
    public void toCollectionUpdate() {
        var collector = InnerBiConstraintCollectors.<Integer, Integer, Integer, ArrayList<Integer>> toCollection(
                (a, b) -> a, ArrayList::new);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0);
        var slot2 = insert(collector, container, 2, 0);
        assertResult(collector, container, new ArrayList<>(asList(1, 2)));
        slot1.update(3, 0);
        assertResult(collector, container, new ArrayList<>(asList(3, 2)));
        slot1.update(3, 0); // no-op
        assertResult(collector, container, new ArrayList<>(asList(3, 2)));
        slot2.remove();
        assertResult(collector, container, new ArrayList<>(singletonList(3)));
        slot1.remove();
        assertResult(collector, container, new ArrayList<>());
    }

    @Test
    public void toMapUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer, Integer> toMap(
                Integer::sum, Integer::sum);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0);
        var slot2 = insert(collector, container, 1, 0);
        assertResult(collector, container, asMap(2, singleton(2), 1, singleton(1)));
        slot1.update(3, 0);
        assertResult(collector, container, asMap(1, singleton(1), 3, singleton(3)));
        slot1.update(3, 0); // Objects.equals short-circuit on Pair(3,3)
        assertResult(collector, container, asMap(1, singleton(1), 3, singleton(3)));
        slot2.remove();
        assertResult(collector, container, asMap(3, singleton(3)));
        slot1.remove();
        assertResult(collector, container, emptyMap());
    }

    @Test
    public void toMapMergedUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer, Integer> toMap(
                Integer::sum, Integer::sum, Integer::sum);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0);
        var slot2 = insert(collector, container, 1, 0);
        assertResult(collector, container, asMap(2, 2, 1, 1));
        slot1.update(3, 0);
        assertResult(collector, container, asMap(1, 1, 3, 3));
        slot1.update(3, 0); // Objects.equals short-circuit on Pair(3,3)
        assertResult(collector, container, asMap(1, 1, 3, 3));
        slot2.remove();
        assertResult(collector, container, asMap(3, 3));
        slot1.remove();
        assertResult(collector, container, emptyMap());
    }

    @Test
    public void toSortedMapUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer, Integer> toSortedMap(
                Integer::sum, Integer::sum);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0);
        var slot2 = insert(collector, container, 1, 0);
        assertResult(collector, container, asSortedMap(1, singleton(1), 2, singleton(2)));
        slot1.update(3, 0);
        assertResult(collector, container, asSortedMap(1, singleton(1), 3, singleton(3)));
        slot1.update(3, 0); // Objects.equals short-circuit on Pair(3,3)
        assertResult(collector, container, asSortedMap(1, singleton(1), 3, singleton(3)));
        slot2.remove();
        assertResult(collector, container, asSortedMap(3, singleton(3)));
        slot1.remove();
        assertResult(collector, container, emptySortedMap());
    }

    @Test
    public void minComparableUpdate() {
        var baseLocalDateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        var collector = min((Integer a, Integer b) -> baseLocalDateTime.plusMinutes(a + b));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 5, 0);
        var slot2 = insert(collector, container, 3, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(3));
        slot2.update(6, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(5));
        slot2.update(6, 0); // Objects.equals short-circuit
        assertResult(collector, container, baseLocalDateTime.plusMinutes(5));
        slot1.update(1, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(1));
        slot2.remove();
        assertResult(collector, container, baseLocalDateTime.plusMinutes(1));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void maxComparableUpdate() {
        var baseLocalDateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        var collector = max((Integer a, Integer b) -> baseLocalDateTime.plusMinutes(a + b));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 3, 0);
        var slot2 = insert(collector, container, 5, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(5));
        slot2.update(2, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(3));
        slot2.update(2, 0); // Objects.equals short-circuit
        assertResult(collector, container, baseLocalDateTime.plusMinutes(3));
        slot1.update(7, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(7));
        slot2.remove();
        assertResult(collector, container, baseLocalDateTime.plusMinutes(7));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void minNotComparableUpdate() {
        var collector = min((String a, String b) -> a, o -> o);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, "b", null);
        var slot2 = insert(collector, container, "a", null);
        assertResult(collector, container, "a");
        slot2.update("c", null);
        assertResult(collector, container, "b");
        slot2.update("c", null); // Objects.equals short-circuit
        assertResult(collector, container, "b");
        slot1.update("a", null);
        assertResult(collector, container, "a");
        slot2.remove();
        assertResult(collector, container, "a");
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void maxNotComparableUpdate() {
        var collector = max((String a, String b) -> a, o -> o);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, "b", null);
        var slot2 = insert(collector, container, "a", null);
        assertResult(collector, container, "b");
        slot2.update("c", null);
        assertResult(collector, container, "c");
        slot2.update("c", null); // Objects.equals short-circuit
        assertResult(collector, container, "c");
        slot1.update("a", null);
        assertResult(collector, container, "c");
        slot2.remove();
        assertResult(collector, container, "a");
        slot1.remove();
        assertResult(collector, container, null);
    }

    private static <A, B, Container_, Result_> void assertResult(
            BiConstraintCollector<A, B, Container_, Result_> collector, Object container, Result_ expectedResult) {
        Result_ actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult)
                .as("Collector (" + collector + ") did not produce expected result.")
                .isEqualTo(expectedResult);
    }

    private static <A, B, Container_, Result_> void assertResultRecursive(
            BiConstraintCollector<A, B, Container_, Result_> collector,
            Object container, Result_ expectedResult) {
        var actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult)
                .as("Collector (" + collector + ") did not produce expected result.")
                .usingRecursiveComparison()
                .ignoringFields("sourceTree", "indexFunction", "sequenceList", "startItemToSequence")
                .isEqualTo(expectedResult);
    }

    private static <Container_> void assertUnfairness(
            BiConstraintCollector<String, Integer, Container_, LoadBalance<String>> collector, Object container,
            BigDecimal expectedValue) {
        LoadBalance<String> actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult.unfairness())
                .as("Collector (" + collector + ") did not produce expected result.")
                .isEqualTo(expectedValue);
    }

}
