package ai.timefold.solver.core.impl.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * {@code UniformRandomUnionMoveIterator} must draw a fresh move for every call to {@code next()},
 * not just once per exhaustion.
 */
@Execution(ExecutionMode.CONCURRENT)
class UniformRandomUnionMoveIteratorTest {

    @Test
    void drawsFromEveryIteratorWithinOneRun() {
        var moveIteratorList = List.<Iterator<Move<TestdataSolution>>> of(
                new NeverEndingMoveIterator(0), new NeverEndingMoveIterator(1), new NeverEndingMoveIterator(2));
        var iterator = UniformRandomUnionMoveIterator.of(new Random(0), moveIteratorList, (i, workingRandom) -> i);

        var seenIndexSet = new HashSet<Integer>();
        for (var i = 0; i < 30; i++) {
            var move = (LabeledMove) iterator.next();
            seenIndexSet.add(move.index());
        }

        assertThat(seenIndexSet).containsExactlyInAnyOrder(0, 1, 2);
    }

    @Test
    void exhaustsEveryIteratorExactlyOnce() {
        var moveIteratorList = List.<Iterator<Move<TestdataSolution>>> of(
                new FiniteMoveIterator(0, 3), new FiniteMoveIterator(1, 1), new FiniteMoveIterator(2, 5));
        var iterator = UniformRandomUnionMoveIterator.of(new Random(0), moveIteratorList, (i, workingRandom) -> i);

        var countByIndex = new HashMap<Integer, Integer>();
        while (iterator.hasNext()) {
            var move = (LabeledMove) iterator.next();
            countByIndex.merge(move.index(), 1, Integer::sum);
        }

        assertThat(countByIndex).containsExactlyInAnyOrderEntriesOf(Map.of(0, 3, 1, 1, 2, 5));
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
    }

    @Test
    void hasNextOnlyPollsTheDrawnIterator() {
        var a = new CountingMoveIterator(0);
        var b = new CountingMoveIterator(1);
        var c = new CountingMoveIterator(2);
        var moveIteratorList = List.<Iterator<Move<TestdataSolution>>> of(a, b, c);
        var iterator = UniformRandomUnionMoveIterator.of(new Random(0), moveIteratorList, (i, workingRandom) -> i);
        // Constructing the union primes every iterator with exactly one hasNext() call.
        assertThat(a.hasNextCallCount).isEqualTo(1);
        assertThat(b.hasNextCallCount).isEqualTo(1);
        assertThat(c.hasNextCallCount).isEqualTo(1);

        var move = (LabeledMove) iterator.next();
        var drawn = List.of(a, b, c).get(move.index());

        for (var moveIterator : List.of(a, b, c)) {
            if (moveIterator == drawn) {
                assertThat(moveIterator.hasNextCallCount).isEqualTo(2);
            } else {
                assertThat(moveIterator.hasNextCallCount).isEqualTo(1);
            }
        }
    }

    /**
     * Never reports exhaustion.
     */
    private record NeverEndingMoveIterator(int index) implements Iterator<Move<TestdataSolution>> {

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Move<TestdataSolution> next() {
            return new LabeledMove(index);
        }

    }

    /**
     * Reports exhaustion after producing a fixed number of moves.
     */
    private static final class FiniteMoveIterator implements Iterator<Move<TestdataSolution>> {

        private final int index;
        private int remaining;

        private FiniteMoveIterator(int index, int moveCount) {
            this.index = index;
            this.remaining = moveCount;
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public Move<TestdataSolution> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            remaining--;
            return new LabeledMove(index);
        }

    }

    /**
     * Never exhausted; counts {@link #hasNext()} calls. Does not reuse {@link FiniteMoveIterator},
     * whose {@code next()} guard itself calls {@code hasNext()} and would inflate the count.
     */
    private static final class CountingMoveIterator implements Iterator<Move<TestdataSolution>> {

        private final int index;
        private int hasNextCallCount = 0;

        private CountingMoveIterator(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            hasNextCallCount++;
            return true;
        }

        @Override
        public Move<TestdataSolution> next() {
            return new LabeledMove(index);
        }

    }

    @NullMarked
    private record LabeledMove(int index) implements Move<TestdataSolution> {

        @Override
        public void execute(MutableSolutionView<TestdataSolution> solutionView) {
            // Never actually executed by this test.
        }

    }

}
