package ai.timefold.solver.core.preview.api.neighborhood.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class FixedWeightMoveSelectionStrategyTest {

    private static final MoveSource<TestdataSolution> A = new TestMoveSource("a");
    private static final MoveSource<TestdataSolution> B = new TestMoveSource("b");
    private static final MoveSource<TestdataSolution> C = new TestMoveSource("c");
    private static final StepContext<TestdataSolution> STEP_CONTEXT = TestdataSolution::new;

    @Test
    void samePicksAsBiasedRandomUnionMoveIterator() {
        // Mirrors UnionMoveSelectorTest.biasedRandomSelection: a has 3 moves with weight 1000, b has 2 with weight 20.
        var strategy = started(Map.of("a", 1000.0, "b", 20.0), List.of(A, B));
        var random = new TestRandom(1.0 / 1020.0, 1019.0 / 1020.0, 1000.0 / 1020.0, 0.0, 999.0 / 1020.0);
        var remainingMap = new HashMap<>(Map.of(A, 3, B, 2));
        var pickList = new ArrayList<String>();
        for (var i = 0; i < 5; i++) {
            var moveSource = strategy.nextMoveSource(random);
            pickList.add(moveSource.id());
            if (remainingMap.merge(moveSource, -1, Integer::sum) == 0) {
                strategy.moveSourceExhausted(moveSource);
            }
        }

        assertThat(pickList).containsExactly("a", "b", "b", "a", "a");
        assertThat(strategy.nextMoveSource(random)).isNull();
    }

    @Test
    void subclassWithEqualWeightsUsesBiasedAlgorithm() {
        var strategy = started(Map.of("a", 2.0, "b", 2.0, "c", 2.0), List.of(A, B, C));

        // The biased algorithm uses nextDouble(); the uniform one would use nextInt().
        assertThat(strategy.nextMoveSource(new TestRandom(0.5))).isSameAs(B);
    }

    @Test
    void exhaustedNotChosenUntilStepStarted() {
        var strategy = started(Map.of("a", 1.0, "b", 1.0, "c", 1.0), List.of(A, B, C));
        strategy.moveSourceExhausted(A);

        // Offsets: b [0, 1), c [1, 2).
        assertThat(strategy.nextMoveSource(new TestRandom(0.0))).isSameAs(B);

        strategy.stepStarted(STEP_CONTEXT);
        assertThat(strategy.nextMoveSource(new TestRandom(0.0))).isSameAs(A);
    }

    @Test
    void zeroWeightNeverChosen() {
        var strategy = started(Map.of("a", 0.0, "b", 1.0, "c", 0.0), List.of(A, B, C));

        assertThat(strategy.nextMoveSource(new TestRandom(0.0))).isSameAs(B);
        assertThat(strategy.nextMoveSource(new TestRandom(0.99))).isSameAs(B);

        strategy.moveSourceExhausted(B);
        assertThat(strategy.nextMoveSource(new TestRandom(0.5))).isNull();
    }

    @Test
    void getWeightCalledOncePerMoveSourceWithPhaseContext() {
        var callList = new ArrayList<String>();
        var context = new TestPhaseContext(List.of(A, B, C));
        var strategy = new FixedWeightMoveSelectionStrategy<TestdataSolution>() {
            @Override
            protected double getWeight(MoveSource<TestdataSolution> moveSource, PhaseContext<TestdataSolution> phaseContext) {
                assertThat(phaseContext).isSameAs(context);
                callList.add(moveSource.id());
                return 1.0;
            }
        };
        strategy.phaseStarted(context);
        strategy.stepStarted(STEP_CONTEXT);
        strategy.nextMoveSource(new TestRandom(0.5));

        assertThat(callList).containsExactly("a", "b", "c");
    }

    @Test
    void invalidWeightsFail() {
        for (var weight : new double[] { -1.0, Double.NaN, Double.POSITIVE_INFINITY }) {
            var weightMap = Map.of("a", 1.0, "b", weight, "c", 1.0);
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> started(weightMap, List.of(A, B, C)))
                    .withMessageContaining("weight (" + weight + ")")
                    .withMessageContaining("move source (b)");
        }
        var zeroWeightMap = Map.of("a", 0.0, "b", 0.0, "c", 0.0);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> started(zeroWeightMap, List.of(A, B, C)))
                .withMessageContaining("All weights")
                .withMessageContaining("[a, b, c]");
        var overflowWeightMap = Map.of("a", Double.MAX_VALUE, "b", Double.MAX_VALUE, "c", 1.0);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> started(overflowWeightMap, List.of(A, B, C)))
                .withMessageContaining("sum of the weights")
                .withMessageContaining("[a, b, c]");
    }

    @Test
    void defaultUsesUniformAlgorithm() {
        var strategy = new FixedWeightMoveSelectionStrategy<TestdataSolution>();
        strategy.phaseStarted(new TestPhaseContext(List.of(A, B, C)));
        strategy.stepStarted(STEP_CONTEXT);

        assertThat(strategy.nextMoveSource(new TestRandom(2))).isSameAs(C);
        strategy.moveSourceExhausted(A);
        // Available: c, b.
        assertThat(strategy.nextMoveSource(new TestRandom(0))).isSameAs(C);
        strategy.moveSourceExhausted(C);
        // One available move source: no random number.
        assertThat(strategy.nextMoveSource(new TestRandom(new int[0]))).isSameAs(B);
        strategy.moveSourceExhausted(B);
        assertThat(strategy.nextMoveSource(new TestRandom(new int[0]))).isNull();
    }

    private static MoveSelectionStrategy<TestdataSolution> started(Map<String, Double> weightMap,
            List<MoveSource<TestdataSolution>> moveSourceList) {
        var strategy = new FixedWeightMoveSelectionStrategy<TestdataSolution>() {
            @Override
            protected double getWeight(MoveSource<TestdataSolution> moveSource, PhaseContext<TestdataSolution> context) {
                return weightMap.get(moveSource.id());
            }
        };
        strategy.phaseStarted(new TestPhaseContext(moveSourceList));
        strategy.stepStarted(STEP_CONTEXT);
        return strategy;
    }

    private record TestMoveSource(String id) implements MoveSource<TestdataSolution> {

        @Override
        public MoveProvider<TestdataSolution> provider() {
            throw new UnsupportedOperationException();
        }

    }

    private record TestPhaseContext(List<MoveSource<TestdataSolution>> moveSources)
            implements
                PhaseContext<TestdataSolution> {

        @Override
        public TestdataSolution solution() {
            return new TestdataSolution();
        }

    }

}
