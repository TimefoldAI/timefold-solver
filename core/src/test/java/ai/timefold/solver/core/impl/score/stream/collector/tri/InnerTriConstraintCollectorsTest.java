package ai.timefold.solver.core.impl.score.stream.collector.tri;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.compose;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.countTri;
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

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractConstraintCollectorsTest;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

final class InnerTriConstraintCollectorsTest extends AbstractConstraintCollectorsTest {

    @Override
    @Test
    public void count() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Long> collector = countTri();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 1;
        int firstValueC = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, 1L);
        // Add second value, we have two now.
        int secondValueA = 1;
        int secondValueB = 2;
        int secondValueC = 3;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, 2L);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, Long> collector = ConstraintCollectors
                .countDistinct((a, b, c) -> a + b + c);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        int firstValueC = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, 1L);
        // Add second value, we have two now.
        int secondValueA = 1;
        int secondValueB = 2;
        int secondValueC = 3;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, 2L);
        // Add third value, same as the second. We now have two distinct values.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, Long> collector = ConstraintCollectors.sum((a, b, c) -> a + b + c);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        int firstValueC = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, 6L);
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        int secondValueC = 1;
        int secondSum = secondValueA + secondValueB + secondValueC;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, 16L);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, 26L);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 16L);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 6L);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0L);
    }

    @Override
    @Test
    public void sumBigDecimal() {
        TriConstraintCollector<Integer, Integer, Integer, ?, BigDecimal> collector = ConstraintCollectors
                .sumBigDecimal((a, b, c) -> BigDecimal.valueOf(a + b + c));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, BigDecimal.ZERO);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        int firstValueC = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, BigDecimal.valueOf(6));
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        int secondValueC = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, BigDecimal.valueOf(16));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, BigDecimal.valueOf(26));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, BigDecimal.valueOf(16));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, BigDecimal.valueOf(6));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, BigDecimal.ZERO);
    }

    @Override
    @Test
    public void sumBigInteger() {
        TriConstraintCollector<Integer, Integer, Integer, ?, BigInteger> collector = ConstraintCollectors
                .sumBigInteger((a, b, c) -> BigInteger.valueOf(a + b + c));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, BigInteger.ZERO);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        int firstValueC = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, BigInteger.valueOf(6));
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        int secondValueC = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, BigInteger.valueOf(16));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, BigInteger.valueOf(26));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, BigInteger.valueOf(16));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, BigInteger.valueOf(6));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, BigInteger.ZERO);
    }

    @Override
    @Test
    public void sumDuration() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Duration> collector = ConstraintCollectors
                .sumDuration((a, b, c) -> Duration.ofSeconds(a + b + c));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Duration.ZERO);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        int firstValueC = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, Duration.ofSeconds(6));
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        int secondValueC = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, Duration.ofSeconds(16));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, Duration.ofSeconds(26));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, Duration.ofSeconds(16));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, Duration.ofSeconds(6));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, Duration.ZERO);
    }

    @Override
    @Test
    public void sumPeriod() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Period> collector = ConstraintCollectors
                .sumPeriod((a, b, c) -> Period.ofDays(a + b + c));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Period.ZERO);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        int firstValueC = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, Period.ofDays(6));
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        int secondValueC = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, Period.ofDays(16));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, Period.ofDays(26));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, Period.ofDays(16));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, Period.ofDays(6));
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
        var collector = min((Integer a, Integer b, Integer c) -> baseLocalDateTime.plusMinutes(a + b + c));
        var container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the min
        int firstValueA = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(2));
        // add second value, lesser than the first, becomes the new min
        int secondValueA = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(1));
        // add third value, same as the second, result does not change
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, 0, 0);
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
        TriConstraintCollector<String, String, String, ?, String> collector = min((a, b, c) -> a, o -> o);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the min
        String firstValue = "b";
        Runnable firstRetractor = accumulate(collector, container, firstValue, null, null);
        assertResult(collector, container, firstValue);
        // add second value, lesser than the first, becomes the new min
        String secondValue = "a";
        Runnable secondRetractor = accumulate(collector, container, secondValue, null, null);
        assertResult(collector, container, secondValue);
        // add third value, same as the second, result does not change
        Runnable thirdRetractor = accumulate(collector, container, secondValue, null, null);
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
        var collector = max((Integer a, Integer b, Integer c) -> baseLocalDateTime.plusMinutes(a + b + c));
        var container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the max
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(2));
        // add second value, lesser than the first, result does not change
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(2));
        // add third value, same as the first, result does not change
        Runnable thirdRetractor = accumulate(collector, container, firstValue, 0, 0);
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
        TriConstraintCollector<String, String, String, ?, String> collector = max((a, b, c) -> a, o -> o);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // add first value, which becomes the max
        String firstValue = "b";
        Runnable firstRetractor = accumulate(collector, container, firstValue, null, null);
        assertResult(collector, container, firstValue);
        // add second value, lesser than the first, result does not change
        String secondValue = "a";
        Runnable secondRetractor = accumulate(collector, container, secondValue, null, null);
        assertResult(collector, container, firstValue);
        // add third value, same as the first, result does not change
        Runnable thirdRetractor = accumulate(collector, container, firstValue, null, null);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, Double> collector =
                ConstraintCollectors.average((i, i2, i3) -> i + i2 + i3);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, 4.0D);
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, 2.5D);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigDecimal((i, i2, i3) -> BigDecimal.valueOf(i + i2 + i3));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, BigDecimal.valueOf(4));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, BigDecimal.valueOf(2)); // Scale = 0.
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigInteger((i, i2, i3) -> BigInteger.valueOf(i + i2 + i3));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, BigDecimal.valueOf(4));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, BigDecimal.valueOf(2)); // Scale = 0.
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, Duration> collector =
                ConstraintCollectors.averageDuration((i, i2, i3) -> Duration.ofSeconds(i + i2 + i3));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, Duration.ofSeconds(4));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, Duration.ofMillis(2500));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, Set<Integer>> collector = ConstraintCollectors
                .toSet((a, b, c) -> a + b + c);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySet());
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 1;
        int firstValueC = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, singleton(6));
        // Add second value, we have two now.
        int secondValueA = 3;
        int secondValueB = 4;
        int secondValueC = 2;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, asSet(6, 9));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, asSet(6, 9));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSet(6, 9));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, singleton(6));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySet());
    }

    @Override
    @Test
    public void toSortedSet() {
        TriConstraintCollector<Integer, Integer, Integer, ?, SortedSet<Integer>> collector = ConstraintCollectors
                .toSortedSet((a, b, c) -> a + b + c);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedSet());
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 1;
        int firstValueC = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, asSortedSet(6));
        // Add second value, we have two now.
        int secondValueA = 3;
        int secondValueB = 4;
        int secondValueC = 2;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, asSortedSet(6, 9));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, asSortedSet(6, 9));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSortedSet(6, 9));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asSortedSet(6));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySortedSet());
    }

    @Override
    @Test
    public void toList() {
        TriConstraintCollector<Integer, Integer, Integer, ?, List<Integer>> collector = ConstraintCollectors
                .toList((a, b, c) -> a + b + c);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyList());
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 1;
        int firstValueC = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, singletonList(6));
        // Add second value, we have two now.
        int secondValueA = 3;
        int secondValueB = 4;
        int secondValueC = 2;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, asList(6, 9));
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, asList(6, 9, 9));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asList(6, 9));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, singletonList(6));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptyList());
    }

    @Override
    @Test
    public void toMap() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Map<Integer, Set<Integer>>> collector = ConstraintCollectors
                .toMap((a, b, c) -> a + b + c, (a, b, c) -> a + b + c);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, asMap(2, singleton(2)));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, asMap(2, singleton(2), 1, singleton(1)));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, Map<Integer, Integer>> collector = ConstraintCollectors
                .toMap((a, b, c) -> a, (a, b, c) -> b + c, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 1, 2);
        assertResult(collector, container, asMap(2, 3));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 1, 2);
        assertResult(collector, container, asMap(2, 3, 1, 3));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 2, 3);
        assertResult(collector, container, asMap(2, 3, 1, 8));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asMap(2, 3, 1, 5));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asMap(2, 3));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptyMap());
    }

    @Override
    @Test
    public void toSortedMap() {
        TriConstraintCollector<Integer, Integer, Integer, ?, SortedMap<Integer, Set<Integer>>> collector = ConstraintCollectors
                .toSortedMap((a, b, c) -> a + b + c, (a, b, c) -> a + b + c);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, asSortedMap(2, singleton(2)));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, asSortedMap(2, singleton(2), 1, singleton(1)));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, SortedMap<Integer, Integer>> collector = ConstraintCollectors
                .toSortedMap((a, b, c) -> a, (a, b, c) -> b + c, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 1, 2);
        assertResult(collector, container, asSortedMap(2, 3));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 1, 2);
        assertResult(collector, container, asSortedMap(2, 3, 1, 3));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 2, 3);
        assertResult(collector, container, asSortedMap(2, 3, 1, 8));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, asSortedMap(2, 3, 1, 5));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, asSortedMap(2, 3));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, emptySortedMap());
    }

    @Override
    @Test
    public void conditionally() {
        TriConstraintCollector<Integer, Integer, Integer, Object, Integer> collector =
                ConstraintCollectors.conditionally(
                        (i, i2, i3) -> i < 2,
                        max((i, i2, i3) -> i + i2 + i3, i -> i));
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, null);
        // Add first value, we have one now.
        int firstValue = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, 1);
        // Add second value; it is skipped even though it is greater than the first value.
        int secondValue = 2;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, 1);
        // Add third value, same as the first. We now have two values, both of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, firstValue, 0, 0);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, Pair<Integer, Integer>> collector =
                compose(min((i, i2, i3) -> i + i2 + i3, i -> i),
                        max((i, i2, i3) -> i + i2 + i3, i -> i),
                        Pair::new);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, new Pair<>(null, null));
        // Add first value.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, new Pair<>(2, 2));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, new Pair<>(1, 2));
        // Add third value, same as the second, result does not change.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, Triple<Integer, Integer, Double>> collector =
                compose(min((i, i2, i3) -> i + i2 + i3, i -> i),
                        max((i, i2, i3) -> i + i2 + i3, i -> i),
                        ConstraintCollectors.average((i, i2, i3) -> i + i2 + i3),
                        Triple::of);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Triple.of(null, null, null));
        // Add first value.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, Triple.of(4, 4, 4D));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, Triple.of(1, 4, 2.5D));
        // Add third value, same as the second.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
        TriConstraintCollector<Integer, Integer, Integer, ?, Quadruple<Long, Integer, Integer, Double>> collector =
                compose(countTri(),
                        min((i, i2, i3) -> i + i2 + i3, i -> i),
                        max((i, i2, i3) -> i + i2 + i3, i -> i),
                        ConstraintCollectors.average((i, i2, i3) -> i + i2 + i3),
                        Quadruple::new);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, new Quadruple<>(0L, null, null, null));
        // Add first value.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, new Quadruple<>(1L, 4, 4, 4D));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, new Quadruple<>(2L, 1, 4, 2.5D));
        // Add third value, same as the second.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
        var collector =
                ConstraintCollectors.toConsecutiveSequences((Integer a, Integer b, Integer c) -> a + b + c, Integer::intValue);
        var container = collector.supplier().get();
        // Add first value, sequence is [2]
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResultRecursive(collector, container, buildSequenceChain(2));
        // Add second value, sequence is [1,2]
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResultRecursive(collector, container, buildSequenceChain(1, 2));
        // Add third value, same as the second. Sequence is [{1,1},2}]
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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
                ConstraintCollectors.toConnectedRanges((Integer a, Integer b, Object c) -> new Interval(a, b),
                        Interval::start,
                        Interval::end, (a, b) -> b - a);
        var container = collector.supplier().get();
        // Add first value, sequence is [(1,3)]
        var firstRetractor = accumulate(collector, container, 1, 3, null);
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3)));
        // Add second value, sequence is [(1,3),(2,4)]
        var secondRetractor = accumulate(collector, container, 2, 4, null);
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3), new Interval(2, 4)));
        // Add third value, same as the second. Sequence is [{1,1},2}]
        var thirdRetractor = accumulate(collector, container, 2, 4, null);
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
                ConstraintCollectors.toConnectedRanges((DynamicInterval a, Object b, Object c) -> a,
                        DynamicInterval::getStart,
                        DynamicInterval::getEnd, (a, b) -> b - a);

        var first = new DynamicInterval(0);
        var second = new DynamicInterval(10);
        var third = new DynamicInterval(20);
        var container = dynamicCollector.supplier().get();

        // Add first value, sequence is [[(0, 10)]]
        var firstRetractor = accumulate(dynamicCollector, container, first, null, null);
        assertResult(dynamicCollector, container, buildDynamicConsecutiveUsage(new DynamicInterval(0)));

        // Add third value, sequence is [[(0, 10)], [(20, 30)]]
        accumulate(dynamicCollector, container, third, null, null);
        assertResult(dynamicCollector, container,
                buildDynamicConsecutiveUsage(new DynamicInterval(0), new DynamicInterval(20)));

        // Add second value, sequence is [[(0, 10), (10, 20), (20, 30)]]
        accumulate(dynamicCollector, container, second, null, null);
        assertResult(dynamicCollector, container,
                buildDynamicConsecutiveUsage(new DynamicInterval(0), new DynamicInterval(10), new DynamicInterval(20)));

        // Change first value, retract it, then re-add it
        first.setStart(-5);
        firstRetractor.run();
        accumulate(dynamicCollector, container, first, null, null);

        assertResult(dynamicCollector, container,
                buildDynamicConsecutiveUsage(new DynamicInterval(-5), new DynamicInterval(10), new DynamicInterval(20)));
    }

    @Override
    @Test
    public void loadBalance() {
        var collector = ConstraintCollectors.<String, Integer, Integer, String> loadBalance((a, b, c) -> a, (a, b, c) -> b);
        var container = collector.supplier().get();

        // Default state.
        assertUnfairness(collector, container, BigDecimal.ZERO);
        // Add first value, we have one now.
        var firstRetractor = accumulate(collector, container, "A", 2, 0);
        assertUnfairness(collector, container, BigDecimal.ZERO);
        // Add second value, we have two now.
        var secondRetractor = accumulate(collector, container, "B", 1, 0);
        // sqrt((3-2.5)^2 + (2 - 2.5)^2) = sqrt(0.5^2 + 0.5^2) = sqrt(0.25 + 0.25) = sqrt(0.5)
        assertUnfairness(collector, container, BigDecimal.valueOf(0.707107));
        // Add third value, same as the second. We now have three values, two of which are the same.
        var thirdRetractor = accumulate(collector, container, "B", 1, 0);
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
        var collector = ConstraintCollectors.collectAndThen(countTri(), i -> i * 10);
        var container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0L);
        // Add first value, we have one now.
        var firstValue = 2;
        var firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, 10L);
        // Add second value, we have two now.
        var secondValue = 1;
        var secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, 20L);
        // Add third value, same as the second. We now have three values, two of which are the same.
        var thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
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

    private static <A, B, C, Container_, Result_> Runnable accumulate(
            TriConstraintCollector<A, B, C, Container_, Result_> collector, Object container, A valueA, B valueB,
            C valueC) {
        var slot = ((TriConstraintCollectorAccumulator<Container_, A, B, C>) collector.accumulator())
                .intoGroup((Container_) container);
        slot.add(valueA, valueB, valueC);
        return slot::remove;
    }

    private static <A, B, C, Container_, Result_> TriConstraintCollectorValueHandle<A, B, C> insert(
            TriConstraintCollector<A, B, C, Container_, Result_> collector, Object container, A a, B b, C c) {
        var slot = ((TriConstraintCollectorAccumulator<Container_, A, B, C>) collector.accumulator())
                .intoGroup((Container_) container);
        slot.add(a, b, c);
        return slot;
    }

    @Test
    public void countUpdate() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Long> collector = countTri();
        Object container = collector.supplier().get();
        var slot = insert(collector, container, 1, 0, 0);
        assertResult(collector, container, 1L);
        slot.replaceWith(42, 0, 0); // no-op for count
        assertResult(collector, container, 1L);
        slot.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void conditionallyUpdate() {
        TriConstraintCollector<Integer, Integer, Integer, Object, Integer> collector =
                ConstraintCollectors.conditionally((Integer a, Integer b, Integer c) -> a < 2,
                        max((i, i2, i3) -> i + i2 + i3, i -> i));
        Object container = collector.supplier().get();
        var slot = insert(collector, container, 1, 0, 0); // active (1 < 2)
        assertResult(collector, container, 1);
        slot.replaceWith(2, 0, 0); // active → inactive (2 is not < 2)
        assertResult(collector, container, null);
        slot.replaceWith(0, 0, 0); // inactive → active (0 < 2)
        assertResult(collector, container, 0);
        slot.replaceWith(3, 0, 0); // active → inactive
        assertResult(collector, container, null);
        slot.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void compose2Update() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Pair<Integer, Integer>> collector =
                compose(min((i, i2, i3) -> i + i2 + i3, i -> i),
                        max((i, i2, i3) -> i + i2 + i3, i -> i),
                        Pair::new);
        Object container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0, 0);
        var slot2 = insert(collector, container, 4, 0, 0);
        assertResult(collector, container, new Pair<>(2, 4));
        slot1.replaceWith(3, 0, 0); // 2 → 3; min becomes 3
        assertResult(collector, container, new Pair<>(3, 4));
        slot2.remove();
        slot1.remove();
        assertResult(collector, container, new Pair<>(null, null));
    }

    @Test
    public void collectAndThenUpdate() {
        var collector = ConstraintCollectors.collectAndThen(countTri(), i -> i * 10);
        var container = collector.supplier().get();
        var slot = insert(collector, container, 1, 0, 0);
        assertResult(collector, container, 10L);
        slot.replaceWith(99, 0, 0); // count no-op; result unchanged
        assertResult(collector, container, 10L);
        slot.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void sumUpdate() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Long> collector =
                ConstraintCollectors.sum((a, b, c) -> a + b + c);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0, 0);
        assertResult(collector, container, 2L);
        slot1.replaceWith(5, 0, 0);
        assertResult(collector, container, 5L);
        slot1.replaceWith(5, 0, 0); // no-op
        assertResult(collector, container, 5L);
        var slot2 = insert(collector, container, 3, 0, 0);
        assertResult(collector, container, 8L);
        slot1.replaceWith(1, 0, 0);
        assertResult(collector, container, 4L);
        slot2.remove();
        assertResult(collector, container, 1L);
        slot1.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void averageUpdate() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Double> collector =
                ConstraintCollectors.average((a, b, c) -> a + b + c);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4, 0, 0);
        var slot2 = insert(collector, container, 2, 0, 0);
        assertResult(collector, container, 3.0D);
        slot1.replaceWith(6, 0, 0); // (6+2)/2 = 4.0; count unchanged
        assertResult(collector, container, 4.0D);
        slot1.replaceWith(6, 0, 0); // no-op
        assertResult(collector, container, 4.0D);
        slot2.remove();
        assertResult(collector, container, 6.0D);
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void countDistinctUpdate() {
        TriConstraintCollector<String, String, String, ?, Long> collector =
                ConstraintCollectors.countDistinct((a, b, c) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, "a", null, null);
        var slot2 = insert(collector, container, "b", null, null);
        assertResult(collector, container, 2L);
        slot1.replaceWith("b", null, null); // both map to "b"
        assertResult(collector, container, 1L);
        slot1.replaceWith("b", null, null); // no-op: Objects.equals short-circuit
        assertResult(collector, container, 1L);
        slot1.replaceWith("c", null, null); // "b" and "c"
        assertResult(collector, container, 2L);
        slot2.remove();
        assertResult(collector, container, 1L);
        slot1.remove();
        assertResult(collector, container, 0L);
    }

    @Test
    public void sumBigDecimalUpdate() {
        TriConstraintCollector<BigDecimal, BigDecimal, BigDecimal, ?, BigDecimal> collector =
                ConstraintCollectors.sumBigDecimal((a, b, c) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, BigDecimal.ONE, null, null);
        var slot2 = insert(collector, container, BigDecimal.TEN, null, null);
        assertResult(collector, container, BigDecimal.valueOf(11));
        var bd4 = BigDecimal.valueOf(4);
        slot1.replaceWith(bd4, null, null);
        assertResult(collector, container, BigDecimal.valueOf(14));
        slot1.replaceWith(bd4, null, null); // no-op: same reference, == short-circuit
        assertResult(collector, container, BigDecimal.valueOf(14));
        slot2.remove();
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot1.remove();
        assertResult(collector, container, BigDecimal.ZERO);
    }

    @Test
    public void sumBigIntegerUpdate() {
        TriConstraintCollector<BigInteger, BigInteger, BigInteger, ?, BigInteger> collector =
                ConstraintCollectors.sumBigInteger((a, b, c) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, BigInteger.ONE, null, null);
        var slot2 = insert(collector, container, BigInteger.TEN, null, null);
        assertResult(collector, container, BigInteger.valueOf(11));
        var bi4 = BigInteger.valueOf(4);
        slot1.replaceWith(bi4, null, null);
        assertResult(collector, container, BigInteger.valueOf(14));
        slot1.replaceWith(bi4, null, null); // no-op: same reference, == short-circuit
        assertResult(collector, container, BigInteger.valueOf(14));
        slot2.remove();
        assertResult(collector, container, BigInteger.valueOf(4));
        slot1.remove();
        assertResult(collector, container, BigInteger.ZERO);
    }

    @Test
    public void sumDurationUpdate() {
        TriConstraintCollector<Duration, Duration, Duration, ?, Duration> collector =
                ConstraintCollectors.sumDuration((a, b, c) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, Duration.ofSeconds(1), null, null);
        var slot2 = insert(collector, container, Duration.ofSeconds(2), null, null);
        assertResult(collector, container, Duration.ofSeconds(3));
        var d4 = Duration.ofSeconds(4);
        slot1.replaceWith(d4, null, null);
        assertResult(collector, container, Duration.ofSeconds(6));
        slot1.replaceWith(d4, null, null); // no-op: same reference, == short-circuit
        assertResult(collector, container, Duration.ofSeconds(6));
        slot2.remove();
        assertResult(collector, container, Duration.ofSeconds(4));
        slot1.remove();
        assertResult(collector, container, Duration.ZERO);
    }

    @Test
    public void sumPeriodUpdate() {
        TriConstraintCollector<Period, Period, Period, ?, Period> collector =
                ConstraintCollectors.sumPeriod((a, b, c) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, Period.ofDays(1), null, null);
        var slot2 = insert(collector, container, Period.ofDays(2), null, null);
        assertResult(collector, container, Period.ofDays(3));
        var p4 = Period.ofDays(4);
        slot1.replaceWith(p4, null, null);
        assertResult(collector, container, Period.ofDays(6));
        slot1.replaceWith(p4, null, null); // no-op: same reference, == short-circuit
        assertResult(collector, container, Period.ofDays(6));
        slot2.remove();
        assertResult(collector, container, Period.ofDays(4));
        slot1.remove();
        assertResult(collector, container, Period.ZERO);
    }

    @Test
    public void averageBigDecimalUpdate() {
        TriConstraintCollector<Integer, Integer, Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigDecimal((a, b, c) -> BigDecimal.valueOf(a + b + c));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4, 0, 0);
        var slot2 = insert(collector, container, 2, 0, 0);
        assertResult(collector, container, BigDecimal.valueOf(3));
        slot1.replaceWith(6, 0, 0); // (6+2)/2 = 4; count unchanged
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot1.replaceWith(6, 0, 0); // no-op: same input value
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot2.remove();
        assertResult(collector, container, BigDecimal.valueOf(6));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void averageBigIntegerUpdate() {
        TriConstraintCollector<Integer, Integer, Integer, ?, BigDecimal> collector =
                ConstraintCollectors.averageBigInteger((a, b, c) -> BigInteger.valueOf(a + b + c));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4, 0, 0);
        var slot2 = insert(collector, container, 2, 0, 0);
        assertResult(collector, container, BigDecimal.valueOf(3));
        slot1.replaceWith(6, 0, 0);
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot1.replaceWith(6, 0, 0); // no-op: same input value
        assertResult(collector, container, BigDecimal.valueOf(4));
        slot2.remove();
        assertResult(collector, container, BigDecimal.valueOf(6));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void averageDurationUpdate() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Duration> collector =
                ConstraintCollectors.averageDuration((a, b, c) -> Duration.ofSeconds(a + b + c));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 4, 0, 0);
        var slot2 = insert(collector, container, 2, 0, 0);
        assertResult(collector, container, Duration.ofSeconds(3));
        slot1.replaceWith(6, 0, 0); // (6+2)/2 = 4; count unchanged
        assertResult(collector, container, Duration.ofSeconds(4));
        slot1.replaceWith(6, 0, 0); // no-op: same input value
        assertResult(collector, container, Duration.ofSeconds(4));
        slot2.remove();
        assertResult(collector, container, Duration.ofSeconds(6));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void toConsecutiveSequencesUpdate() {
        var collector = ConstraintCollectors.toConsecutiveSequences(
                (Integer a, Integer b, Integer c) -> a + b + c, Integer::intValue);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0, 0);
        var slot2 = insert(collector, container, 3, 0, 0); // gap of 2 — two sequences
        assertResultRecursive(collector, container, buildSequenceChain(1, 3));
        slot1.replaceWith(2, 0, 0); // 2 and 3 are consecutive — one sequence
        assertResultRecursive(collector, container, buildSequenceChain(2, 3));
        slot1.replaceWith(2, 0, 0); // same value → result unchanged
        assertResultRecursive(collector, container, buildSequenceChain(2, 3));
        slot2.remove();
        assertResultRecursive(collector, container, buildSequenceChain(2));
        slot1.remove();
        assertResultRecursive(collector, container, buildSequenceChain());
    }

    @Test
    public void consecutiveUsageUpdate() {
        var collector = ConstraintCollectors.toConnectedRanges(
                (Integer a, Integer b, Object c) -> new Interval(a, b),
                Interval::start,
                Interval::end, (a, b) -> b - a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 3, null);
        var slot2 = insert(collector, container, 10, 20, null); // disjoint
        assertResult(collector, container, buildConsecutiveUsage(new Interval(1, 3), new Interval(10, 20)));
        var i812 = new Interval(8, 12);
        slot1.replaceWith(8, 12, null); // now overlaps with (10,20)
        assertResult(collector, container, buildConsecutiveUsage(new Interval(8, 12), new Interval(10, 20)));
        slot1.replaceWith(8, 12, null); // same value → result unchanged
        assertResult(collector, container, buildConsecutiveUsage(new Interval(8, 12), new Interval(10, 20)));
        slot2.remove();
        assertResult(collector, container, buildConsecutiveUsage(i812));
        slot1.remove();
        assertResult(collector, container, buildConsecutiveUsage());
    }

    @Test
    public void toListUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer, Integer> toList((a, b, c) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0, 0);
        var slot2 = insert(collector, container, 2, 0, 0);
        assertResult(collector, container, asList(1, 2));
        slot1.replaceWith(3, 0, 0);
        assertResult(collector, container, asList(3, 2));
        slot1.replaceWith(3, 0, 0); // no-op
        assertResult(collector, container, asList(3, 2));
        slot2.remove();
        assertResult(collector, container, singletonList(3));
        slot1.remove();
        assertResult(collector, container, emptyList());
    }

    @Test
    public void toSetUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer, Integer> toSet((a, b, c) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0, 0);
        var slot2 = insert(collector, container, 2, 0, 0);
        assertResult(collector, container, asSet(1, 2));
        slot1.replaceWith(3, 0, 0);
        assertResult(collector, container, asSet(2, 3));
        slot1.replaceWith(3, 0, 0); // Objects.equals short-circuit
        assertResult(collector, container, asSet(2, 3));
        slot2.remove();
        assertResult(collector, container, asSet(3));
        slot1.remove();
        assertResult(collector, container, emptySet());
    }

    @Test
    public void toSortedSetUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer, Integer> toSortedSet((a, b, c) -> a);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0, 0);
        var slot2 = insert(collector, container, 2, 0, 0);
        assertResult(collector, container, asSortedSet(1, 2));
        slot1.replaceWith(3, 0, 0);
        assertResult(collector, container, asSortedSet(2, 3));
        slot1.replaceWith(3, 0, 0); // Objects.equals short-circuit
        assertResult(collector, container, asSortedSet(2, 3));
        slot2.remove();
        assertResult(collector, container, asSortedSet(3));
        slot1.remove();
        assertResult(collector, container, emptySortedSet());
    }

    @Test
    public void toCollectionUpdate() {
        var collector =
                InnerTriConstraintCollectors.<Integer, Integer, Integer, Integer, ArrayList<Integer>> toCollection(
                        (a, b, c) -> a, ArrayList::new);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 1, 0, 0);
        var slot2 = insert(collector, container, 2, 0, 0);
        assertResult(collector, container, new ArrayList<>(asList(1, 2)));
        slot1.replaceWith(3, 0, 0);
        assertResult(collector, container, new ArrayList<>(asList(3, 2)));
        slot1.replaceWith(3, 0, 0); // no-op
        assertResult(collector, container, new ArrayList<>(asList(3, 2)));
        slot2.remove();
        assertResult(collector, container, new ArrayList<>(singletonList(3)));
        slot1.remove();
        assertResult(collector, container, new ArrayList<>());
    }

    @Test
    public void toMapUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer, Integer, Integer> toMap(
                (a, b, c) -> a + b + c, (a, b, c) -> a + b + c);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0, 0);
        var slot2 = insert(collector, container, 1, 0, 0);
        assertResult(collector, container, asMap(2, singleton(2), 1, singleton(1)));
        slot1.replaceWith(3, 0, 0);
        assertResult(collector, container, asMap(1, singleton(1), 3, singleton(3)));
        slot1.replaceWith(3, 0, 0); // Objects.equals short-circuit on Pair(3,3)
        assertResult(collector, container, asMap(1, singleton(1), 3, singleton(3)));
        slot2.remove();
        assertResult(collector, container, asMap(3, singleton(3)));
        slot1.remove();
        assertResult(collector, container, emptyMap());
    }

    @Test
    public void toMapMergedUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer, Integer, Integer> toMap(
                (a, b, c) -> a + b + c, (a, b, c) -> a + b + c, Integer::sum);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0, 0);
        var slot2 = insert(collector, container, 1, 0, 0);
        assertResult(collector, container, asMap(2, 2, 1, 1));
        slot1.replaceWith(3, 0, 0);
        assertResult(collector, container, asMap(1, 1, 3, 3));
        slot1.replaceWith(3, 0, 0); // Objects.equals short-circuit on Pair(3,3)
        assertResult(collector, container, asMap(1, 1, 3, 3));
        slot2.remove();
        assertResult(collector, container, asMap(3, 3));
        slot1.remove();
        assertResult(collector, container, emptyMap());
    }

    @Test
    public void toSortedMapUpdate() {
        var collector = ConstraintCollectors.<Integer, Integer, Integer, Integer, Integer> toSortedMap(
                (a, b, c) -> a + b + c, (a, b, c) -> a + b + c);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 2, 0, 0);
        var slot2 = insert(collector, container, 1, 0, 0);
        assertResult(collector, container, asSortedMap(1, singleton(1), 2, singleton(2)));
        slot1.replaceWith(3, 0, 0);
        assertResult(collector, container, asSortedMap(1, singleton(1), 3, singleton(3)));
        slot1.replaceWith(3, 0, 0); // Objects.equals short-circuit on Pair(3,3)
        assertResult(collector, container, asSortedMap(1, singleton(1), 3, singleton(3)));
        slot2.remove();
        assertResult(collector, container, asSortedMap(3, singleton(3)));
        slot1.remove();
        assertResult(collector, container, emptySortedMap());
    }

    @Test
    public void minComparableUpdate() {
        var baseLocalDateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        var collector = min((Integer a, Integer b, Integer c) -> baseLocalDateTime.plusMinutes(a + b + c));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 5, 0, 0);
        var slot2 = insert(collector, container, 3, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(3));
        slot2.replaceWith(6, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(5));
        slot2.replaceWith(6, 0, 0); // Objects.equals short-circuit
        assertResult(collector, container, baseLocalDateTime.plusMinutes(5));
        slot1.replaceWith(1, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(1));
        slot2.remove();
        assertResult(collector, container, baseLocalDateTime.plusMinutes(1));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void maxComparableUpdate() {
        var baseLocalDateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        var collector = max((Integer a, Integer b, Integer c) -> baseLocalDateTime.plusMinutes(a + b + c));
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, 3, 0, 0);
        var slot2 = insert(collector, container, 5, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(5));
        slot2.replaceWith(2, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(3));
        slot2.replaceWith(2, 0, 0); // Objects.equals short-circuit
        assertResult(collector, container, baseLocalDateTime.plusMinutes(3));
        slot1.replaceWith(7, 0, 0);
        assertResult(collector, container, baseLocalDateTime.plusMinutes(7));
        slot2.remove();
        assertResult(collector, container, baseLocalDateTime.plusMinutes(7));
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void minNotComparableUpdate() {
        var collector = min((String a, String b, String c) -> a, o -> o);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, "b", null, null);
        var slot2 = insert(collector, container, "a", null, null);
        assertResult(collector, container, "a");
        slot2.replaceWith("c", null, null);
        assertResult(collector, container, "b");
        slot2.replaceWith("c", null, null); // Objects.equals short-circuit
        assertResult(collector, container, "b");
        slot1.replaceWith("a", null, null);
        assertResult(collector, container, "a");
        slot2.remove();
        assertResult(collector, container, "a");
        slot1.remove();
        assertResult(collector, container, null);
    }

    @Test
    public void maxNotComparableUpdate() {
        var collector = max((String a, String b, String c) -> a, o -> o);
        var container = collector.supplier().get();
        var slot1 = insert(collector, container, "b", null, null);
        var slot2 = insert(collector, container, "a", null, null);
        assertResult(collector, container, "b");
        slot2.replaceWith("c", null, null);
        assertResult(collector, container, "c");
        slot2.replaceWith("c", null, null); // Objects.equals short-circuit
        assertResult(collector, container, "c");
        slot1.replaceWith("a", null, null);
        assertResult(collector, container, "c");
        slot2.remove();
        assertResult(collector, container, "a");
        slot1.remove();
        assertResult(collector, container, null);
    }

    private static <A, B, C, Container_, Result_> void assertResult(
            TriConstraintCollector<A, B, C, Container_, Result_> collector, Object container, Result_ expectedResult) {
        Result_ actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult)
                .as("Collector (" + collector + ") did not produce expected result.")
                .isEqualTo(expectedResult);
    }

    private static <A, B, C, Container_, Result_> void assertResultRecursive(
            TriConstraintCollector<A, B, C, Container_, Result_> collector,
            Object container, Result_ expectedResult) {
        var actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult)
                .as("Collector (" + collector + ") did not produce expected result.")
                .usingRecursiveComparison()
                .ignoringFields("sourceTree", "indexFunction", "sequenceList", "startItemToSequence")
                .isEqualTo(expectedResult);
    }

    private static <Container_> void assertUnfairness(
            TriConstraintCollector<String, Integer, Integer, Container_, LoadBalance<String>> collector, Object container,
            BigDecimal expectedValue) {
        LoadBalance<String> actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult.unfairness())
                .as("Collector (" + collector + ") did not produce expected result.")
                .isEqualTo(expectedValue);
    }

}
