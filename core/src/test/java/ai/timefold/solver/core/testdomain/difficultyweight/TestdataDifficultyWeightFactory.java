package ai.timefold.solver.core.testdomain.difficultyweight;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class TestdataDifficultyWeightFactory implements
        SelectionSorterWeightFactory<TestdataDifficultyWeightSolution, TestdataDifficultyWeightEntity> {

    @Override
    public TestdataDifficultyWeightComparable createSorterWeight(TestdataDifficultyWeightSolution solution,
            TestdataDifficultyWeightEntity entity) {
        return new TestdataDifficultyWeightComparable();
    }

    public static class TestdataDifficultyWeightComparable implements Comparable<TestdataDifficultyWeightComparable> {

        @Override
        public int compareTo(TestdataDifficultyWeightComparable other) {
            return 0;
        }
    }
}
