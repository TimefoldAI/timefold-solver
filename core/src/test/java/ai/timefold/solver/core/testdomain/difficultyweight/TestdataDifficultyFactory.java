package ai.timefold.solver.core.testdomain.difficultyweight;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class TestdataDifficultyFactory
        implements SelectionSorterWeightFactory<TestdataDifficultyWeightSolution, TestdataDifficultyWeightEntity> {

    @Override
    public Comparable createSorterWeight(TestdataDifficultyWeightSolution testdataDifficultyWeightSolution,
            TestdataDifficultyWeightEntity selection) {
        return 0;
    }
}
