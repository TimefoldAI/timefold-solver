package ai.timefold.solver.core.impl.neighborhood.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.selection.FixedWeightMoveSelectionStrategy;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSource;
import ai.timefold.solver.core.preview.api.neighborhood.selection.PhaseContext;
import ai.timefold.solver.core.preview.api.neighborhood.selection.StepContext;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveIterable;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class StrategyUnionMoveIteratorTest {

    private static final StepContext<TestdataSolution> STEP_CONTEXT = TestdataSolution::new;

    @Test
    void moveIteratorCreatedOnlyWhenChosen() {
        var sourceList = sourceList(3);
        var createdList = new ArrayList<Integer>();
        var callCount = new int[1];
        var strategy = new RecordingStrategy(sourceList, sources -> callCount[0]++ < 2 ? sources.get(1) : null);
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 1, 2, 1 }, createdList),
                new Random(0), 0);
        assertThat(createdList).isEmpty();

        assertThat(drain(iterator)).containsExactly("1.0", "1.1");
        assertThat(createdList).containsExactly(1);
    }

    @Test
    void emptySourceExhaustedWhenChosen() {
        var sourceList = sourceList(3);
        var callCount = new int[1];
        var strategy = new RecordingStrategy(sourceList, sources -> sources.get(callCount[0]++ == 0 ? 1 : 0));
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 1, 0, 0 }),
                new Random(0), 0);
        assertThat(strategy.eventList).isEmpty();

        iterator.next();
        assertThat(strategy.eventList).containsExactly("next", "exhausted s1", "next", "exhausted s0");
    }

    @Test
    void allExhaustedEndsWithoutAsking() {
        var sourceList = sourceList(2);
        var callCount = new int[1];
        var strategy = new RecordingStrategy(sourceList, sources -> sources.get(callCount[0]++));
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 0, 0 }),
                new Random(0), 0);

        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterator.hasNext()).isFalse();
        assertThat(strategy.eventList).containsExactly("next", "exhausted s0", "next", "exhausted s1");
    }

    @Test
    void endedSourceExhaustedOnce() {
        var sourceList = sourceList(2);
        var strategy = new RecordingStrategy(sourceList, sources -> sources.get(1));
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 5, 2 }),
                new Random(0), 0);
        iterator.next();
        iterator.next();

        assertThat(strategy.eventList).containsExactly("next", "next", "exhausted s1");
    }

    @Test
    void nullEndsStep() {
        var sourceList = sourceList(2);
        var callCount = new int[1];
        var strategy = new RecordingStrategy(sourceList, sources -> callCount[0]++ == 0 ? sources.get(0) : null);
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 5, 5 }),
                new Random(0), 0);
        iterator.next();

        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterator.hasNext()).isFalse();
        assertThat(strategy.eventList).containsExactly("next", "next");
    }

    @Test
    void nullBeforeFirstMoveEndsStep() {
        var sourceList = sourceList(2);
        var strategy = new RecordingStrategy(sourceList, sources -> null);
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 5, 5 }),
                new Random(0), 3);

        assertThat(iterator.hasNext()).isFalse();
        assertThat(strategy.eventList).containsExactly("next");
    }

    @Test
    void onlyZeroWeightSourcesWithMovesEndStep() {
        var sourceList = sourceList(2);
        var strategy = new FixedWeightMoveSelectionStrategy<TestdataSolution>() {
            @Override
            protected double getWeight(MoveSource<TestdataSolution> moveSource, PhaseContext<TestdataSolution> context) {
                return moveSource.id().equals("s0") ? 0.0 : 1.0;
            }
        };
        strategy.phaseStarted(new TestPhaseContext(List.copyOf(sourceList), new Random(0)));
        strategy.stepStarted(STEP_CONTEXT);
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 5, 0 }),
                new Random(0), 0);

        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void negativeIndexSourceFails() {
        var sourceList = sourceList(2);
        var unknownSource = new DefaultMoveSource<>(-1, "negative", unsupportedProvider());
        var strategy = new RecordingStrategy(sourceList, sources -> unknownSource);
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 5, 5 }),
                new Random(0), 0);

        assertThatIllegalStateException()
                .isThrownBy(iterator::hasNext)
                .withMessageContaining("unknown move source (negative)");
    }

    @Test
    void exhaustedSourceFails() {
        var sourceList = sourceList(2);
        var strategy = new RecordingStrategy(sourceList, sources -> sourceList.get(1));
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 5, 0 }),
                new Random(0), 7);

        assertThatIllegalStateException()
                .isThrownBy(iterator::hasNext)
                .withMessageContaining("exhausted move source (s1)")
                .withMessageContaining("step index (7)");
    }

    @Test
    void unknownSourceFails() {
        var sourceList = sourceList(2);
        var unknownSource = new DefaultMoveSource<>(0, "unknown", unsupportedProvider());
        var strategy = new RecordingStrategy(sourceList, sources -> unknownSource);
        var iterator = new StrategyUnionMoveIterator<>(strategy, sourceList, iterableList(new int[] { 5, 5 }),
                new Random(0), 0);

        assertThatIllegalStateException()
                .isThrownBy(iterator::hasNext)
                .withMessageContaining("unknown move source (unknown)");
    }

    private static List<DefaultMoveSource<TestdataSolution>> sourceList(int count) {
        var sourceList = new ArrayList<DefaultMoveSource<TestdataSolution>>(count);
        for (var i = 0; i < count; i++) {
            sourceList.add(new DefaultMoveSource<>(i, "s" + i, unsupportedProvider()));
        }
        return sourceList;
    }

    private static MoveProvider<TestdataSolution> unsupportedProvider() {
        return moveStreamFactory -> {
            throw new UnsupportedOperationException();
        };
    }

    private static List<MoveIterable<TestdataSolution>> iterableList(int[] moveCounts) {
        return iterableList(moveCounts, new ArrayList<>());
    }

    private static List<MoveIterable<TestdataSolution>> iterableList(int[] moveCounts, List<Integer> createdList) {
        var iterableList = new ArrayList<MoveIterable<TestdataSolution>>(moveCounts.length);
        for (var i = 0; i < moveCounts.length; i++) {
            var sourceIndex = i;
            var moveCount = moveCounts[i];
            iterableList.add(random -> {
                createdList.add(sourceIndex);
                return new FiniteMoveIterator(sourceIndex, moveCount);
            });
        }
        return iterableList;
    }

    private static List<String> drain(Iterator<Move<TestdataSolution>> iterator) {
        var labelList = new ArrayList<String>();
        while (iterator.hasNext()) {
            labelList.add(((LabeledMove) iterator.next()).label());
        }
        return labelList;
    }

    private record TestPhaseContext(List<MoveSource<TestdataSolution>> moveSources, RandomGenerator random)
            implements
                PhaseContext<TestdataSolution> {

        @Override
        public TestdataSolution solution() {
            return new TestdataSolution();
        }

    }

    private static final class RecordingStrategy implements MoveSelectionStrategy<TestdataSolution> {

        private final Function<List<MoveSource<TestdataSolution>>, @Nullable MoveSource<TestdataSolution>> chooseFunction;
        private final List<String> eventList = new ArrayList<>();
        private final List<MoveSource<TestdataSolution>> moveSourceList;
        private @Nullable MoveSource<TestdataSolution> lastMoveSource = null;

        private RecordingStrategy(List<? extends MoveSource<TestdataSolution>> moveSourceList,
                Function<List<MoveSource<TestdataSolution>>, @Nullable MoveSource<TestdataSolution>> chooseFunction) {
            this.moveSourceList = List.copyOf(moveSourceList);
            this.chooseFunction = chooseFunction;
        }

        @Override
        public void phaseStarted(PhaseContext<TestdataSolution> context) {
            // Not called by the iterator.
        }

        @Override
        public void stepStarted(StepContext<TestdataSolution> context) {
            // Not called by the iterator.
        }

        @Override
        public void moveSourceExhausted() {
            eventList.add("exhausted " + Objects.requireNonNull(lastMoveSource).id());
        }

        @Override
        public @Nullable MoveSource<TestdataSolution> nextMoveSource() {
            eventList.add("next");
            lastMoveSource = chooseFunction.apply(moveSourceList);
            return lastMoveSource;
        }

    }

    private static final class FiniteMoveIterator implements Iterator<Move<TestdataSolution>> {

        private final int sourceIndex;
        private final int moveCount;
        private int moveIndex = 0;

        private FiniteMoveIterator(int sourceIndex, int moveCount) {
            this.sourceIndex = sourceIndex;
            this.moveCount = moveCount;
        }

        @Override
        public boolean hasNext() {
            return moveIndex < moveCount;
        }

        @Override
        public Move<TestdataSolution> next() {
            return new LabeledMove(sourceIndex + "." + moveIndex++);
        }

    }

    private record LabeledMove(String label) implements Move<TestdataSolution> {

        @Override
        public void execute(MutableSolutionView<TestdataSolution> solutionView) {
            // Never executed by this test.
        }

    }

}
