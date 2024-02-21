package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

public final class TestdataAllowsUnassignedValuesListEasyScoreCalculator
        implements EasyScoreCalculator<TestdataAllowsUnassignedValuesListSolution, SimpleScore> {

    @Override
    public SimpleScore calculateScore(TestdataAllowsUnassignedValuesListSolution solution) {
        int i = 0;
        for (TestdataAllowsUnassignedValuesListEntity entity : solution.getEntityList()) {
            i += entity.getValueList().size();
        }
        return SimpleScore.of(-i);
    }
}
