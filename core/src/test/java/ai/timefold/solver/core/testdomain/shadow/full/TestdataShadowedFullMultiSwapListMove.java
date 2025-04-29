package ai.timefold.solver.core.testdomain.shadow.full;

import java.util.List;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;

/**
 * A move that sets the values on two entities to specified values multiple times.
 */
public class TestdataShadowedFullMultiSwapListMove extends AbstractMove<TestdataShadowedFullSolution> {
    TestdataShadowedFullEntity a;
    TestdataShadowedFullEntity b;
    List<List<TestdataShadowedFullValue>> aValueLists;
    List<List<TestdataShadowedFullValue>> bValueLists;

    public TestdataShadowedFullMultiSwapListMove(TestdataShadowedFullEntity a, TestdataShadowedFullEntity b,
            List<List<TestdataShadowedFullValue>> aValueLists,
            List<List<TestdataShadowedFullValue>> bValueLists) {
        this.a = a;
        this.b = b;
        this.aValueLists = aValueLists;
        this.bValueLists = bValueLists;
        assert aValueLists.size() == bValueLists.size();
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TestdataShadowedFullSolution> scoreDirector) {
        for (int i = 0; i < aValueLists.size(); i++) {
            scoreDirector.beforeListVariableChanged(a, "valueList", 0, a.valueList.size());
            a.valueList.clear();
            a.valueList.addAll(aValueLists.get(i));
            scoreDirector.afterListVariableChanged(a, "valueList", 0, a.valueList.size());

            scoreDirector.beforeListVariableChanged(b, "valueList", 0, b.valueList.size());
            b.valueList.clear();
            b.valueList.addAll(bValueLists.get(i));
            scoreDirector.afterListVariableChanged(b, "valueList", 0, b.valueList.size());

            scoreDirector.triggerVariableListeners();
        }
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TestdataShadowedFullSolution> scoreDirector) {
        return true;
    }
}
