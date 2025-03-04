package ai.timefold.solver.core.impl.testdata.domain.shadow.full;

import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;

/**
 * A move that swaps the tails of two entities multiple times,
 * Causing multiple listVariableChanged events which must be
 * processed between changes.
 */
public class TestdataShadowedFullMultiTailSwapListMove extends AbstractMove<TestdataShadowedFullSolution> {
    TestdataShadowedFullEntity a;
    TestdataShadowedFullEntity b;
    long seed;

    public TestdataShadowedFullMultiTailSwapListMove(TestdataShadowedFullEntity a, TestdataShadowedFullEntity b, long seed) {
        this.a = a;
        this.b = b;
        this.seed = seed;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TestdataShadowedFullSolution> scoreDirector) {
        var random = new Random(seed);
        int swaps = random.nextInt(3) + 2;
        for (int i = 0; i < swaps; i++) {
            var leftStart = a.valueList.isEmpty() ? 0 : random.nextInt(a.valueList.size());
            var rightStart = b.valueList.isEmpty() ? 0 : random.nextInt(b.valueList.size());
            var leftTail = List.copyOf(a.valueList.subList(leftStart, a.valueList.size()));
            var rightTail = List.copyOf(b.valueList.subList(rightStart, b.valueList.size()));

            scoreDirector.beforeListVariableChanged(a, "valueList", leftStart, a.valueList.size());
            a.valueList.subList(leftStart, a.valueList.size()).clear();
            a.valueList.addAll(rightTail);
            scoreDirector.afterListVariableChanged(a, "valueList", leftStart, a.valueList.size());

            scoreDirector.beforeListVariableChanged(b, "valueList", rightStart, b.valueList.size());
            b.valueList.subList(rightStart, b.valueList.size()).clear();
            b.valueList.addAll(leftTail);
            scoreDirector.afterListVariableChanged(b, "valueList", rightStart, b.valueList.size());

            scoreDirector.triggerVariableListeners();
        }
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TestdataShadowedFullSolution> scoreDirector) {
        return true;
    }
}
