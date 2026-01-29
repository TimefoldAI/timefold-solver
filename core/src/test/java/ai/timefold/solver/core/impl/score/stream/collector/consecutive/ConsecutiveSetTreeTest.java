package ai.timefold.solver.core.impl.score.stream.collector.consecutive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.util.MutableInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ConsecutiveSetTreeTest {

    @Test
    void testNonconsecutiveNumbers() {
        var tree = getIntegerConsecutiveSetTree();
        var start1 = mutable(3);
        var middle3 = mutable(5);
        var end7 = mutable(6);

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
        var tree = getIntegerConsecutiveSetTree();
        var breakStart3 = mutable(3);
        var breakEnd5 = mutable(5);

        tree.add(mutable(1), 1);
        tree.add(mutable(2), 2);
        tree.add(breakStart3, 3);

        tree.add(breakEnd5, 5);
        tree.add(mutable(6), 6);
        tree.add(mutable(7), 7);
        tree.add(mutable(8), 8);

        var sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertThat(sequenceList).hasSize(2);
        var breakList = new IterableList<>(tree.getBreaks());
        assertThat(breakList).hasSize(1);

        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(sequenceList.get(1).getCount()).isEqualTo(4);
        assertThat(breakList.get(0)).usingRecursiveComparison().isEqualTo(getBreak(tree, breakStart3, breakEnd5));
    }

    @Test
    void testDuplicateNumbers() {
        var tree = getIntegerConsecutiveSetTree();
        var duplicateValue = mutable(3);
        tree.add(mutable(1), 1);
        tree.add(mutable(2), 2);
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

        tree.remove(duplicateValue);
        tree.remove(duplicateValue);
        tree.remove(duplicateValue);
        duplicateValue.setValue(0); // mimic the constraint collector changing a planning variable

        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(2);
        assertThat(breakList).isEmpty();
    }

    @Test
    void testDuplicateIndexes() {
        var THREE_1 = new MutableInt(3);
        var THREE_2 = new MutableInt(3);
        var THREE_3 = new MutableInt(3);

        var a = new AtomicInteger(0);
        var b = new AtomicInteger(1);
        var c = new AtomicInteger(2);

        var tree = getMutableIntConsecutiveSetTree();
        tree.add(a, THREE_1);
        tree.add(b, THREE_2);
        tree.add(c, THREE_3);

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

        tree.remove(a);
        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(2);
        assertThat(breakList).isEmpty();

        tree.remove(b);
        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(1);
        assertThat(breakList).isEmpty();

        tree.remove(c);
        assertThat(sequenceList).isEmpty();
        assertThat(tree.getBreaks()).isEmpty();
    }

    @Test
    void testConsecutiveReverseNumbers() {
        var tree = getIntegerConsecutiveSetTree();
        var breakStart3 = mutable(3);
        var breakEnd5 = mutable(5);

        tree.add(breakStart3, 3);
        tree.add(mutable(2), 2);
        tree.add(mutable(1), 1);

        tree.add(mutable(8), 8);
        tree.add(mutable(7), 7);
        tree.add(mutable(6), 6);
        tree.add(breakEnd5, 5);

        var sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertThat(sequenceList).hasSize(2);
        var breakList = new IterableList<>(tree.getBreaks());
        assertThat(breakList).hasSize(1);

        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(sequenceList.get(1).getCount()).isEqualTo(4);
        assertThat(breakList.get(0)).usingRecursiveComparison().isEqualTo(getBreak(tree, breakStart3, breakEnd5));
    }

    @Test
    void testJoinOfTwoChains() {
        var tree = getIntegerConsecutiveSetTree();
        tree.add(mutable(1), 1);
        tree.add(mutable(2), 2);
        tree.add(mutable(3), 3);

        tree.add(mutable(5), 5);
        tree.add(mutable(6), 6);
        tree.add(mutable(7), 7);
        tree.add(mutable(8), 8);

        tree.add(mutable(4), 4);

        var sequenceList = new IterableList<>(tree.getConsecutiveSequences());

        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(8);
        assertThat(tree.getBreaks()).isEmpty();
    }

    @Test
    void testBreakOfChain() {
        var tree = getIntegerConsecutiveSetTree();
        var removed4 = mutable(4);
        var breakStart3 = mutable(3);
        var breakEnd5 = mutable(5);

        tree.add(mutable(1), 1);
        tree.add(mutable(2), 2);
        tree.add(breakStart3, 3);
        tree.add(removed4, 4);
        tree.add(breakEnd5, 5);
        tree.add(mutable(6), 6);
        tree.add(mutable(7), 7);

        tree.remove(removed4);
        removed4.setValue(8); // mimic changing a planning variable

        var sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertThat(sequenceList).hasSize(2);
        var breakList = new IterableList<>(tree.getBreaks());
        assertThat(breakList).hasSize(1);

        assertThat(sequenceList).hasSize(2);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(sequenceList.get(1).getCount()).isEqualTo(3);
        assertThat(breakList).hasSize(1);
        assertThat(breakList.get(0)).usingRecursiveComparison().isEqualTo(getBreak(tree, breakStart3, breakEnd5));
    }

    @Test
    void testChainRemoval() {
        var tree = getIntegerConsecutiveSetTree();
        var removed1 = mutable(1);
        var removed2 = mutable(2);
        var removed3 = mutable(3);

        tree.add(removed1, 1);
        tree.add(removed2, 2);
        tree.add(removed3, 3);

        tree.add(mutable(5), 5);
        tree.add(mutable(6), 6);
        tree.add(mutable(7), 7);

        // mimic changing planning variables
        tree.remove(removed2);
        tree.remove(removed1);
        tree.remove(removed3);

        removed1.setValue(3);
        removed2.setValue(10);
        removed3.setValue(-1);

        var sequenceList = new IterableList<>(tree.getConsecutiveSequences());
        assertThat(sequenceList).hasSize(1);

        assertThat(sequenceList.get(0).getCount()).isEqualTo(3);
        assertThat(tree.getBreaks()).isEmpty();
    }

    @Test
    void testShorteningOfChain() {
        var tree = getIntegerConsecutiveSetTree();
        var start = mutable(1);
        var end = mutable(7);

        tree.add(start, 1);
        tree.add(mutable(2), 2);
        tree.add(mutable(3), 3);
        tree.add(mutable(4), 4);
        tree.add(mutable(5), 5);
        tree.add(mutable(6), 6);
        tree.add(end, 7);

        // mimic changing planning variable
        tree.remove(end);
        end.setValue(3);

        var sequenceList = new IterableList<>(tree.getConsecutiveSequences());

        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(6);
        assertThat(tree.getBreaks()).isEmpty();

        // mimic changing planning variable
        tree.remove(start);
        start.setValue(3);

        assertThat(sequenceList).hasSize(1);
        assertThat(sequenceList.get(0).getCount()).isEqualTo(5);
        assertThat(tree.getBreaks()).isEmpty();
    }

    @Test
    void testRandomSequences() {
        var random = new Random(1);
        var valueToCountMap = new TreeMap<Integer, Integer>();

        // Tree we are testing is at most difference 2
        var tree = new ConsecutiveSetTree<Integer, Integer, Integer>((a, b) -> b - a, Integer::sum, 2, 0);

        for (var i = 0; i < 1000; i++) {
            var value = random.nextInt(64);
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

            var freshTree =
                    new ConsecutiveSetTree<Integer, Integer, Integer>((a, b) -> b - a, Integer::sum, 2, 0);
            for (var entry : valueToCountMap.entrySet()) {
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
        var random = new Random(1);
        var valueToCountMap =
                new TreeMap<Integer, Integer>(
                        Comparator.<Integer, Integer> comparing(Math::abs).thenComparing(System::identityHashCode));

        // Tree we are absolute value consecutive
        var tree = new ConsecutiveSetTree<Integer, Integer, Integer>((a, b) -> b - a, Integer::sum, 2, 0);

        for (var i = 0; i < 1000; i++) {
            var value = random.nextInt(64) - 32;
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

            var freshTree = new ConsecutiveSetTree<Integer, Integer, Integer>((a, b) -> b - a, Integer::sum, 2, 0);
            for (var entry : valueToCountMap.entrySet()) {
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

    private ConsecutiveSetTree<MutableInt, Integer, Integer> getIntegerConsecutiveSetTree() {
        return new ConsecutiveSetTree<>((a, b) -> b - a, Integer::sum, 1, 0);
    }

    private ConsecutiveSetTree<AtomicInteger, MutableInt, MutableInt> getMutableIntConsecutiveSetTree() {
        return new ConsecutiveSetTree<>(
                (a, b) -> new MutableInt(b.intValue() - a.intValue()),
                (a, b) -> new MutableInt(a.intValue() + b.intValue()),
                new MutableInt(1), new MutableInt(0));
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

    private static MutableInt mutable(int value) {
        return new MutableInt(value);
    }

    private record Timeslot(OffsetDateTime from, OffsetDateTime to) {
        public Timeslot(int fromIndex, int toIndex) {
            this(OffsetDateTime.of(2000, 1, fromIndex + 1, 0, 0, 0, 0, ZoneOffset.UTC),
                    OffsetDateTime.of(2000, 1, toIndex + 1, 0, 0, 0, 0, ZoneOffset.UTC));
        }

    }

}
