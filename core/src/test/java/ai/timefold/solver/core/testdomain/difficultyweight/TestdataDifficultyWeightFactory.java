package ai.timefold.solver.core.testdomain.difficultyweight;

import ai.timefold.solver.core.api.domain.common.SorterWeightFactory;

public class TestdataDifficultyWeightFactory implements
        SorterWeightFactory<TestdataDifficultyWeightSolution, TestdataDifficultyWeightEntity> {

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
