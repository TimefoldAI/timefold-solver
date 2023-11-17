package ai.timefold.solver.core.impl.score.stream.tri;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.compose;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.countLongTri;
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
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiConsumer;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors.SequenceChain;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.ConsecutiveSetTree;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

class InnerTriConstraintCollectorsTest {

    @Test
    void countTri() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Integer> collector = ConstraintCollectors.countTri();
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 1;
        int firstValueC = 3;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, 1);
        // Add second value, we have two now.
        int secondValueA = 1;
        int secondValueB = 2;
        int secondValueC = 3;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, 2);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
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

    @Test
    void countTriLong() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Long> collector = countLongTri();
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

    @Test
    void countDistinctTri() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Integer> collector = ConstraintCollectors
                .countDistinct((a, b, c) -> a + b + c);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        int firstValueC = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, 1);
        // Add second value, we have two now.
        int secondValueA = 1;
        int secondValueB = 2;
        int secondValueC = 3;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, 2);
        // Add third value, same as the second. We now have two distinct values.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
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

    @Test
    void countDistinctTriLong() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Long> collector = ConstraintCollectors
                .countDistinctLong((a, b, c) -> a + b + c);
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

    @Test
    void sumTri() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Integer> collector = ConstraintCollectors
                .sum((a, b, c) -> a + b + c);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, 0);
        // Add first value, we have one now.
        int firstValueA = 2;
        int firstValueB = 3;
        int firstValueC = 1;
        Runnable firstRetractor = accumulate(collector, container, firstValueA, firstValueB, firstValueC);
        assertResult(collector, container, 6);
        // Add second value, we have two now.
        int secondValueA = 4;
        int secondValueB = 5;
        int secondValueC = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, 16);
        // Add third value, same as the second. We now have three values, two of which are the same.
        Runnable thirdRetractor = accumulate(collector, container, secondValueA, secondValueB, secondValueC);
        assertResult(collector, container, 26);
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResult(collector, container, 16);
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResult(collector, container, 6);
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, 0);
    }

    @Test
    void sumTriLong() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Long> collector = ConstraintCollectors
                .sumLong((a, b, c) -> a + b + c);
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

    @Test
    void sumTriBigDecimal() {
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

    @Test
    void sumTriBigInteger() {
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

    @Test
    void sumTriDuration() {
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

    @Test
    void sumTriPeriod() {
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

    @Test
    void minComparableTri() {
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

    @Test
    void minNotComparableTri() {
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

    @Test
    void maxComparableTri() {
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

    @Test
    void maxNotComparableTri() {
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

    @Test
    void averageTri() {
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

    @Test
    void averageTriLong() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Double> collector =
                ConstraintCollectors.averageLong((i, i2, i3) -> i + i2 + i3);
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

    @Test
    void averageTriBigDecimal() {
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

    @Test
    void averageTriBigInteger() {
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

    @Test
    void averageTriDuration() {
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

    @Test
    void toSetTri() {
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

    @Test
    void toSortedSetTri() {
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

    @Test
    void toListTri() {
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

    @Test
    void toMapTri() {
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

    @Test
    void toMapTriMerged() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Map<Integer, Integer>> collector = ConstraintCollectors
                .toMap((a, b, c) -> a + b + c, (a, b, c) -> a + b + c, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptyMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, asMap(2, 2));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, asMap(2, 2, 1, 1));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, asMap(2, 2, 1, 2));
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

    @Test
    void toSortedMapBi() {
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

    @Test
    void toSortedMapTri() {
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

    @Test
    void toSortedMapTriMerged() {
        TriConstraintCollector<Integer, Integer, Integer, ?, SortedMap<Integer, Integer>> collector = ConstraintCollectors
                .toSortedMap((a, b, c) -> a + b + c, (a, b, c) -> a + b + c, Integer::sum);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, emptySortedMap());
        // Add first value, we have one now.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, asSortedMap(2, 2));
        // Add second value, we have two now.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, asSortedMap(2, 2, 1, 1));
        // Add third value, same as the second. We now have three values, two of which map to the same key.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, asSortedMap(2, 2, 1, 2));
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

    @Test
    void conditionallyTri() {
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

    @Test
    void compose2Tri() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Pair<Integer, Integer>> collector =
                compose(min((i, i2, i3) -> i + i2 + i3, i -> i),
                        max((i, i2, i3) -> i + i2 + i3, i -> i),
                        Pair::of);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Pair.of(null, null));
        // Add first value.
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, Pair.of(2, 2));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, Pair.of(1, 2));
        // Add third value, same as the second, result does not change.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, Pair.of(1, 2));
        // Retract one instance of the second value; nothing should change.
        secondRetractor.run();
        assertResult(collector, container, Pair.of(1, 2));
        // Retract final instance of the second value.
        thirdRetractor.run();
        assertResult(collector, container, Pair.of(2, 2));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, Pair.of(null, null));
    }

    @Test
    void compose3Tri() {
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

    @Test
    void compose4Tri() {
        TriConstraintCollector<Integer, Integer, Integer, ?, Quadruple<Integer, Integer, Integer, Double>> collector =
                compose(ConstraintCollectors.countTri(),
                        min((i, i2, i3) -> i + i2 + i3, i -> i),
                        max((i, i2, i3) -> i + i2 + i3, i -> i),
                        ConstraintCollectors.average((i, i2, i3) -> i + i2 + i3),
                        Quadruple::of);
        Object container = collector.supplier().get();

        // Default state.
        assertResult(collector, container, Quadruple.of(0, null, null, null));
        // Add first value.
        int firstValue = 4;
        Runnable firstRetractor = accumulate(collector, container, firstValue, 0, 0);
        assertResult(collector, container, Quadruple.of(1, 4, 4, 4D));
        // Add second value, lesser than the first.
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, Quadruple.of(2, 1, 4, 2.5D));
        // Add third value, same as the second.
        Runnable thirdRetractor = accumulate(collector, container, secondValue, 0, 0);
        assertResult(collector, container, Quadruple.of(3, 1, 4, 2D));
        // Retract one instance of the second value.
        secondRetractor.run();
        assertResult(collector, container, Quadruple.of(2, 1, 4, 2.5D));
        // Retract final instance of the second value.
        thirdRetractor.run();
        assertResult(collector, container, Quadruple.of(1, 4, 4, 4D));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResult(collector, container, Quadruple.of(0, null, null, null));
    }

    @Test
    void toConsecutiveSequences() {
        // Do a basic test w/o edge cases; edge cases are covered in ConsecutiveSetTreeTest
        var collector = ConstraintCollectors.toConsecutiveSequences(Integer::intValue);
        var container = collector.supplier().get();
        // Add first value, sequence is [2]
        int firstValue = 2;
        Runnable firstRetractor = accumulate(collector, container, firstValue);
        assertResultRecursive(collector, container, consecutiveData(2));
        // Add second value, sequence is [1,2]
        int secondValue = 1;
        Runnable secondRetractor = accumulate(collector, container, secondValue);
        assertResultRecursive(collector, container, consecutiveData(1, 2));
        // Add third value, same as the second. Sequence is [{1,1},2}]
        Runnable thirdRetractor = accumulate(collector, container, secondValue);
        assertResultRecursive(collector, container, consecutiveData(1, 1, 2));
        // Retract one instance of the second value; we only have two values now.
        secondRetractor.run();
        assertResultRecursive(collector, container, consecutiveData(1, 2));
        // Retract final instance of the second value; we only have one value now.
        thirdRetractor.run();
        assertResultRecursive(collector, container, consecutiveData(2));
        // Retract last value; there are no values now.
        firstRetractor.run();
        assertResultRecursive(collector, container, consecutiveData());
    }

    private SequenceChain<Integer, Integer> consecutiveData(Integer... data) {
        return Arrays.stream(data).collect(
                () -> new ConsecutiveSetTree<Integer, Integer, Integer>((a, b) -> b - a, Integer::sum, 1, 0),
                (tree, datum) -> tree.add(datum, datum),
                mergingNotSupported());
    }

    private static <T> BiConsumer<T, T> mergingNotSupported() {
        return (a, b) -> {
            throw new UnsupportedOperationException();
        };
    }

    private static <A, B, C, Container_, Result_> Runnable accumulate(
            TriConstraintCollector<A, B, C, Container_, Result_> collector, Object container, A valueA, B valueB,
            C valueC) {
        return collector.accumulator().apply((Container_) container, valueA, valueB, valueC);
    }

    private static <A, B, Container_, Result_> Runnable accumulate(
            BiConstraintCollector<A, B, Container_, Result_> collector, Object container, A valueA, B valueB) {
        return collector.accumulator().apply((Container_) container, valueA, valueB);
    }

    private static <A, Container_, Result_> Runnable accumulate(
            UniConstraintCollector<A, Container_, Result_> collector, Object container, A value) {
        return collector.accumulator().apply((Container_) container, value);
    }

    private static <A, B, C, Container_, Result_> void assertResult(
            TriConstraintCollector<A, B, C, Container_, Result_> collector, Object container, Result_ expectedResult) {
        Result_ actualResult = collector.finisher().apply((Container_) container);
        assertThat(actualResult)
                .as("Collector (" + collector + ") did not produce expected result.")
                .isEqualTo(expectedResult);
    }

    private static <A, B, Container_, Result_> void assertResult(
            BiConstraintCollector<A, B, Container_, Result_> collector, Object container, Result_ expectedResult) {
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

}
