package ai.timefold.solver.core.impl.score.stream.collector.consecutive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ConsecutiveSetTreeTest {

    @Test
    void testNonconsecutiveNumbers() {
        var tree = getIntegerConsecutiveSetTree();
        var start1 = atomic(3);
        var middle3 = atomic(5);
        var end7 = atomic(5);

        tree.add(start1, 1);
        tree.add(middle3, 3);
        tree.add(end7, 7);

        var sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertSoftly(softly -> {
            softly.assertThat(sequenceList).hasSize(3);
            softly.assertThat(tree.getFirstSequence())
                    .usingRecursiveComparison()
                    .isEqualTo(sequenceList.get(0));
            softly.assertThat(tree.getLastSequence())
                    .usingRecursiveComparison()
                    .isEqualTo(sequenceList.get(2));
        });

        var breakList = new IterableList<>(tree.getBreaks());
        assertSoftly(softly -> {
            softly.assertThat(breakList).hasSize(2);
            softly.assertThat(tree.getFirstBreak())
                    .usingRecursiveComparison()
                    .isEqualTo(breakList.get(0));
            softly.assertThat(tree.getLastBreak())
                    .usingRecursiveComparison()
                    .isEqualTo(breakList.get(1));
        });

        assertThat(tree.getConsecutiveSequences()).allMatch(seq -> seq.getCount() == 1);
    }

    @Test
    void testConsecutiveNumbers() {
        ConsecutiveSetTree<AtomicInteger, Integer, Integer> tree = getIntegerConsecutiveSetTree();
        AtomicInteger breakStart3 = atomic(3);
        AtomicInteger breakEnd5 = atomic(5);

        tree.add(atomic(1), 1);
        tree.add(atomic(2), 2);
        tree.add(breakStart3, 3);

        tree.add(breakEnd5, 5);
        tree.add(atomic(6), 6);
        tree.add(atomic(7), 7);
        tree.add(atomic(8), 8);

        IterableList<Sequence<AtomicInteger, Integer>> sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertThat(sequenceList).hasSize(2);
        IterableList<Break<AtomicInteger, Integer>> breakList = new IterableList<>(tree.getBreaks());
        assertThat(breakList).hasSize(1);

        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(sequenceList.get(1).getCount()).isEqualTo(4);
        assertThat(breakList.get(0)).usingRecursiveComparison().isEqualTo(getBreak(tree, breakStart3, breakEnd5));
    }

    @Test
    void testDuplicateNumbers() {
        var tree = getIntegerConsecutiveSetTree();
        var duplicateValue = atomic(3);
        tree.add(atomic(1), 1);
        tree.add(atomic(2), 2);
        tree.add(duplicateValue, 3);
        tree.add(duplicateValue, 3);
        tree.add(duplicateValue, 3);

        var sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertSoftly(softly -> {
            softly.assertThat(sequenceList).hasSize(1);
            softly.assertThat(tree.getFirstSequence())
                    .usingRecursiveComparison()
                    .isEqualTo(sequenceList.get(0));
            softly.assertThat(tree.getLastSequence())
                    .usingRecursiveComparison()
                    .isEqualTo(sequenceList.get(0));
            softly.assertThat(sequenceList)
                    .first()
                    .matches(sequence -> sequence.getCount() == 3);
        });

        var breakList = new IterableList<>(tree.getBreaks());
        assertSoftly(softly -> {
            softly.assertThat(breakList).isEmpty();
            softly.assertThat(tree.getBreaks()).isEmpty();
            softly.assertThat(tree.getFirstBreak()).isNull();
            softly.assertThat(tree.getLastBreak()).isNull();
        });

        duplicateValue.set(0); // mimic the constraint collector changing a planning variable

        tree.remove(duplicateValue);
        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(breakList).isEmpty();

        tree.remove(duplicateValue);
        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(breakList).isEmpty();

        tree.remove(duplicateValue);
        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(2);
        assertThat(tree.getBreaks()).isEmpty();
    }

    @Test
    void testConsecutiveReverseNumbers() {
        ConsecutiveSetTree<AtomicInteger, Integer, Integer> tree = getIntegerConsecutiveSetTree();
        AtomicInteger breakStart3 = atomic(3);
        AtomicInteger breakEnd5 = atomic(5);

        tree.add(breakStart3, 3);
        tree.add(atomic(2), 2);
        tree.add(atomic(1), 1);

        tree.add(atomic(8), 8);
        tree.add(atomic(7), 7);
        tree.add(atomic(6), 6);
        tree.add(breakEnd5, 5);

        IterableList<Sequence<AtomicInteger, Integer>> sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertThat(sequenceList).hasSize(2);
        IterableList<Break<AtomicInteger, Integer>> breakList = new IterableList<>(tree.getBreaks());
        assertThat(breakList).hasSize(1);

        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(sequenceList.get(1).getCount()).isEqualTo(4);
        assertThat(breakList.get(0)).usingRecursiveComparison().isEqualTo(getBreak(tree, breakStart3, breakEnd5));
    }

    @Test
    void testJoinOfTwoChains() {
        ConsecutiveSetTree<AtomicInteger, Integer, Integer> tree = getIntegerConsecutiveSetTree();
        tree.add(atomic(1), 1);
        tree.add(atomic(2), 2);
        tree.add(atomic(3), 3);

        tree.add(atomic(5), 5);
        tree.add(atomic(6), 6);
        tree.add(atomic(7), 7);
        tree.add(atomic(8), 8);

        tree.add(atomic(4), 4);

        IterableList<Sequence<AtomicInteger, Integer>> sequenceList = new IterableList<>(tree.getConsecutiveSequences());

        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(8);
        assertThat(tree.getBreaks()).isEmpty();
    }

    @Test
    void testBreakOfChain() {
        ConsecutiveSetTree<AtomicInteger, Integer, Integer> tree = getIntegerConsecutiveSetTree();
        AtomicInteger removed4 = atomic(4);
        AtomicInteger breakStart3 = atomic(3);
        AtomicInteger breakEnd5 = atomic(5);

        tree.add(atomic(1), 1);
        tree.add(atomic(2), 2);
        tree.add(breakStart3, 3);
        tree.add(removed4, 4);
        tree.add(breakEnd5, 5);
        tree.add(atomic(6), 6);
        tree.add(atomic(7), 7);

        removed4.set(8); // mimic changing a planning variable
        tree.remove(removed4);

        IterableList<Sequence<AtomicInteger, Integer>> sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertThat(sequenceList).hasSize(2);
        IterableList<Break<AtomicInteger, Integer>> breakList = new IterableList<>(tree.getBreaks());
        assertThat(breakList).hasSize(1);

        assertThat(sequenceList).hasSize(2);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(sequenceList.get(1).getCount()).isEqualTo(3);
        assertThat(breakList).hasSize(1);
        assertThat(breakList.get(0)).usingRecursiveComparison().isEqualTo(getBreak(tree, breakStart3, breakEnd5));
    }

    @Test
    void testChainRemoval() {
        ConsecutiveSetTree<AtomicInteger, Integer, Integer> tree = getIntegerConsecutiveSetTree();
        AtomicInteger removed1 = atomic(1);
        AtomicInteger removed2 = atomic(2);
        AtomicInteger removed3 = atomic(3);

        tree.add(removed1, 1);
        tree.add(removed2, 2);
        tree.add(removed3, 3);

        tree.add(atomic(5), 5);
        tree.add(atomic(6), 6);
        tree.add(atomic(7), 7);

        // mimic changing planning variables
        removed1.set(3);
        removed2.set(10);
        removed3.set(-1);

        tree.remove(removed2);
        tree.remove(removed1);
        tree.remove(removed3);

        IterableList<Sequence<AtomicInteger, Integer>> sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertThat(sequenceList).hasSize(1);

        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(tree.getBreaks()).isEmpty();
    }

    @Test
    void testShorteningOfChain() {
        ConsecutiveSetTree<AtomicInteger, Integer, Integer> tree = getIntegerConsecutiveSetTree();
        AtomicInteger start = atomic(1);
        AtomicInteger end = atomic(7);

        tree.add(start, 1);
        tree.add(atomic(2), 2);
        tree.add(atomic(3), 3);
        tree.add(atomic(4), 4);
        tree.add(atomic(5), 5);
        tree.add(atomic(6), 6);
        tree.add(end, 7);

        // mimic changing planning variable
        end.set(3);

        tree.remove(end);

        IterableList<Sequence<AtomicInteger, Integer>> sequenceList = new IterableList<>(tree.getConsecutiveSequences());

        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(6);
        assertThat(tree.getBreaks()).isEmpty();

        // mimic changing planning variable
        start.set(3);

        tree.remove(start);
        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(5);
        assertThat(tree.getBreaks()).isEmpty();
    }

    @Test
    void testRandomSequences() {
        Random random = new Random(1);
        TreeMap<Integer, Integer> valueToCountMap = new TreeMap<>();

        // Tree we are testing is at most difference 2
        ConsecutiveSetTree<Integer, Integer, Integer> tree =
                new ConsecutiveSetTree<>((a, b) -> b - a, Integer::sum, 2, 0);

        for (int i = 0; i < 1000; i++) {
            int value = random.nextInt(64);
            String op;
            if (valueToCountMap.containsKey(value) && random.nextDouble() < 0.75) {
                op = valueToCountMap.keySet().stream().map(Object::toString)
                        .collect(Collectors.joining(", ", "Removing " + value + " from [", "]"));
                valueToCountMap.computeIfPresent(value, (key, count) -> (count == 1) ? null : count - 1);
                tree.remove(value);
            } else {
                op = valueToCountMap.keySet().stream().map(Object::toString)
                        .collect(Collectors.joining(", ", "Adding " + value + " to [", "]"));
                valueToCountMap.merge(value, 1, Integer::sum);
                tree.add(value, value);
            }

            ConsecutiveSetTree<Integer, Integer, Integer> freshTree =
                    new ConsecutiveSetTree<>((a, b) -> b - a, Integer::sum, 2, 0);
            for (Map.Entry<Integer, Integer> entry : valueToCountMap.entrySet()) {
                IntStream.range(0, entry.getValue()).map(index -> entry.getKey()).forEach(key -> freshTree.add(key, key));
            }

            assertThat(tree.getConsecutiveSequences()).as("Mismatched Sequence: " + op)
                    .usingRecursiveComparison()
                    .ignoringFields("sourceTree")
                    .isEqualTo(freshTree.getConsecutiveSequences());
            assertThat(tree.getBreaks()).as("Mismatched Break: " + op)
                    .usingRecursiveComparison()
                    .isEqualTo(freshTree.getBreaks());
        }
    }

    @Test
    void testRandomSequencesWithDuplicates() {
        Random random = new Random(1);
        TreeMap<Integer, Integer> valueToCountMap =
                new TreeMap<>(Comparator.<Integer, Integer> comparing(Math::abs).thenComparing(System::identityHashCode));

        // Tree we are absolute value consecutive
        ConsecutiveSetTree<Integer, Integer, Integer> tree =
                new ConsecutiveSetTree<>((a, b) -> b - a, Integer::sum, 2, 0);

        for (int i = 0; i < 1000; i++) {
            int value = random.nextInt(64) - 32;
            String op;
            if (valueToCountMap.containsKey(value) && random.nextDouble() < 0.75) {
                op = valueToCountMap.keySet().stream().map(Object::toString)
                        .collect(Collectors.joining(", ", "Removing " + value + " from [", "]"));
                valueToCountMap.computeIfPresent(value, (key, count) -> (count == 1) ? null : count - 1);
                tree.remove(value);
            } else {
                op = valueToCountMap.keySet().stream().map(Object::toString)
                        .collect(Collectors.joining(", ", "Adding " + value + " to [", "]"));
                valueToCountMap.merge(value, 1, Integer::sum);
                tree.add(value, Math.abs(value));
            }

            ConsecutiveSetTree<Integer, Integer, Integer> freshTree =
                    new ConsecutiveSetTree<>((a, b) -> b - a, Integer::sum, 2, 0);
            for (Map.Entry<Integer, Integer> entry : valueToCountMap.entrySet()) {
                IntStream.range(0, entry.getValue()).map(index -> entry.getKey())
                        .forEach(key -> freshTree.add(key, Math.abs(key)));
            }

            assertThat(tree.getConsecutiveSequences()).as("Mismatched Sequence: " + op)
                    .usingRecursiveComparison()
                    .ignoringFields("sourceTree")
                    .isEqualTo(freshTree.getConsecutiveSequences());
            assertThat(tree.getBreaks()).as("Mismatched Break: " + op)
                    .usingRecursiveComparison()
                    .isEqualTo(freshTree.getBreaks());
        }
    }

    @Test
    void testTimeslotConsecutive() {
        ConsecutiveSetTree<Timeslot, OffsetDateTime, Duration> tree = new ConsecutiveSetTree<>(
                Duration::between, Duration::plus, Duration.ofDays(1), Duration.ZERO);

        Timeslot t1 = new Timeslot(0, 1);
        Timeslot t2 = new Timeslot(1, 2);

        Timeslot t3 = new Timeslot(3, 4);
        Timeslot t4 = new Timeslot(4, 5);
        Timeslot t5 = new Timeslot(5, 6);

        tree.add(t4, t4.from);
        tree.add(t2, t2.from);
        tree.add(t4, t4.from);
        tree.add(t3, t3.from);
        tree.add(t1, t1.from);
        tree.add(t5, t5.from);

        Iterable<Sequence<Timeslot, Duration>> sequenceList = tree.getConsecutiveSequences();
        assertThat(sequenceList).hasSize(2);
        Iterator<Sequence<Timeslot, Duration>> sequenceIterator = sequenceList.iterator();
        Iterable<Break<Timeslot, Duration>> breakList = tree.getBreaks();
        Iterator<Break<Timeslot, Duration>> breakIterator = breakList.iterator();
        assertThat(breakList).hasSize(1);

        assertThat(sequenceList).hasSize(2);
        assertThat(sequenceIterator.next().getItems()).containsExactly(t1, t2);
        assertThat(sequenceIterator.next().getItems()).containsExactly(t3, t4, t5);

        assertThat(breakList).hasSize(1);
        assertThat(breakIterator.next()).usingRecursiveComparison()
                .isEqualTo(getBreak(tree, t2, t3));
    }

    private ConsecutiveSetTree<AtomicInteger, Integer, Integer> getIntegerConsecutiveSetTree() {
        return new ConsecutiveSetTree<>((a, b) -> b - a, Integer::sum, 1, 0);
    }

    private <ValueType_, DifferenceType_ extends Comparable<DifferenceType_>> Break<ValueType_, DifferenceType_>
            getBreak(SequenceChain<ValueType_, DifferenceType_> consecutiveData, ValueType_ start, ValueType_ end) {
        for (var sequenceBreak : consecutiveData.getBreaks()) {
            if (sequenceBreak.getPreviousSequenceEnd().equals(start) && sequenceBreak.getNextSequenceStart().equals(end)) {
                return sequenceBreak;
            }
        }
        throw new IllegalStateException("Unable to find sequence with start (" + start + ") and end (" + end + ") in ("
                + consecutiveData.getConsecutiveSequences() + ")");
    }

    private static AtomicInteger atomic(int value) {
        return new AtomicInteger(value);
    }

    private record Timeslot(OffsetDateTime from, OffsetDateTime to) {
        public Timeslot(int fromIndex, int toIndex) {
            this(OffsetDateTime.of(2000, 1, fromIndex + 1, 0, 0, 0, 0, ZoneOffset.UTC),
                    OffsetDateTime.of(2000, 1, toIndex + 1, 0, 0, 0, 0, ZoneOffset.UTC));
        }

    }

}
